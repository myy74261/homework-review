package com.reviewdesk.web;

import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.StudentSubmissionService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 竞赛演示用的学生端，只负责查看作业、提交与查看最终状态。
 */
@Controller
public class StudentWorkspaceController {

    private static final String DEMO_STUDENT_NAME = "林清和";
    private static final String DEMO_STUDENT_NUMBER = "20230116";
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("M 月 d 日 HH:mm", Locale.CHINA);

    private final AssignmentService assignmentService;
    private final StudentSubmissionService submissionService;
    private final ReviewService reviewService;

    public StudentWorkspaceController(AssignmentService assignmentService,
                                      StudentSubmissionService submissionService,
                                      ReviewService reviewService) {
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.reviewService = reviewService;
    }

    @GetMapping("/student")
    public String home(Model model) {
        List<StudentAssignmentView> assignments = assignmentService.listAssignments().stream()
                .map(assignment -> {
                    Submission latest = submissionService
                            .findLatest(assignment.getId(), DEMO_STUDENT_NUMBER)
                            .orElse(null);
                    return new StudentAssignmentView(
                            assignment,
                            latest,
                            formatTime(assignment.getDueAt()));
                })
                .toList();
        model.addAttribute("studentName", DEMO_STUDENT_NAME);
        model.addAttribute("studentNumber", DEMO_STUDENT_NUMBER);
        model.addAttribute("assignments", assignments);
        return "student-home";
    }

    @GetMapping("/student/assignments/{assignmentId}")
    public String assignment(@PathVariable long assignmentId, Model model) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        model.addAttribute("assignment", assignment);
        model.addAttribute("dueAtText", formatTime(assignment.getDueAt()));
        model.addAttribute("rubrics", assignmentService.listRubrics(assignmentId));
        model.addAttribute("studentName", DEMO_STUDENT_NAME);
        model.addAttribute("studentNumber", DEMO_STUDENT_NUMBER);
        Submission latestSubmission = submissionService
                .findLatest(assignmentId, DEMO_STUDENT_NUMBER)
                .orElse(null);
        model.addAttribute("latestSubmission", latestSubmission);
        model.addAttribute("reviewFeedback",
                latestSubmission != null && "REVIEWED".equals(latestSubmission.getReviewStatus())
                        ? reviewService.requireDraft(latestSubmission.getId())
                        : null);
        return "student-assignment";
    }

    @PostMapping("/student/assignments/{assignmentId}/submissions")
    public String submit(@PathVariable long assignmentId,
                         @RequestParam String content,
                         RedirectAttributes redirectAttributes) {
        try {
            submissionService.submit(
                    assignmentId, DEMO_STUDENT_NAME, DEMO_STUDENT_NUMBER, content);
            redirectAttributes.addFlashAttribute("successMessage", "作业已提交，等待教师审核。");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/assignments/" + assignmentId;
    }

    private String formatTime(java.time.Instant instant) {
        return DATE_TIME.format(instant.atZone(DISPLAY_ZONE));
    }

    public record StudentAssignmentView(Assignment assignment, Submission latestSubmission,
                                        String dueAtText) {
    }
}
