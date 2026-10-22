package com.reviewdesk.web.api;

import com.reviewdesk.domain.ReviewVersion;
import java.time.Instant;

/**
 * 单个审核反馈版本的完整内容。
 */
public record VersionDetailResponse(
        long id,
        long versionNumber,
        String title,
        String content,
        String triggerType,
        Instant createdAt,
        int characterCount
) {

    public static VersionDetailResponse from(ReviewVersion version) {
        return new VersionDetailResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getTitle(),
                version.getContent(),
                version.getTriggerType(),
                version.getCreatedAt(),
                version.getCharacterCount());
    }
}

