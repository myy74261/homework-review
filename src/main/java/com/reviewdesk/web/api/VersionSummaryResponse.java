package com.reviewdesk.web.api;

import com.reviewdesk.domain.ReviewVersion;
import java.time.Instant;

/**
 * 审核记录列表中的一条反馈版本。
 */
public record VersionSummaryResponse(
        long id,
        long versionNumber,
        String triggerType,
        Instant createdAt,
        int characterCount
) {

    public static VersionSummaryResponse from(ReviewVersion version) {
        return new VersionSummaryResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getTriggerType(),
                version.getCreatedAt(),
                version.getCharacterCount());
    }
}

