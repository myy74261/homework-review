package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 截止后自动 AI 预审的幂等任务。
 *
 * <p>任务只生成教师可见建议，不发布成绩或反馈。</p>
 */
@Entity
@Table(name = "ai_batch_review_jobs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_batch_jobs_submission",
                columnNames = "submission_id"),
        indexes = {
                @Index(name = "idx_ai_batch_jobs_assignment", columnList = "assignment_id"),
                @Index(name = "idx_ai_batch_jobs_status", columnList = "status,next_attempt_at")
        })
public class AiBatchReviewJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long assignmentId;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = 300)
    private String errorMessage;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AiBatchReviewJob() {
        // JPA 需要的无参构造方法
    }

    public AiBatchReviewJob(Long assignmentId, Long submissionId) {
        this.assignmentId = assignmentId;
        this.submissionId = submissionId;
        this.status = "PENDING";
        this.attemptCount = 0;
        this.nextAttemptAt = Instant.now();
        this.createdAt = this.nextAttemptAt;
        this.updatedAt = this.nextAttemptAt;
    }

    public void markRunning() {
        this.status = "RUNNING";
        this.attemptCount++;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = "COMPLETED";
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void skip(String message) {
        this.status = "SKIPPED";
        this.errorMessage = message;
        this.updatedAt = Instant.now();
    }

    public void fail(String message, Instant retryAt, boolean canRetry) {
        this.status = canRetry ? "PENDING" : "FAILED";
        this.errorMessage = message;
        this.nextAttemptAt = retryAt;
        this.updatedAt = Instant.now();
    }

    /**
     * 作业截止时间变化时，调整尚未开始任务的执行时间。
     */
    public void postponeUntil(Instant nextAttemptAt) {
        if ("PENDING".equals(status)) {
            this.nextAttemptAt = nextAttemptAt;
            this.updatedAt = Instant.now();
        }
    }

    /**
     * 已领取但尚未调用模型的任务在截止条件不再成立时放回队列。
     */
    public void releaseClaimUntil(Instant nextAttemptAt) {
        if ("RUNNING".equals(status)) {
            this.status = "PENDING";
            this.attemptCount = Math.max(0, this.attemptCount - 1);
            this.nextAttemptAt = nextAttemptAt;
            this.updatedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
