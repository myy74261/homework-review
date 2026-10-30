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
 * 协作者针对同一份学生提交保存的独立审核草稿。
 */
@Entity
@Table(name = "collaborator_review_drafts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_collaborator_review_draft",
                columnNames = {"submission_id", "reviewer_account_id"}),
        indexes = @Index(name = "idx_collaborator_review_submission",
                columnList = "submission_id"))
public class CollaboratorReviewDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "reviewer_account_id", nullable = false)
    private Long reviewerAccountId;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private long revision;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CollaboratorReviewDraft() {
        // JPA 需要的无参构造方法
    }

    public CollaboratorReviewDraft(Long submissionId, Long reviewerAccountId,
                                    String title, String content) {
        this.submissionId = submissionId;
        this.reviewerAccountId = reviewerAccountId;
        this.title = title;
        this.content = content;
        this.revision = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String title, String content, long expectedRevision) {
        if (revision != expectedRevision) {
            throw new StaleReviewException(revision);
        }
        this.title = title;
        this.content = content;
        this.revision++;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public Long getReviewerAccountId() {
        return reviewerAccountId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
