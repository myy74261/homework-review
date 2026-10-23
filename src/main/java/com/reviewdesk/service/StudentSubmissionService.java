package com.reviewdesk.service;

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

    public StudentSubmissionService(AssignmentService assignmentService,
                                    SubmissionRepository submissionRepository,
                                    ReviewDraftRepository draftRepository,
                                    ReviewService reviewService,
                                    ReviewActivityService activityService) {
        this.assignmentService = assignmentService;
        this.submissionRepository = submissionRepository;
        this.draftRepository = draftRepository;
        this.reviewService = reviewService;
        this.activityService = activityService;
    }

    public List<Submission> listForStudent(String studentNumber) {
        return submissionRepository.findByStudentNumberOrderBySubmittedAtDesc(studentNumber);
    }

    public Optional<Submission> findLatest(long assignmentId, String studentNumber) {
        return submissionRepository.findFirstByAssignmentIdAndStudentNumberOrderByAttemptNumberDesc(
                assignmentId, studentNumber);
    }

    /**
     * 每次重新提交都会创建新的提交记录，保留之前提交供教师对照。
     */
    @Transactional
    public Submission submit(long assignmentId, String studentName, String studentNumber,
                             String content) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        if (content == null || content.strip().length() < 20) {
            throw new IllegalArgumentException("提交内容不能少于 20 个字符");
        }

        int attempt = findLatest(assignmentId, studentNumber)
                .map(previous -> previous.getAttemptNumber() + 1)
                .orElse(1);
        long nextId = submissionRepository.findTopByOrderByIdDesc()
                .map(previous -> previous.getId() + 1)
                .orElse(1L);
        String cleanContent = content.strip();
        Submission submission = new Submission(
                nextId,
                assignment.getId(),
                assignment.getTitle(),
                assignment.getCourseName(),
                studentName.strip(),
                studentNumber.strip(),
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
