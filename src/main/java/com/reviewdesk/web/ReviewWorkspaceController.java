package com.reviewdesk.web;

import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.service.AccessControlService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ReviewActivityService;
import com.reviewdesk.service.IntegrityService;
import com.reviewdesk.service.ReviewScoreService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.SubmissionAttachmentService;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 渲染教师作业审核工作台。
 *
 * <p>学生提交只读展示，教师反馈来自服务端，并可进入反馈版本流程。</p>
 */
@Controller
public class ReviewWorkspaceController {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("M 月 d 日 HH:mm", Locale.CHINA);

    private final ReviewService reviewService;
    private final AssignmentService assignmentService;
    private final ReviewActivityService activityService;
    private final AccessControlService accessControlService;
    private final ReviewScoreService scoreService;
    private final IntegrityService integrityService;
    private final SubmissionAttachmentService attachmentService;

    public ReviewWorkspaceController(ReviewService reviewService,
                                     AssignmentService assignmentService,
                                     ReviewActivityService activityService,
                                     AccessControlService accessControlService,
                                     ReviewScoreService scoreService,
                                     IntegrityService integrityService,
                                     SubmissionAttachmentService attachmentService) {
        this.reviewService = reviewService;
        this.assignmentService = assignmentService;
        this.activityService = activityService;
        this.accessControlService = accessControlService;
        this.scoreService = scoreService;
        this.integrityService = integrityService;
        this.attachmentService = attachmentService;
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        boolean teacher = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_TEACHER".equals(authority.getAuthority()));
        return "redirect:" + (teacher ? "/teacher" : "/student");
    }

    @GetMapping("/reviews/{submissionId}")
    public String workspace(@PathVariable long submissionId,
                            Authentication authentication, Model model) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        Submission submission = reviewService.requireSubmission(submissionId);
        ReviewDraft draft = reviewService.requireDraft(submissionId);
        activityService.record(submissionId, "SUBMISSION_VIEWED", "教师打开审核工作台");
        model.addAttribute("submission", submission);
        model.addAttribute("review", draft);
        model.addAttribute("scores", scoreService.getOrCreate(submissionId));
        model.addAttribute("finalScoreTotal", scoreService.finalTotal(submissionId));
        model.addAttribute("integrityFlags", integrityService.list(submissionId));
        model.addAttribute("attachments", attachmentService.list(submissionId));
        model.addAttribute("formatText", switch (assignmentService
                .requireAssignment(submission.getAssignmentId()).getSubmissionFormat()) {
            case "CODE" -> "代码提交";
            case "SCREENSHOT" -> "截图或设计提交";
            case "MIXED" -> "混合形式提交";
            default -> "文档提交";
        });
        model.addAttribute("reviewChecklist", reviewChecklist(assignmentService
                .requireAssignment(submission.getAssignmentId()).getSubmissionFormat()));
        model.addAttribute("versionCount", reviewService.countVersions(submissionId));
        model.addAttribute("dueAt", formatTime(submission.getDueAt()));
        model.addAttribute("submittedAt", formatTime(submission.getSubmittedAt()));
        model.addAttribute("attempt", "第 " + submission.getAttemptNumber() + " 次提交");
        model.addAttribute("wordCount",
                String.format(Locale.CHINA, "%,d", submission.getWordCount()));
        List<RubricCriterion> rubric = assignmentService.listRubrics(submission.getAssignmentId())
                .stream()
                .map(criterion -> new RubricCriterion(
                        criterion.getName(),
                        criterion.getPoints(),
                        criterion.getName().contains("证据") || criterion.getName().contains("方法")
                                ? "待核对" : "待确认",
                        criterion.getDescription()))
                .toList();
        model.addAttribute("rubric", rubric);
        return "review-workspace";
    }

    private List<String> reviewChecklist(String submissionFormat) {
        return switch (submissionFormat) {
            case "CODE" -> List.of(
                    "先运行或阅读代码，确认功能、边界条件与错误处理。",
                    "对照量规检查结构、可读性和关键实现说明。",
                    "只在需要时补充诚信核查，避免重复填写文档类审核项。");
            case "SCREENSHOT" -> List.of(
                    "打开截图或设计稿，核对目标、交互状态与可读性。",
                    "将视觉证据与量规维度一一对应。",
                    "如无文字稿，可跳过文档结构类重复检查。");
            case "MIXED" -> List.of(
                    "按“附件证据 → 文字说明 → 量规评分”的顺序审核。",
                    "优先检查文档、代码和截图之间是否互相印证。",
                    "仅对可疑来源或 AI 使用扩项，不重复审核已验证内容。");
            default -> List.of(
                    "先核对作业要求与评分量规，再阅读提交全文。",
                    "定位具体证据后填写反馈，避免泛泛评价。",
                    "按需核查引用与 AI 使用声明。");
        };
    }

    private String formatTime(java.time.Instant instant) {
        return DATE_TIME.format(instant.atZone(DISPLAY_ZONE));
    }
}

