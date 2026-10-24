package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 教师对单个评分维度的规则建议分和最终分。
 */
@Entity
@Table(name = "review_scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_scores_submission_rubric",
                columnNames = {"submission_id", "rubric_id"}),
        indexes = @Index(name = "idx_review_scores_submission",
                columnList = "submission_id"))
public class ReviewScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private Long rubricId;

    @Column(nullable = false, length = 100)
    private String rubricName;

    @Column(nullable = false)
    private int maxPoints;

    @Column(nullable = false)
    private int suggestedPoints;

    private Integer finalPoints;

    @Lob
    @Column(nullable = false)
    private String suggestion;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ReviewScore() {
        // JPA 需要的无参构造方法
    }

    public ReviewScore(Long submissionId, Long rubricId, String rubricName,
                       int maxPoints, int suggestedPoints, String suggestion) {
        this.submissionId = submissionId;
        this.rubricId = rubricId;
        this.rubricName = rubricName;
        this.maxPoints = maxPoints;
        this.suggestedPoints = suggestedPoints;
        this.suggestion = suggestion;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void setFinalPoints(Integer finalPoints) {
        this.finalPoints = finalPoints;
        this.updatedAt = Instant.now();
    }

    /**
     * 教师确认新的薄弱项后刷新规则建议，不覆盖教师已经填写的最终分。
     */
    public void updateSuggestion(int suggestedPoints, String suggestion) {
        this.suggestedPoints = suggestedPoints;
        this.suggestion = suggestion;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public Long getRubricId() {
        return rubricId;
    }

    public String getRubricName() {
        return rubricName;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public int getSuggestedPoints() {
        return suggestedPoints;
    }

    public Integer getFinalPoints() {
        return finalPoints;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

