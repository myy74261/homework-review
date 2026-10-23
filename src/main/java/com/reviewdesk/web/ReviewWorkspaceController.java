package com.reviewdesk.web;

import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.AccessControlService;
import com.reviewdesk.service.ReviewActivityService;
import com.reviewdesk.service.ReviewService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    public ReviewWorkspaceController(ReviewService reviewService,
                                     AssignmentService assignmentService,
                                     ReviewActivityService activityService,
                                     AccessControlService accessControlService) {
        this.reviewService = reviewService;
        this.assignmentService = assignmentService;
        this.activityService = activityService;
        this.accessControlService = accessControlService;
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

    private String formatTime(java.time.Instant instant) {
        return DATE_TIME.format(instant.atZone(DISPLAY_ZONE));
    }
}

