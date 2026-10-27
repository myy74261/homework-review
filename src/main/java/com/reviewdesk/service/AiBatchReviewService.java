package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.AiBatchReviewJob;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.AccountRepository;
import com.reviewdesk.repository.AiBatchReviewJobRepository;
import com.reviewdesk.repository.ClassroomRepository;
import com.reviewdesk.repository.SubmissionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 作业截止后自动创建并执行 AI 预审任务。
 */
@Service
@Transactional(readOnly = true)
public class AiBatchReviewService {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    private final AiBatchReviewJobRepository jobRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentService assignmentService;
    private final ClassroomRepository classroomRepository;
    private final AccountRepository accountRepository;
    private final AiSettingsService settingsService;
    private final AiReviewService aiReviewService;

    public AiBatchReviewService(
            AiBatchReviewJobRepository jobRepository,
            SubmissionRepository submissionRepository,
            AssignmentService assignmentService,
            ClassroomRepository classroomRepository,
            AccountRepository accountRepository,
            AiSettingsService settingsService,
            AiReviewService aiReviewService) {
        this.jobRepository = jobRepository;
        this.submissionRepository = submissionRepository;
        this.assignmentService = assignmentService;
        this.classroomRepository = classroomRepository;
        this.accountRepository = accountRepository;
        this.settingsService = settingsService;
        this.aiReviewService = aiReviewService;
    }

    /**
     * 为已截止但尚未完成审核的提交创建幂等任务。未配置模型的教师会暂时跳过，
     * 配置完成后下一轮扫描会自动补建。
     */
    @Transactional
    public int enqueueDueSubmissions() {
        int created = 0;
        for (Submission submission : submissionRepository
                .findByDueAtBeforeAndReviewStatusNot(Instant.now(), "REVIEWED")) {
            if (jobRepository.existsBySubmissionId(submission.getId())
                    || !isTeacherConfigured(submission)) {
                continue;
            }
            jobRepository.save(new AiBatchReviewJob(
                    submission.getAssignmentId(), submission.getId()));
            created++;
        }
        return created;
    }

    @Transactional
    public Optional<AiBatchReviewJob> claimNext() {
        Optional<AiBatchReviewJob> next = jobRepository
                .findFirstByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                        "PENDING", Instant.now());
        next.ifPresent(job -> {
            job.markRunning();
            jobRepository.save(job);
        });
        return next;
    }

    @Transactional
    public void process(long jobId) {
        AiBatchReviewJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("AI 批量任务不存在"));
        Submission submission = submissionRepository.findById(job.getSubmissionId())
                .orElse(null);
        if (submission == null || "REVIEWED".equals(submission.getReviewStatus())) {
            job.skip("提交已删除或教师已完成审核");
            jobRepository.save(job);
            return;
        }
        try {
            Account teacher = requireTeacher(submission);
            if (!settingsService.isConfigured(teacher.getUsername())) {
                throw new IllegalStateException("教师尚未配置 AI 模型");
            }
            aiReviewService.generate(teacher.getUsername(), submission.getId());
            job.complete();
        } catch (Exception exception) {
            boolean canRetry = job.getAttemptCount() < MAX_ATTEMPTS;
            job.fail(
                    safeMessage(exception.getMessage()),
                    Instant.now().plus(RETRY_DELAY),
                    canRetry);
        }
        jobRepository.save(job);
    }

    public List<BatchProgressView> progress(Collection<Long> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AiBatchReviewJob>> byAssignment = jobRepository
                .findByAssignmentIdIn(assignmentIds).stream()
                .collect(Collectors.groupingBy(AiBatchReviewJob::getAssignmentId));
        return assignmentIds.stream()
                .map(assignmentId -> {
                    List<AiBatchReviewJob> jobs =
                            byAssignment.getOrDefault(assignmentId, List.of());
                    return new BatchProgressView(
                            assignmentId,
                            jobs.size(),
                            count(jobs, "COMPLETED"),
                            count(jobs, "RUNNING"),
                            count(jobs, "PENDING"),
                            count(jobs, "FAILED"),
                            count(jobs, "SKIPPED"));
                })
                .toList();
    }

    private boolean isTeacherConfigured(Submission submission) {
        try {
            return settingsService.isConfigured(requireTeacher(submission).getUsername());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Account requireTeacher(Submission submission) {
        Assignment assignment = assignmentService.requireAssignment(
                submission.getAssignmentId());
        Classroom classroom = classroomRepository.findById(assignment.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("作业课堂不存在"));
        return accountRepository.findById(classroom.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("作业教师不存在"));
    }

    private long count(List<AiBatchReviewJob> jobs, String status) {
        return jobs.stream().filter(job -> status.equals(job.getStatus())).count();
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "AI 预审失败";
        }
        return message.length() <= 300 ? message : message.substring(0, 300);
    }

    public record BatchProgressView(
            long assignmentId,
            long total,
            long completed,
            long running,
            long pending,
            long failed,
            long skipped
    ) {
    }
}
