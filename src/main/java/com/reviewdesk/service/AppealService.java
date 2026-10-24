package com.reviewdesk.service;

import com.reviewdesk.domain.Appeal;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.AppealRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学生申诉与教师复核。
 */
@Service
@Transactional(readOnly = true)
public class AppealService {

    private static final Set<String> ACTIVE_STATUSES =
            Set.of("SUBMITTED", "UNDER_REVIEW");

    private final AppealRepository appealRepository;
    private final ReviewService reviewService;
    private final AssignmentService assignmentService;
    private final AccessControlService accessControlService;
    private final ReviewActivityService activityService;

    public AppealService(AppealRepository appealRepository,
                         ReviewService reviewService,
                         AssignmentService assignmentService,
                         AccessControlService accessControlService,
                         ReviewActivityService activityService) {
        this.appealRepository = appealRepository;
        this.reviewService = reviewService;
        this.assignmentService = assignmentService;
        this.accessControlService = accessControlService;
        this.activityService = activityService;
    }

    @Transactional
    public Appeal submit(long submissionId, long studentId,
                         String reason, String requestedResolution) {
        Submission submission = reviewService.requireSubmission(submissionId);
        if (!submission.getStudentAccountId().equals(studentId)) {
            throw new SecurityException("无权申诉该提交");
        }
        Assignment assignment =
                assignmentService.requireAssignment(submission.getAssignmentId());
        if (!"REVIEWED".equals(submission.getReviewStatus())) {
            throw new IllegalArgumentException("教师发布最终反馈后才能申诉");
        }
        if (!assignment.isAppealAllowed()) {
            throw new IllegalArgumentException("该作业未开放申诉");
        }
        if (appealRepository.existsBySubmissionIdAndStatusIn(
                submissionId, ACTIVE_STATUSES)) {
            throw new IllegalArgumentException("已有处理中的申诉");
        }
        if (reason == null || reason.strip().length() < 10) {
            throw new IllegalArgumentException("申诉理由不能少于 10 个字符");
        }
        Appeal appeal = appealRepository.save(new Appeal(
                submissionId,
                studentId,
                reason.strip(),
                requestedResolution == null ? null : requestedResolution.strip()));
        activityService.record(submissionId, "APPEAL_SUBMITTED", "学生提交申诉");
        return appeal;
    }

    public List<Appeal> listForSubmission(long submissionId) {
        return appealRepository.findBySubmissionIdOrderByCreatedAtDesc(submissionId);
    }

    public long assignmentIdFor(long submissionId) {
        return reviewService.requireSubmission(submissionId).getAssignmentId();
    }

    public List<Appeal> listForSubmissions(Collection<Long> submissionIds) {
        return submissionIds.isEmpty()
                ? List.of()
                : appealRepository.findBySubmissionIdInOrderByCreatedAtDesc(submissionIds);
    }

    @Transactional
    public Appeal respond(String teacherUsername, long appealId,
                          String status, String response) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在"));
        accessControlService.requireTeacherSubmission(
                teacherUsername, appeal.getSubmissionId());
        if (!Set.of("ACCEPTED", "REJECTED", "UNDER_REVIEW").contains(status)) {
            throw new IllegalArgumentException("不支持的申诉状态");
        }
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("请填写教师复核说明");
        }
        appeal.respond(status, response.strip());
        activityService.record(
                appeal.getSubmissionId(), "APPEAL_RESOLVED", status);
        return appealRepository.save(appeal);
    }
}
