package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * AI 针对某个评分维度提出的预审建议。
 *
 * <p>PENDING 仅代表模型建议。只有教师主动采纳后才会转为 ReviewIssue。</p>
 */
@Entity
@Table(name = "ai_review_suggestions", indexes = {
        @Index(name = "idx_ai_suggestions_run", columnList = "run_id"),
        @Index(name = "idx_ai_suggestions_submission", columnList = "submission_id")
})
public class AiReviewSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false, length = 100)
    private String rubricName;

    @Lob
    @Column(nullable = false)
    private String evidenceQuote;

    @Lob
    @Column(nullable = false)
    private String suggestion;

    private Integer suggestedPoints;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;

    protected AiReviewSuggestion() {
        // JPA 需要的无参构造方法
    }

    public AiReviewSuggestion(Long runId, Long submissionId, String rubricName,
                              String evidenceQuote, String suggestion,
                              Integer suggestedPoints) {
        this.runId = runId;
        this.submissionId = submissionId;
        this.rubricName = rubricName;
        this.evidenceQuote = evidenceQuote;
        this.suggestion = suggestion;
        this.suggestedPoints = suggestedPoints;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public void decide(String status) {
        this.status = status;
        this.decidedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getRubricName() {
        return rubricName;
    }

    public String getEvidenceQuote() {
        return evidenceQuote;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public Integer getSuggestedPoints() {
        return suggestedPoints;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
