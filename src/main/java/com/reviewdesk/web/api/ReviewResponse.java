package com.reviewdesk.web.api;

import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.service.ReviewService;
import java.time.Instant;

/**
 * 当前审核意见的接口结构。
 */
public record ReviewResponse(
        long submissionId,
        String title,
        String content,
        long revision,
        Instant updatedAt,
        int characterCount,
        long versionCount
) {

    public static ReviewResponse from(ReviewDraft draft, long versionCount) {
        return new ReviewResponse(
                draft.getSubmissionId(),
                draft.getTitle(),
                draft.getContent(),
                draft.getRevision(),
                draft.getUpdatedAt(),
                ReviewService.countCharacters(draft.getContent()),
                versionCount);
    }
}

