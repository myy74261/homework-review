package com.reviewdesk.service;

import com.reviewdesk.domain.AiReviewRun;
import com.reviewdesk.domain.AiReviewSuggestion;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.AssignmentRubric;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.AiReviewRunRepository;
import com.reviewdesk.repository.AiReviewSuggestionRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 预审生成和教师决策记录。
 *
 * <p>模型建议不会直接写反馈、评分或诚信结论。采纳动作必须由教师显式触发。</p>
 */
@Service
@Transactional(readOnly = true)
public class AiReviewService {

    private final AiSuggestionClient client;
    private final AiReviewRunRepository runRepository;
    private final AiReviewSuggestionRepository suggestionRepository;
    private final AccessControlService accessControlService;
    private final ReviewService reviewService;
    private final AssignmentService assignmentService;
    private final ReviewIssueService issueService;
    private final ReviewActivityService activityService;
    private final AiSettingsService settingsService;

    public AiReviewService(
            AiSuggestionClient client,
            AiReviewRunRepository runRepository,
            AiReviewSuggestionRepository suggestionRepository,
            AccessControlService accessControlService,
            ReviewService reviewService,
            AssignmentService assignmentService,
            ReviewIssueService issueService,
            ReviewActivityService activityService,
            AiSettingsService settingsService) {
        this.client = client;
        this.runRepository = runRepository;
        this.suggestionRepository = suggestionRepository;
        this.accessControlService = accessControlService;
        this.reviewService = reviewService;
        this.assignmentService = assignmentService;
        this.issueService = issueService;
        this.activityService = activityService;
        this.settingsService = settingsService;
    }

    public boolean isConfigured(String teacherUsername) {
        return settingsService.isConfigured(teacherUsername);
    }

    public List<ReviewRunView> list(long submissionId) {
        return runRepository.findBySubmissionIdOrderByCreatedAtDesc(submissionId).stream()
                .map(run -> new ReviewRunView(
                        run,
                        suggestionRepository.findByRunIdOrderByIdAsc(run.getId())))
                .toList();
    }

    @Transactional
    public ReviewRunView generate(String teacherUsername, long submissionId) {
        AiSuggestionClient.ProviderConfig provider =
                settingsService.resolve(teacherUsername);
        if (!provider.configured()) {
            throw new IllegalStateException(
                    "AI 模型尚未配置，请在教师端“个人中心 → AI 接口设置”中接入");
        }
        accessControlService.requireTeacherOwnerSubmission(teacherUsername, submissionId);
        Submission submission = reviewService.requireSubmission(submissionId);
        requireEditable(submission);
        Assignment assignment = assignmentService.requireAssignment(
                submission.getAssignmentId());
        List<AssignmentRubric> rubrics = assignmentService.listRubrics(
                assignment.getId());
        String rubricSummary = rubrics.stream()
                .map(rubric -> "- " + rubric.getName() + "（满分 "
                        + rubric.getPoints() + "）：" + rubric.getDescription())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

        AiSuggestionClient.ReviewResult result = client.reviewSubmission(
                        provider,
                        assignment.getTitle(),
                        assignment.getSubmissionFormat(),
                        submission.getContent(),
                        submission.getAiUsage(),
                        submission.getAiTools(),
                        submission.getAiScope(),
                        submission.getSourceDeclaration(),
                        submission.getCollaborationDeclaration(),
                        rubricSummary)
                .orElseThrow(() -> new IllegalStateException(
                        "AI 预审暂时没有返回有效建议，请检查模型配置后重试"));

        suggestionRepository.findBySubmissionIdAndStatus(submissionId, "PENDING")
                .forEach(suggestion -> {
                    suggestion.decide("SUPERSEDED");
                    suggestionRepository.save(suggestion);
                });
        AiReviewRun run = runRepository.save(new AiReviewRun(
                submissionId, provider.model(), result.summary()));
        List<AiReviewSuggestion> suggestions = result.suggestions().stream()
                .map(item -> toSuggestion(run, submissionId, rubrics, item))
                .flatMap(Optional::stream)
                .map(suggestionRepository::save)
                .toList();
        if (suggestions.isEmpty()) {
            throw new IllegalStateException("AI 返回的建议没有匹配到有效评分维度");
        }
        activityService.record(
                submissionId,
                "AI_REVIEW_GENERATED",
                provider.model() + " 生成 " + suggestions.size() + " 条预审建议");
        return new ReviewRunView(run, suggestions);
    }

    @Transactional
    public AiReviewSuggestion adopt(String teacherUsername, long suggestionId,
                                    String teacherNote) {
        AiReviewSuggestion suggestion = requireSuggestion(suggestionId);
        accessControlService.requireTeacherOwnerSubmission(
                teacherUsername, suggestion.getSubmissionId());
        requireEditable(reviewService.requireSubmission(suggestion.getSubmissionId()));
        if (!"PENDING".equals(suggestion.getStatus())) {
            throw new IllegalStateException("该 AI 建议已经处理");
        }
        String finalNote = teacherNote == null || teacherNote.isBlank()
                ? suggestion.getSuggestion() : teacherNote.strip();
        issueService.create(
                suggestion.getSubmissionId(),
                suggestion.getRubricName(),
                suggestion.getEvidenceQuote(),
                finalNote);
        suggestion.decide("ADOPTED");
        suggestionRepository.save(suggestion);
        activityService.record(
                suggestion.getSubmissionId(),
                "AI_SUGGESTION_ADOPTED",
                suggestion.getRubricName());
        return suggestion;
    }

    @Transactional
    public AiReviewSuggestion dismiss(String teacherUsername, long suggestionId) {
        AiReviewSuggestion suggestion = requireSuggestion(suggestionId);
        accessControlService.requireTeacherOwnerSubmission(
                teacherUsername, suggestion.getSubmissionId());
        requireEditable(reviewService.requireSubmission(suggestion.getSubmissionId()));
        if ("PENDING".equals(suggestion.getStatus())) {
            suggestion.decide("DISMISSED");
            suggestionRepository.save(suggestion);
            activityService.record(
                    suggestion.getSubmissionId(),
                    "AI_SUGGESTION_DISMISSED",
                    suggestion.getRubricName());
        }
        return suggestion;
    }

    private Optional<AiReviewSuggestion> toSuggestion(
            AiReviewRun run,
            long submissionId,
            List<AssignmentRubric> rubrics,
            AiSuggestionClient.ReviewSuggestion item) {
        AssignmentRubric rubric = rubrics.stream()
                .filter(candidate -> candidate.getName().equals(item.rubricName()))
                .findFirst()
                .or(() -> rubrics.stream()
                        .filter(candidate -> normalize(candidate.getName())
                                .contains(normalize(item.rubricName()))
                                || normalize(item.rubricName())
                                .contains(normalize(candidate.getName())))
                        .findFirst())
                .orElse(null);
        if (rubric == null) {
            return Optional.empty();
        }
        Integer points = item.suggestedPoints() == null
                ? null : Math.max(0, Math.min(rubric.getPoints(), item.suggestedPoints()));
        return Optional.of(new AiReviewSuggestion(
                run.getId(),
                submissionId,
                rubric.getName(),
                item.evidenceQuote(),
                item.suggestion(),
                points));
    }

    private String normalize(String value) {
        return value == null ? "" : value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s：:，,。._-]", "");
    }

    private AiReviewSuggestion requireSuggestion(long suggestionId) {
        return suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("AI 预审建议不存在"));
    }

    private void requireEditable(Submission submission) {
        if ("REVIEWED".equals(submission.getReviewStatus())) {
            throw new IllegalStateException("反馈已发布，请通过学生申诉复核流程调整");
        }
    }

    public record ReviewRunView(AiReviewRun run, List<AiReviewSuggestion> suggestions) {
    }
}
