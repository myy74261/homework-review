package com.reviewdesk.web.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 教师确认一条审核问题时提交的证据和反馈。
 */
public record CreateIssueRequest(
        @NotBlank @Size(max = 100) String rubricName,
        @NotBlank @Size(max = 2_000) String evidenceQuote,
        @NotBlank @Size(max = 2_000) String teacherNote
) {
}

