package com.reviewdesk.web;

import com.reviewdesk.domain.LessonPlan;
import com.reviewdesk.service.AccessControlService;
import com.reviewdesk.service.TeachingSuggestionService;
import com.reviewdesk.web.api.LessonPlanResponse;
import com.reviewdesk.web.api.SaveLessonPlanRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 备课建议生成、自动保存和重新生成接口。
 */
@RestController
@RequestMapping("/api/assignments/{assignmentId}/lesson-plan")
public class LessonPlanApiController {

    private final TeachingSuggestionService suggestionService;
    private final AccessControlService accessControlService;

    public LessonPlanApiController(TeachingSuggestionService suggestionService,
                                   AccessControlService accessControlService) {
        this.suggestionService = suggestionService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public LessonPlanResponse getLessonPlan(@PathVariable long assignmentId,
                                            Authentication authentication) {
        accessControlService.requireTeacherAssignment(authentication.getName(), assignmentId);
        return LessonPlanResponse.from(
                suggestionService.getOrCreateWorkspace(assignmentId));
    }

    @PutMapping
    public LessonPlanResponse saveLessonPlan(@PathVariable long assignmentId,
                                             Authentication authentication,
                                             @Valid @RequestBody SaveLessonPlanRequest request) {
        accessControlService.requireTeacherAssignment(authentication.getName(), assignmentId);
        suggestionService.save(
                assignmentId, request.title(), request.content(), request.baseRevision());
        return LessonPlanResponse.from(
                suggestionService.getOrCreateWorkspace(assignmentId));
    }

    @PostMapping("/regenerate")
    public LessonPlanResponse regenerate(@PathVariable long assignmentId,
                                         Authentication authentication) {
        accessControlService.requireTeacherAssignment(authentication.getName(), assignmentId);
        LessonPlan plan = suggestionService.regenerate(
                assignmentId, authentication.getName());
        return LessonPlanResponse.from(
                suggestionService.getOrCreateWorkspace(plan.getAssignmentId()));
    }
}

