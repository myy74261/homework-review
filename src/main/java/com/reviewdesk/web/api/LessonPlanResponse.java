package com.reviewdesk.web.api;

import com.reviewdesk.service.TeachingSuggestionService;
import java.time.Instant;
import java.util.List;

/**
 * 备课稿及问题依据的接口结构。
 */
public record LessonPlanResponse(
        long assignmentId,
        String title,
        String content,
        long revision,
        Instant updatedAt,
        int evidenceCount,
        List<IssueGroupResponse> issueGroups
) {

    public static LessonPlanResponse from(
            TeachingSuggestionService.LessonPlanWorkspace workspace) {
        return new LessonPlanResponse(
                workspace.assignment().getId(),
                workspace.plan().getTitle(),
                workspace.plan().getContent(),
                workspace.plan().getRevision(),
                workspace.plan().getUpdatedAt(),
                workspace.evidence().size(),
                workspace.groups().stream()
                        .map(group -> new IssueGroupResponse(
                                group.rubricName(), group.count()))
                        .toList());
    }

    public record IssueGroupResponse(String rubricName, long count) {
    }
}

