package com.reviewdesk.web;

import com.reviewdesk.domain.Assignment;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.TeachingWorkspaceService;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 教师端作业发布、提交总览和审核入口。
 */
@Controller
public class TeacherDashboardController {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");

    private final TeachingWorkspaceService workspaceService;
    private final AssignmentService assignmentService;

    public TeacherDashboardController(TeachingWorkspaceService workspaceService,
                                      AssignmentService assignmentService) {
        this.workspaceService = workspaceService;
        this.assignmentService = assignmentService;
    }

    @GetMapping("/teacher")
    public String dashboard(Model model) {
        var submissions = workspaceService.listSubmissions();
        model.addAttribute("assignments", workspaceService.listAssignments());
        model.addAttribute("submissions", submissions);
        model.addAttribute("reviewedCount", submissions.stream()
                .filter(row -> "已审核".equals(row.statusText()))
                .count());
        model.addAttribute("commonIssues", workspaceService.listCommonIssues());
        return "teacher-dashboard";
    }

    @GetMapping("/teacher/assignments/new")
    public String publishForm(Model model) {
        model.addAttribute("form", PublishAssignmentForm.defaults());
        return "publish-assignment";
    }

    @PostMapping("/teacher/assignments")
    public String publish(@Valid @ModelAttribute("form") PublishAssignmentForm form,
                          BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "publish-assignment";
        }
        List<AssignmentService.RubricDefinition> rubrics = List.of(
                new AssignmentService.RubricDefinition(
                        form.criterion1Name(), form.criterion1Points(),
                        form.criterion1Description()),
                new AssignmentService.RubricDefinition(
                        form.criterion2Name(), form.criterion2Points(),
                        form.criterion2Description()),
                new AssignmentService.RubricDefinition(
                        form.criterion3Name(), form.criterion3Points(),
                        form.criterion3Description()),
                new AssignmentService.RubricDefinition(
                        form.criterion4Name(), form.criterion4Points(),
                        form.criterion4Description()));
        try {
            Assignment assignment = assignmentService.publish(
                    form.title(),
                    form.courseName(),
                    form.instructions(),
                    form.dueAt().atZone(DISPLAY_ZONE).toInstant(),
                    rubrics);
            return "redirect:/teacher?published=" + assignment.getId();
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("assignment.invalid", exception.getMessage());
            return "publish-assignment";
        }
    }
}

