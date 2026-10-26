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
 * 一次 AI 作业预审记录。模型输出只作为教师建议，不进入最终结果。
 */
@Entity
@Table(name = "ai_review_runs", indexes = {
        @Index(name = "idx_ai_review_runs_submission", columnList = "submission_id")
})
public class AiReviewRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false, length = 100)
    private String model;

    @Lob
    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private Instant createdAt;

    protected AiReviewRun() {
        // JPA 需要的无参构造方法
    }

    public AiReviewRun(Long submissionId, String model, String summary) {
        this.submissionId = submissionId;
        this.model = model;
        this.summary = summary;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getModel() {
        return model;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
