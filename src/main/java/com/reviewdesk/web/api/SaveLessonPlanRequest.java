package com.reviewdesk.web.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 教师更新备课稿时提交的数据。
 */
public record SaveLessonPlanRequest(
        @NotBlank @Size(max = 240) String title,
        @NotBlank @Size(max = 100_000) String content,
        @Min(0) long baseRevision
) {
}

