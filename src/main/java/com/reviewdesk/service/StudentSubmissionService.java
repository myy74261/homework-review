package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.ReviewDraftRepository;
import com.reviewdesk.repository.SubmissionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学生提交作业，并为教师创建对应的审核草稿。
 */
@Service
@Transactional(readOnly = true)
public class StudentSubmissionService {

    private final AssignmentService assignmentService;
    private final SubmissionRepository submissionRepository;
    private final ReviewDraftRepository draftRepository;
    private final ReviewService reviewService;
    private final ReviewActivityService activityService;
    private final ClassroomService classroomService;

    public StudentSubmissionService(AssignmentService assignmentService,
                                    SubmissionRepository submissionRepository,
                                    ReviewDraftRepository draftRepository,
                                    ReviewService reviewService,
                                    ReviewActivityService activityService,
                                    ClassroomService classroomService) {
        this.assignmentService = assignmentService;
        this.submissionRepository = submissionRepository;
        this.draftRepository = draftRepository;
        this.reviewService = reviewService;
        this.activityService = activityService;
        this.classroomService = classroomService;
    }

    public List<Submission> listForStudent(Account student) {
        return submissionRepository.findByStudentAccountIdOrderBySubmittedAtDesc(
                student.getId());
    }

    public Optional<Submission> findLatest(long assignmentId, Account student) {
        return submissionRepository
                .findFirstByAssignmentIdAndStudentAccountIdOrderByAttemptNumberDesc(
                        assignmentId, student.getId());
    }

    /**
     * 每次重新提交都会创建新的提交记录，保留之前提交供教师对照。
     */
    @Transactional
    public Submission submit(long assignmentId, Account student, String content) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        if (!classroomService.isStudentMember(student, assignment.getClassroomId())) {
            throw new SecurityException("尚未加入该作业所属课堂");
        }
        if (content == null || content.strip().length() < 20) {
            throw new IllegalArgumentException("提交内容不能少于 20 个字符");
        }

        int attempt = findLatest(assignmentId, student)
                .map(previous -> previous.getAttemptNumber() + 1)
                .orElse(1);
        long nextId = submissionRepository.findTopByOrderByIdDesc()
                .map(previous -> previous.getId() + 1)
                .orElse(1L);
        String cleanContent = content.strip();
        Submission submission = new Submission(
                nextId,
                assignment.getId(),
                student.getId(),
                assignment.getTitle(),
                assignment.getCourseName(),
                student.getDisplayName(),
                student.getStudentNumber(),
                cleanContent,
                ReviewService.countCharacters(cleanContent),
                attempt,
                "在线提交",
                assignment.getDueAt(),
                java.time.Instant.now());
        submissionRepository.save(submission);
        draftRepository.save(new ReviewDraft(
                submission.getId(),
                assignment.getTitle() + "审核意见",
                "待教师填写"));
        reviewService.createVersion(submission.getId(), "INITIAL");
        activityService.record(submission.getId(), "SUBMISSION_CREATED", "学生在线提交");
        return submission;
    }
}
