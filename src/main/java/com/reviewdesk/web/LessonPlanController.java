package com.reviewdesk.web;

import com.reviewdesk.service.AccessControlService;
import com.reviewdesk.service.TeachingSuggestionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 渲染教师可编辑的课堂讲解建议页面。
 */
@Controller
public class LessonPlanController {

    private final TeachingSuggestionService suggestionService;
    private final AccessControlService accessControlService;

    public LessonPlanController(TeachingSuggestionService suggestionService,
                                AccessControlService accessControlService) {
        this.suggestionService = suggestionService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/teacher/assignments/{assignmentId}/lesson-plan")
    public String lessonPlan(@PathVariable long assignmentId,
                             Authentication authentication, Model model) {
        accessControlService.requireTeacherAssignment(authentication.getName(), assignmentId);
        TeachingSuggestionService.LessonPlanWorkspace workspace =
                suggestionService.getOrCreateWorkspace(assignmentId);
        model.addAttribute("assignment", workspace.assignment());
        model.addAttribute("plan", workspace.plan());
        model.addAttribute("evidence", workspace.evidence());
        model.addAttribute("groups", workspace.groups());
        return "lesson-plan";
    }
}

