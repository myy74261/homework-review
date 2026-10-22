package com.reviewdesk.web.api;

import com.reviewdesk.domain.ReviewVersion;
import com.reviewdesk.service.ReviewDiffService;
import java.util.List;

/**
 * 历史反馈与当前反馈的逐行比较结果。
 */
public record DiffResponse(
        long versionId,
        int added,
        int removed,
        List<DiffLineResponse> lines
) {

    public static DiffResponse from(ReviewVersion version,
                                    ReviewDiffService.DiffResult result) {
        return new DiffResponse(
                version.getId(),
                result.added(),
                result.removed(),
                result.lines().stream()
                        .map(line -> new DiffLineResponse(line.type(), line.text()))
                        .toList());
    }
}

