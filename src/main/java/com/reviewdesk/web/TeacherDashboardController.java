package com.reviewdesk.web;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.AppealService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ClassroomService;
import com.reviewdesk.service.TeachingWorkspaceService;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 教师端作业发布、提交总览和审核入口。
 */
@Controller
public class TeacherDashboardController {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");

    private final TeachingWorkspaceService workspaceService;
    private final AssignmentService assignmentService;
    private final AccountService accountService;
    private final ClassroomService classroomService;
    private final AppealService appealService;

    public TeacherDashboardController(TeachingWorkspaceService workspaceService,
                                      AssignmentService assignmentService,
                                      AccountService accountService,
                                      ClassroomService classroomService,
                                      AppealService appealService) {
        this.workspaceService = workspaceService;
        this.assignmentService = assignmentService;
        this.accountService = accountService;
        this.classroomService = classroomService;
        this.appealService = appealService;
    }

    @GetMapping("/teacher")
    public String dashboard(Authentication authentication, Model model) {
        Account teacher = accountService.current(authentication);
        List<Classroom> classrooms = classroomService.teacherClassrooms(teacher);
        List<Long> classroomIds = classrooms.stream().map(Classroom::getId).toList();
        var submissions = workspaceService.listSubmissions(classroomIds);
        Map<Long, Long> classroomMembers = new LinkedHashMap<>();
        classrooms.forEach(classroom -> classroomMembers.put(
                classroom.getId(), classroomService.memberCount(classroom.getId())));
        model.addAttribute("classrooms", classrooms);
        model.addAttribute("classroomMembers", classroomMembers);
        model.addAttribute("assignments", workspaceService.listAssignments(classroomIds));
        model.addAttribute("submissions", submissions);
        model.addAttribute("reviewedCount", submissions.stream()
                .filter(row -> "已审核".equals(row.statusText()))
                .count());
        model.addAttribute("commonIssues", workspaceService.listCommonIssues(classroomIds));
        Map<Long, Submission> submissionById = submissions.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.submission().getId(),
                        row -> row.submission()));
        model.addAttribute("appeals", appealService.listForSubmissions(
                        submissionById.keySet()).stream()
                .map(appeal -> new AppealOverview(
                        appeal,
                        submissionById.get(appeal.getSubmissionId())))
                .toList());
        return "teacher-dashboard";
    }

    @GetMapping("/teacher/assignments/new")
    public String publishForm(Authentication authentication, Model model) {
        Account teacher = accountService.current(authentication);
        List<Classroom> classrooms = classroomService.teacherClassrooms(teacher);
        if (classrooms.isEmpty()) {
            return "redirect:/teacher/classrooms/new";
        }
        model.addAttribute("classrooms", classrooms);
        model.addAttribute("form", PublishAssignmentForm.defaults(classrooms.get(0).getId()));
        return "publish-assignment";
    }

    @PostMapping("/teacher/assignments")
    public String publish(@Valid @ModelAttribute("form") PublishAssignmentForm form,
                          BindingResult bindingResult, Authentication authentication,
                          Model model) {
        Account teacher = accountService.current(authentication);
        List<Classroom> classrooms = classroomService.teacherClassrooms(teacher);
        model.addAttribute("classrooms", classrooms);
        if (bindingResult.hasErrors()) {
            return "publish-assignment";
        }
        if (!classroomService.isTeacherOwner(teacher, form.getClassroomId())) {
            bindingResult.reject("classroom.invalid", "无权向该课堂发布作业");
            return "publish-assignment";
        }
        List<AssignmentService.RubricDefinition> rubrics = form.getRubrics().stream()
                .map(rubric -> new AssignmentService.RubricDefinition(
                        rubric.getName(), rubric.getPoints(), rubric.getDescription()))
                .toList();
        try {
            Assignment assignment = assignmentService.publish(
                    form.getClassroomId(),
                    form.getTitle(),
                    form.getCourseName(),
                    form.getInstructions(),
                    form.getDueAt().atZone(DISPLAY_ZONE).toInstant(),
                    form.getSubmissionFormat(),
                    form.getAiPolicy(),
                    form.isAiDeclarationRequired(),
                    form.isSourceDeclarationRequired(),
                    form.isAppealAllowed(),
                    rubrics);
            return "redirect:/teacher?published=" + assignment.getId();
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("assignment.invalid", exception.getMessage());
            return "publish-assignment";
        }
    }

    @GetMapping("/teacher/classrooms/new")
    public String classroomForm() {
        return "create-classroom";
    }

    @PostMapping("/teacher/classrooms")
    public String createClassroom(Authentication authentication,
                                  @RequestParam String name,
                                  @RequestParam String courseName) {
        Account teacher = accountService.current(authentication);
        classroomService.create(teacher, name, courseName);
        return "redirect:/teacher?classroomCreated";
    }

    public record AppealOverview(com.reviewdesk.domain.Appeal appeal,
                                 Submission submission) {
    }
}

