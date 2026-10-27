package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.ReviewDraftRepository;
import com.reviewdesk.repository.SubmissionRepository;
import java.io.IOException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final SubmissionAttachmentService attachmentService;

    public StudentSubmissionService(AssignmentService assignmentService,
                                    SubmissionRepository submissionRepository,
                                    ReviewDraftRepository draftRepository,
                                    ReviewService reviewService,
                                    ReviewActivityService activityService,
                                    ClassroomService classroomService,
                                    SubmissionAttachmentService attachmentService) {
        this.assignmentService = assignmentService;
        this.submissionRepository = submissionRepository;
        this.draftRepository = draftRepository;
        this.reviewService = reviewService;
        this.activityService = activityService;
        this.classroomService = classroomService;
        this.attachmentService = attachmentService;
    }

    public Optional<Submission> findLatest(long assignmentId, Account student) {
        return submissionRepository
                .findFirstByAssignmentIdAndStudentAccountIdOrderByAttemptNumberDesc(
                        assignmentId, student.getId());
    }

    /**
     * 每次重新提交都会创建新的提交记录，保留之前提交供教师对照。
     */
    public Submission submit(long assignmentId, Account student, String content) {
        try {
            return submit(assignmentId, student, content,
                    "NONE", null, null, null, null, false, new MultipartFile[0]);
        } catch (IOException exception) {
            throw new IllegalStateException("附件保存失败", exception);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Submission submit(long assignmentId, Account student, String content,
                             String aiUsage, String aiTools, String aiScope,
                             String sourceDeclaration, String collaborationDeclaration,
                             boolean integrityConfirmed, MultipartFile[] files)
            throws IOException {
        Assignment assignment = assignmentService.requirePublishedAssignment(assignmentId);
        if (!classroomService.isStudentMember(student, assignment.getClassroomId())) {
            throw new SecurityException("尚未加入该作业所属课堂");
        }
        String cleanContent = content == null ? "" : content.strip();
        int fileCount = files == null
                ? 0 : (int) java.util.Arrays.stream(files).filter(file -> !file.isEmpty()).count();
        if (cleanContent.length() < 20 && fileCount == 0) {
            throw new IllegalArgumentException("请填写至少 20 个字符或上传附件");
        }
        String cleanAiUsage = aiUsage == null || aiUsage.isBlank() ? "NONE" : aiUsage;
        if (assignment.isAiDeclarationRequired() && !"NONE".equals(cleanAiUsage)
                && (aiTools == null || aiTools.isBlank())) {
            throw new IllegalArgumentException("请说明使用的 AI 工具");
        }
        if ("NOT_ALLOWED".equals(assignment.getAiPolicy()) && !"NONE".equals(cleanAiUsage)) {
            throw new IllegalArgumentException("该作业不允许使用 AI");
        }
        if (assignment.isSourceDeclarationRequired()
                && (sourceDeclaration == null || sourceDeclaration.isBlank())) {
            throw new IllegalArgumentException("请填写文献与数据来源声明");
        }
        if ((assignment.isAiDeclarationRequired()
                || assignment.isSourceDeclarationRequired()) && !integrityConfirmed) {
            throw new IllegalArgumentException("请确认诚信与 AI 使用声明");
        }

        int attempt = findLatest(assignmentId, student)
                .map(previous -> previous.getAttemptNumber() + 1)
                .orElse(1);
        long nextId = submissionRepository.findTopByOrderByIdDesc()
                .map(previous -> previous.getId() + 1)
                .orElse(1L);
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
                fileCount > 0 ? "多附件提交" : "在线提交",
                assignment.getDueAt(),
                java.time.Instant.now(),
                cleanAiUsage,
                cleanText(aiTools),
                cleanText(aiScope),
                cleanText(sourceDeclaration),
                cleanText(collaborationDeclaration),
                integrityConfirmed);
        submissionRepository.save(submission);
        attachmentService.store(submission.getId(), files);
        draftRepository.save(new ReviewDraft(
                submission.getId(),
                assignment.getTitle() + "审核意见",
                "待教师填写"));
        reviewService.createVersion(submission.getId(), "INITIAL");
        activityService.record(submission.getId(), "SUBMISSION_CREATED", "学生在线提交");
        return submission;
    }

    private String cleanText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
