package com.reviewdesk.web.spa;

import java.time.Instant;
import java.util.List;

/**
 * React 单页前端使用的稳定 JSON 契约。
 *
 * <p>这些记录不直接暴露持久化实体，字段变更时由服务端适配。</p>
 */
public final class SpaViewModels {

    private SpaViewModels() {
    }

    public record UserView(long id, String username, String displayName,
                           String role, String studentNumber, String email,
                           String department, String professionalTitle) {
    }

    public record SessionView(UserView user, String csrfToken, String csrfHeader,
                              String homePath) {
    }

    public record ClassroomView(long id, String name, String courseName,
                                String joinCode, long memberCount,
                                Instant startAt, Instant endAt, String status,
                                long rosterCount, long linkedRosterCount,
                                long unlistedMemberCount) {
    }

    public record AssignmentView(long id, long classroomId, String title,
                                 String courseName, String instructions,
                                 Instant dueAt, String submissionFormat,
                                 String aiPolicy, boolean aiDeclarationRequired,
                                 boolean sourceDeclarationRequired,
                                 boolean appealAllowed, String status) {
    }

    public record RubricView(long id, String name, int points, String description) {
    }

    public record EditableAssignmentView(AssignmentView assignment,
                                         List<RubricView> rubrics,
                                         long submissionCount) {
    }

    public record SubmissionView(long id, long assignmentId, String assignmentTitle,
                                 String studentName, String studentNumber,
                                 String content, int wordCount, int attemptNumber,
                                 Instant dueAt, Instant submittedAt, Instant reviewedAt,
                                 String reviewStatus, String aiUsage, String aiTools,
                                 String aiScope, String sourceDeclaration,
                                 String collaborationDeclaration,
                                 boolean integrityConfirmed) {
    }

    public record AttachmentView(long id, String originalName, String contentType,
                                 long fileSize, String attachmentKind, String url) {
    }

    public record ReviewDraftView(String title, String content, long revision,
                                  Instant updatedAt, long versionCount) {
    }

    public record ScoreView(long id, long rubricId, String rubricName, int maxPoints,
                            int suggestedPoints, Integer finalPoints, String suggestion) {
    }

    public record IntegrityFlagView(long id, String category, String severity,
                                    String evidence, String reason, String status,
                                    Instant createdAt, Instant resolvedAt) {
    }

    public record AppealView(long id, long submissionId, String reason,
                             String requestedResolution, String status,
                             String teacherResponse, Instant createdAt,
                             Instant respondedAt) {
    }

    public record IssueView(long id, String rubricName, String evidenceQuote,
                            String teacherNote, Instant createdAt) {
    }

    public record AssignmentOverview(AssignmentView assignment, long submissionCount) {
    }

    public record SubmissionOverview(long id, String studentName, String studentNumber,
                                     String assignmentTitle, int attemptNumber,
                                     Instant submittedAt, String reviewStatus,
                                     String statusText) {
    }

    public record CommonIssueView(String rubricName, long count) {
    }

    public record AiBatchProgressView(
            long assignmentId,
            long total,
            long completed,
            long running,
            long pending,
            long failed,
            long skipped
    ) {
    }

    public record AppealOverview(AppealView appeal, String studentName,
                                 String assignmentTitle) {
    }

    public record AiReviewSuggestionView(
            long id,
            String rubricName,
            String evidenceQuote,
            String suggestion,
            Integer suggestedPoints,
            String status,
            Instant createdAt,
            Instant decidedAt
    ) {
    }

    public record AiReviewRunView(
            long id,
            String model,
            String summary,
            Instant createdAt,
            List<AiReviewSuggestionView> suggestions
    ) {
    }

    public record TeacherDashboard(UserView user, List<ClassroomView> classrooms,
                                   List<AssignmentOverview> assignments,
                                   List<SubmissionOverview> submissions,
                                   List<CommonIssueView> commonIssues,
                                   List<AppealOverview> appeals, long reviewedCount,
                                   boolean aiConfigured,
                                   List<AiBatchProgressView> aiBatchProgress,
                                   long enrolledStudentCount,
                                   long submittedStudentCount) {
    }

    public record TeacherReviewWorkspace(
            AssignmentView assignment,
            SubmissionView submission,
            ReviewDraftView review,
            List<AttachmentView> attachments,
            List<RubricView> rubrics,
            List<ScoreView> scores,
            Integer finalScoreTotal,
            List<IntegrityFlagView> integrityFlags,
            List<IssueView> issues,
            List<String> reviewChecklist,
            boolean aiConfigured,
            List<AiReviewRunView> aiReviews
    ) {
    }

    public record StudentAssignmentOverview(AssignmentView assignment,
                                            SubmissionView latestSubmission,
                                            String statusText,
                                            boolean activeAppeal) {
    }

    public record StudentDashboard(UserView user, List<ClassroomView> classrooms,
                                   List<StudentAssignmentOverview> assignments) {
    }

    public record StudentAssignmentDetail(
            AssignmentView assignment,
            List<RubricView> rubrics,
            SubmissionView latestSubmission,
            List<AttachmentView> attachments,
            ReviewDraftView review,
            List<ScoreView> scores,
            Integer finalScoreTotal,
            List<IntegrityFlagView> confirmedIntegrityFlags,
            List<AppealView> appeals,
            boolean activeAppeal
    ) {
    }

    public record LessonIssueGroup(String rubricName, long count,
                                   List<String> teacherNotes) {
    }

    public record LessonWorkspace(AssignmentView assignment,
                                  List<RubricView> rubrics,
                                  String title, String content, long revision,
                                  Instant updatedAt, int evidenceCount,
                                  List<LessonIssueGroup> issueGroups,
                                  boolean aiConfigured) {
    }
}
