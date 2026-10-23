package com.reviewdesk.web;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ClassroomService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.StudentSubmissionService;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
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

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("M 月 d 日 HH:mm", Locale.CHINA);

    private final AssignmentService assignmentService;
    private final StudentSubmissionService submissionService;
    private final ReviewService reviewService;
    private final AccountService accountService;
    private final ClassroomService classroomService;

    public StudentWorkspaceController(AssignmentService assignmentService,
                                      StudentSubmissionService submissionService,
                                      ReviewService reviewService,
                                      AccountService accountService,
                                      ClassroomService classroomService) {
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.reviewService = reviewService;
        this.accountService = accountService;
        this.classroomService = classroomService;
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
                    return new StudentAssignmentView(
                            assignment,
                            latest,
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
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        if (!classroomService.isStudentMember(student, assignment.getClassroomId())) {
            throw new SecurityException("尚未加入该作业所属课堂");
        }
        model.addAttribute("assignment", assignment);
        model.addAttribute("dueAtText", formatTime(assignment.getDueAt()));
        model.addAttribute("rubrics", assignmentService.listRubrics(assignmentId));
        Submission latestSubmission = submissionService
                .findLatest(assignmentId, student)
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
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        Account student = accountService.current(authentication);
        try {
            submissionService.submit(assignmentId, student, content);
            redirectAttributes.addFlashAttribute("successMessage", "作业已提交，等待教师审核。");
        } catch (IllegalArgumentException | SecurityException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
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
        return DATE_TIME.format(instant.atZone(DISPLAY_ZONE));
    }

    public record StudentAssignmentView(Assignment assignment, Submission latestSubmission,
                                        String dueAtText) {
    }
}
