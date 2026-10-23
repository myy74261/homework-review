package com.reviewdesk.web.api;

import com.reviewdesk.domain.ReviewIssue;
import java.time.Instant;

/**
 * 已保存的教师问题标记。
 */
public record IssueResponse(
        long id,
        long submissionId,
        String rubricName,
        String evidenceQuote,
        String teacherNote,
        Instant createdAt
) {

    public static IssueResponse from(ReviewIssue issue) {
        return new IssueResponse(
                issue.getId(),
                issue.getSubmissionId(),
                issue.getRubricName(),
                issue.getEvidenceQuote(),
                issue.getTeacherNote(),
                issue.getCreatedAt());
    }
}

