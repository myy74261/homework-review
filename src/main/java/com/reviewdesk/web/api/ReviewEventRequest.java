package com.reviewdesk.web.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 前端上报一条页面级审核动作。
 */
public record ReviewEventRequest(
        @NotBlank @Size(max = 40) String eventType,
        @Size(max = 240) String detail
) {
}

