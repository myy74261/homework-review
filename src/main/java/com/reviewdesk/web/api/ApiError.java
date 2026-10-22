package com.reviewdesk.web.api;

/**
 * 审核接口的统一错误结构。
 */
public record ApiError(String code, String message, Long currentRevision) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}

