package com.reviewdesk.web;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.AppealService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ClassroomService;
import com.reviewdesk.service.IntegrityService;
import com.reviewdesk.service.ReviewScoreService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.StudentSubmissionService;
import com.reviewdesk.service.SubmissionAttachmentService;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 竞赛演示用的学生端，只负责查看作业、提交与查看最终状态。
 */
@Controller
public class StudentWorkspaceController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StudentWorkspaceController.class);
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("M 月 d 日 HH:mm", Locale.CHINA);

    private final AssignmentService assignmentService;
    private final StudentSubmissionService submissionService;
    private final ReviewService reviewService;
    private final AccountService accountService;
    private final ClassroomService classroomService;
    private final SubmissionAttachmentService attachmentService;
    private final ReviewScoreService scoreService;
    private final IntegrityService integrityService;
    private final AppealService appealService;

    public StudentWorkspaceController(AssignmentService assignmentService,
                                      StudentSubmissionService submissionService,
                                      ReviewService reviewService,
                                      AccountService accountService,
                                      ClassroomService classroomService,
                                      SubmissionAttachmentService attachmentService,
                                      ReviewScoreService scoreService,
                                      IntegrityService integrityService,
                                      AppealService appealService) {
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.reviewService = reviewService;
        this.accountService = accountService;
        this.classroomService = classroomService;
        this.attachmentService = attachmentService;
        this.scoreService = scoreService;
        this.integrityService = integrityService;
        this.appealService = appealService;
    }

    @GetMapping("/student")
    public String home(Authentication authentication, Model model) {
        Account student = accountService.current(authentication);
        List<Classroom> classrooms = classroomService.studentClassrooms(student);
        List<Long> classroomIds = classrooms.stream().map(Classroom::getId).toList();
        List<StudentAssignmentView> assignments =
                assignmentService.listForClassrooms(classroomIds).stream()
                .map(assignment -> {
                    Submission latest = submissionService
                            .findLatest(assignment.getId(), student)
                            .orElse(null);
                    boolean activeAppeal = latest != null
                            && appealService.listForSubmission(latest.getId()).stream()
                            .anyMatch(appeal -> List.of("SUBMITTED", "UNDER_REVIEW")
                                    .contains(appeal.getStatus()));
                    return new StudentAssignmentView(
                            assignment,
                            latest,
                            activeAppeal,
                            formatTime(assignment.getDueAt()));
                })
                .toList();
        model.addAttribute("student", student);
        model.addAttribute("classrooms", classrooms);
        model.addAttribute("assignments", assignments);
        return "student-home";
    }

    @GetMapping("/student/assignments/{assignmentId}")
    public String assignment(@PathVariable long assignmentId,
                             Authentication authentication, Model model) {
        Account student = accountService.current(authentication);
        Assignment assignment = assignmentService.requirePublishedAssignment(assignmentId);
        if (!classroomService.isStudentMember(student, assignment.getClassroomId())) {
            throw new SecurityException("尚未加入该作业所属课堂");
        }
        model.addAttribute("assignment", assignment);
        model.addAttribute("dueAtText", formatTime(assignment.getDueAt()));
        model.addAttribute("submissionFormatText", switch (assignment.getSubmissionFormat()) {
            case "CODE" -> "代码提交";
            case "SCREENSHOT" -> "截图或设计提交";
            case "MIXED" -> "混合形式提交";
            default -> "文档提交";
        });
        model.addAttribute("aiPolicyText", switch (assignment.getAiPolicy()) {
            case "NOT_ALLOWED" -> "不允许使用 AI";
            case "GENERATION_ALLOWED" -> "允许 AI 生成内容，但必须声明";
            default -> "允许 AI 辅助，但必须声明使用范围";
        });
        model.addAttribute("rubrics", assignmentService.listRubrics(assignmentId));
        Submission latestSubmission = submissionService
                .findLatest(assignmentId, student)
                .orElse(null);
        model.addAttribute("latestSubmission", latestSubmission);
        model.addAttribute("attachments", latestSubmission == null
                ? List.of() : attachmentService.list(latestSubmission.getId()));
        model.addAttribute("latestSubmittedAtText",
                latestSubmission == null ? null : formatTime(latestSubmission.getSubmittedAt()));
        model.addAttribute("reviewedAtText",
                latestSubmission == null ? null : formatTime(latestSubmission.getReviewedAt()));
        model.addAttribute("reviewFeedback",
                latestSubmission != null && "REVIEWED".equals(latestSubmission.getReviewStatus())
                        ? reviewService.requireDraft(latestSubmission.getId())
                        : null);
        model.addAttribute("reviewScores",
                latestSubmission != null && "REVIEWED".equals(latestSubmission.getReviewStatus())
                        ? scoreService.list(latestSubmission.getId()) : List.of());
        model.addAttribute("finalScoreTotal",
                latestSubmission != null && "REVIEWED".equals(latestSubmission.getReviewStatus())
                        ? scoreService.finalTotal(latestSubmission.getId()) : null);
        model.addAttribute("confirmedIntegrityFlags",
                latestSubmission != null && "REVIEWED".equals(latestSubmission.getReviewStatus())
                        ? integrityService.confirmedForStudent(latestSubmission.getId())
                        : List.of());
        List<com.reviewdesk.domain.Appeal> appealRecords = latestSubmission == null
                ? List.of() : appealService.listForSubmission(latestSubmission.getId());
        model.addAttribute("hasActiveAppeal", appealRecords.stream()
                .anyMatch(appeal -> List.of("SUBMITTED", "UNDER_REVIEW")
                        .contains(appeal.getStatus())));
        model.addAttribute("appeals", appealRecords.stream()
                .map(appeal -> new StudentAppealView(
                        appeal.getReason(),
                        appeal.getRequestedResolution(),
                        appeal.getTeacherResponse(),
                        appealStatusText(appeal.getStatus()),
                        formatTime(appeal.getCreatedAt())))
                .toList());
        return "student-assignment";
    }

    @PostMapping("/student/assignments/{assignmentId}/submissions")
    public String submit(@PathVariable long assignmentId,
                         @RequestParam(defaultValue = "") String content,
                         @RequestParam(required = false) String aiUsage,
                         @RequestParam(required = false) String aiTools,
                         @RequestParam(required = false) String aiScope,
                         @RequestParam(required = false) String sourceDeclaration,
                         @RequestParam(required = false) String collaborationDeclaration,
                         @RequestParam(defaultValue = "false") boolean integrityConfirmed,
                         @RequestParam(required = false) MultipartFile[] files,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        Account student = accountService.current(authentication);
        try {
            submissionService.submit(
                    assignmentId,
                    student,
                    content,
                    aiUsage,
                    aiTools,
                    aiScope,
                    sourceDeclaration,
                    collaborationDeclaration,
                    integrityConfirmed,
                    files);
            redirectAttributes.addFlashAttribute("successMessage", "作业已提交，等待教师审核。");
        } catch (IOException exception) {
            LOGGER.error("学生提交附件保存失败，assignmentId={}", assignmentId, exception);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "附件保存失败，请联系教师或管理员检查上传目录。");
        } catch (IllegalArgumentException | SecurityException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/assignments/" + assignmentId;
    }

    /**
     * 页面内异步提交，避免部分内嵌浏览器阻止表单整页跳转。
     */
    @PostMapping("/student/assignments/{assignmentId}/submissions/ajax")
    public ResponseEntity<SubmissionAjaxResponse> submitAjax(
            @PathVariable long assignmentId,
            @RequestParam(defaultValue = "") String content,
            @RequestParam(required = false) String aiUsage,
            @RequestParam(required = false) String aiTools,
            @RequestParam(required = false) String aiScope,
            @RequestParam(required = false) String sourceDeclaration,
            @RequestParam(required = false) String collaborationDeclaration,
            @RequestParam(defaultValue = "false") boolean integrityConfirmed,
            @RequestParam(required = false) MultipartFile[] files,
            Authentication authentication) {
        Account student = accountService.current(authentication);
        try {
            submissionService.submit(
                    assignmentId,
                    student,
                    content,
                    aiUsage,
                    aiTools,
                    aiScope,
                    sourceDeclaration,
                    collaborationDeclaration,
                    integrityConfirmed,
                    files);
            return ResponseEntity.ok(new SubmissionAjaxResponse(
                    true,
                    "作业已提交，等待教师审核。",
                    "/student/assignments/" + assignmentId));
        } catch (IOException exception) {
            LOGGER.error("学生异步提交附件保存失败，assignmentId={}", assignmentId, exception);
            return ResponseEntity.internalServerError().body(new SubmissionAjaxResponse(
                    false,
                    "附件保存失败，请联系教师或管理员检查上传目录。",
                    "/student/assignments/" + assignmentId));
        } catch (IllegalArgumentException | SecurityException exception) {
            return ResponseEntity.badRequest().body(new SubmissionAjaxResponse(
                    false,
                    exception.getMessage(),
                    "/student/assignments/" + assignmentId));
        }
    }

    /**
     * 浏览器刷新或直接打开提交接口时回到作业页，避免把只接受 POST 的地址显示成错误页。
     */
    @GetMapping("/student/assignments/{assignmentId}/submissions")
    public String submissionRefresh(@PathVariable long assignmentId) {
        return "redirect:/student/assignments/" + assignmentId;
    }

    @PostMapping("/student/join")
    public String joinClassroom(@RequestParam String joinCode,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Account student = accountService.current(authentication);
        try {
            Classroom classroom = classroomService.join(student, joinCode);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "已加入课堂：" + classroom.getName());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student";
    }

    private String formatTime(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return DATE_TIME.format(instant.atZone(DISPLAY_ZONE));
    }

    private String appealStatusText(String status) {
        return switch (status) {
            case "UNDER_REVIEW" -> "教师正在复核";
            case "ACCEPTED" -> "申诉已采纳";
            case "REJECTED" -> "申诉未采纳";
            default -> "已提交，等待复核";
        };
    }

    public record StudentAppealView(String reason, String requestedResolution,
                                    String teacherResponse, String statusText,
                                    String createdAtText) {
    }

    public record SubmissionAjaxResponse(boolean success, String message,
                                         String redirectUrl) {
    }

    public record StudentAssignmentView(Assignment assignment, Submission latestSubmission,
                                        boolean activeAppeal, String dueAtText) {

        public boolean isReviewed() {
            return latestSubmission != null
                    && "REVIEWED".equals(latestSubmission.getReviewStatus());
        }

        public String getStatusText() {
            if (latestSubmission == null) {
                return "待提交";
            }
            if (activeAppeal) {
                return "申诉处理中";
            }
            return isReviewed() ? "已反馈" : "待审核";
        }

        public String getStatusClass() {
            if (latestSubmission == null) {
                return "status-pending";
            }
            if (activeAppeal) {
                return "status-reviewing";
            }
            return isReviewed() ? "status-submitted" : "status-reviewing";
        }
    }
}
