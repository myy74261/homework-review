package com.reviewdesk.web.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 保存审核意见时提交的数据。
 *
 * <p>{@code baseRevision} 用于阻止过期页面静默覆盖较新的反馈。</p>
 */
public record SaveReviewRequest(
        @NotBlank(message = "审核意见标题不能为空")
        @Size(max = 200, message = "审核意见标题不能超过 200 个字符")
        String title,

        @NotNull(message = "审核意见不能为空")
        @Size(max = 200_000, message = "审核意见不能超过 200000 个字符")
        String content,

        @Min(value = 0, message = "版本号无效")
        long baseRevision
) {
}

