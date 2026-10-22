package com.reviewdesk.web;

import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.service.ReviewService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
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

    public ReviewWorkspaceController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/reviews/" + ReviewService.DEMO_SUBMISSION_ID;
    }

    @GetMapping("/reviews/{submissionId}")
    public String workspace(@PathVariable long submissionId, Model model) {
        Submission submission = reviewService.requireSubmission(submissionId);
        ReviewDraft draft = reviewService.requireDraft(submissionId);
        model.addAttribute("submission", submission);
        model.addAttribute("review", draft);
        model.addAttribute("versionCount", reviewService.countVersions(submissionId));
        model.addAttribute("dueAt", formatTime(submission.getDueAt()));
        model.addAttribute("submittedAt", formatTime(submission.getSubmittedAt()));
        model.addAttribute("attempt", "第 " + submission.getAttemptNumber() + " 次提交");
        model.addAttribute("wordCount",
                String.format(Locale.CHINA, "%,d", submission.getWordCount()));
        model.addAttribute("rubric", List.of(
                new RubricCriterion("研究问题与目标", 25, "达成", "问题、目标与研究边界是否清楚"),
                new RubricCriterion("证据与引用", 25, "需补充", "结论是否有可追溯的数据或文献支持"),
                new RubricCriterion("方法与数据", 30, "需补充", "样本、抽样、回收率与统计过程是否透明"),
                new RubricCriterion("结构与表达", 20, "达成", "论证顺序、段落衔接与学术表达是否清晰")
        ));
        return "review-workspace";
    }

    private String formatTime(java.time.Instant instant) {
        return DATE_TIME.format(instant.atZone(DISPLAY_ZONE));
    }
}

