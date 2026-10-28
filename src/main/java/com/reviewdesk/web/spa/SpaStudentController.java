package com.reviewdesk.web.spa;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Appeal;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.IntegrityFlag;
import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.ReviewScore;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.domain.SubmissionAttachment;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.AppealService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ClassroomService;
import com.reviewdesk.service.IntegrityService;
import com.reviewdesk.service.ReviewScoreService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.StudentSubmissionService;
import com.reviewdesk.service.SubmissionAttachmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * React 学生端页面所需的 JSON 接口。
 */
@RestController
@RequestMapping("/api/app/student")
public class SpaStudentController {

    private final AccountService accountService;
    private final ClassroomService classroomService;
    private final AssignmentService assignmentService;
    private final StudentSubmissionService submissionService;
    private final ReviewService reviewService;
    private final SubmissionAttachmentService attachmentService;
    private final ReviewScoreService scoreService;
    private final IntegrityService integrityService;
    private final AppealService appealService;

    public SpaStudentController(
            AccountService accountService,
            ClassroomService classroomService,
            AssignmentService assignmentService,
            StudentSubmissionService submissionService,
            ReviewService reviewService,
            SubmissionAttachmentService attachmentService,
            ReviewScoreService scoreService,
            IntegrityService integrityService,
            AppealService appealService) {
        this.accountService = accountService;
        this.classroomService = classroomService;
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.reviewService = reviewService;
        this.attachmentService = attachmentService;
        this.scoreService = scoreService;
        this.integrityService = integrityService;
        this.appealService = appealService;
    }

    @GetMapping("/dashboard")
    public SpaViewModels.StudentDashboard dashboard(Authentication authentication) {
        Account student = accountService.current(authentication);
        List<Classroom> classrooms = classroomService.studentClassrooms(student);
        List<Long> classroomIds = classrooms.stream().map(Classroom::getId).toList();
        List<SpaViewModels.StudentAssignmentOverview> assignments =
                assignmentService.listForClassrooms(classroomIds).stream()
                        .map(assignment -> {
                            Submission latest = submissionService
                                    .findLatest(assignment.getId(), student)
                                    .orElse(null);
                            boolean activeAppeal = latest != null
                                    && appealService.listForSubmission(latest.getId()).stream()
                                    .anyMatch(appeal -> isActiveAppeal(appeal.getStatus()));
                            return new SpaViewModels.StudentAssignmentOverview(
                                    toAssignment(assignment),
                                    latest == null ? null : toSubmission(latest),
                                    studentStatus(latest, activeAppeal),
                                    activeAppeal);
                        })
                        .toList();
        return new SpaViewModels.StudentDashboard(
                toUser(student),
                classrooms.stream()
                        .map(classroom -> new SpaViewModels.ClassroomView(
                                classroom.getId(),
                                classroom.getName(),
                                classroom.getCourseName(),
                                classroom.getJoinCode(),
                                classroomService.memberCount(classroom.getId()),
                                classroom.getStartAt(),
                                classroom.getEndAt(),
                                classroomService.lifecycleStatus(classroom),
                                classroomService.rosterCount(classroom.getId()),
                                classroomService.linkedRosterCount(classroom.getId())))
                        .toList(),
                assignments);
    }

    @PostMapping("/classrooms/join")
    public SpaViewModels.ClassroomView joinClassroom(
            Authentication authentication,
            @Valid @RequestBody JoinClassroomRequest request) {
        Classroom classroom = classroomService.join(
                accountService.current(authentication), request.joinCode());
        return new SpaViewModels.ClassroomView(
                classroom.getId(), classroom.getName(), classroom.getCourseName(),
                classroom.getJoinCode(),
                classroomService.memberCount(classroom.getId()),
                classroom.getStartAt(), classroom.getEndAt(),
                classroomService.lifecycleStatus(classroom),
                classroomService.rosterCount(classroom.getId()),
                classroomService.linkedRosterCount(classroom.getId()));
    }

    @GetMapping("/assignments/{assignmentId}")
    public SpaViewModels.StudentAssignmentDetail assignment(
            @PathVariable long assignmentId,
            Authentication authentication) {
        Account student = accountService.current(authentication);
        Assignment assignment = assignmentService.requirePublishedAssignment(assignmentId);
        if (!classroomService.isStudentMember(student, assignment.getClassroomId())) {
            throw new SecurityException("尚未加入该作业所属课堂");
        }
        Submission latest = submissionService
                .findLatest(assignmentId, student)
                .orElse(null);
        boolean reviewed = latest != null
                && "REVIEWED".equals(latest.getReviewStatus());
        List<Appeal> appealRecords = latest == null
                ? List.of()
                : appealService.listForSubmission(latest.getId());
        boolean activeAppeal = appealRecords.stream()
                .anyMatch(appeal -> isActiveAppeal(appeal.getStatus()));
        ReviewDraft review = reviewed
                ? reviewService.requireDraft(latest.getId())
                : null;
        return new SpaViewModels.StudentAssignmentDetail(
                toAssignment(assignment),
                assignmentService.listRubrics(assignmentId).stream()
                        .map(rubric -> new SpaViewModels.RubricView(
                                rubric.getId(), rubric.getName(), rubric.getPoints(),
                                rubric.getDescription()))
                        .toList(),
                latest == null ? null : toSubmission(latest),
                latest == null
                        ? List.of()
                        : attachmentService.list(latest.getId()).stream()
                        .map(this::toAttachment)
                        .toList(),
                review == null ? null : new SpaViewModels.ReviewDraftView(
                        review.getTitle(), review.getContent(), review.getRevision(),
                        review.getUpdatedAt(), reviewService.countVersions(latest.getId())),
                reviewed
                        ? scoreService.list(latest.getId()).stream()
                        .map(this::toScore)
                        .toList()
                        : List.of(),
                reviewed ? scoreService.finalTotal(latest.getId()) : null,
                reviewed
                        ? integrityService.confirmedForStudent(latest.getId()).stream()
                        .map(this::toIntegrityFlag)
                        .toList()
                        : List.of(),
                appealRecords.stream().map(this::toAppeal).toList(),
                activeAppeal);
    }

    @PostMapping("/submissions/{submissionId}/appeals")
    public SpaViewModels.AppealView submitAppeal(
            @PathVariable long submissionId,
            Authentication authentication,
            @Valid @RequestBody SubmitAppealRequest request) {
        Account student = accountService.current(authentication);
        return toAppeal(appealService.submit(
                submissionId,
                student.getId(),
                request.reason(),
                request.requestedResolution()));
    }

    private boolean isActiveAppeal(String status) {
        return "SUBMITTED".equals(status) || "UNDER_REVIEW".equals(status);
    }

    private String studentStatus(Submission submission, boolean activeAppeal) {
        if (submission == null) {
            return "待提交";
        }
        if (activeAppeal) {
            return "申诉处理中";
        }
        return "REVIEWED".equals(submission.getReviewStatus()) ? "已反馈" : "待审核";
    }

    private SpaViewModels.UserView toUser(Account account) {
        return new SpaViewModels.UserView(
                account.getId(), account.getUsername(), account.getDisplayName(),
                account.getRole(), account.getStudentNumber(), account.getEmail(),
                account.getDepartment(), account.getProfessionalTitle());
    }

    private SpaViewModels.AssignmentView toAssignment(Assignment assignment) {
        return new SpaViewModels.AssignmentView(
                assignment.getId(), assignment.getClassroomId(), assignment.getTitle(),
                assignment.getCourseName(), assignment.getInstructions(),
                assignment.getDueAt(), assignment.getSubmissionFormat(),
                assignment.getAiPolicy(), assignment.isAiDeclarationRequired(),
                assignment.isSourceDeclarationRequired(), assignment.isAppealAllowed(),
                assignment.getStatus());
    }

    private SpaViewModels.SubmissionView toSubmission(Submission submission) {
        return new SpaViewModels.SubmissionView(
                submission.getId(), submission.getAssignmentId(),
                submission.getAssignmentTitle(), submission.getStudentName(),
                submission.getStudentNumber(), submission.getContent(),
                submission.getWordCount(), submission.getAttemptNumber(),
                submission.getDueAt(), submission.getSubmittedAt(),
                submission.getReviewedAt(), submission.getReviewStatus(),
                submission.getAiUsage(), submission.getAiTools(), submission.getAiScope(),
                submission.getSourceDeclaration(),
                submission.getCollaborationDeclaration(),
                submission.isIntegrityConfirmed());
    }

    private SpaViewModels.AttachmentView toAttachment(
            SubmissionAttachment attachment) {
        return new SpaViewModels.AttachmentView(
                attachment.getId(), attachment.getOriginalName(),
                attachment.getContentType(), attachment.getFileSize(),
                attachment.getAttachmentKind(),
                "/files/submissions/" + attachment.getSubmissionId()
                        + "/attachments/" + attachment.getId());
    }

    private SpaViewModels.ScoreView toScore(ReviewScore score) {
        return new SpaViewModels.ScoreView(
                score.getId(), score.getRubricId(), score.getRubricName(),
                score.getMaxPoints(), score.getSuggestedPoints(),
                score.getFinalPoints(), score.getSuggestion());
    }

    private SpaViewModels.IntegrityFlagView toIntegrityFlag(IntegrityFlag flag) {
        return new SpaViewModels.IntegrityFlagView(
                flag.getId(), flag.getCategory(), flag.getSeverity(),
                flag.getEvidence(), flag.getReason(), flag.getStatus(),
                flag.getCreatedAt(), flag.getResolvedAt());
    }

    private SpaViewModels.AppealView toAppeal(Appeal appeal) {
        return new SpaViewModels.AppealView(
                appeal.getId(), appeal.getSubmissionId(), appeal.getReason(),
                appeal.getRequestedResolution(), appeal.getStatus(),
                appeal.getTeacherResponse(), appeal.getCreatedAt(),
                appeal.getRespondedAt());
    }

    public record JoinClassroomRequest(
            @NotBlank(message = "请输入课堂码") String joinCode) {
    }

    public record SubmitAppealRequest(
            @NotBlank(message = "请填写申诉理由") String reason,
            String requestedResolution) {
    }
}
