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
 * 一次教师审核反馈的完整快照。
 */
@Entity
@Table(name = "review_versions", indexes = {
        @Index(name = "idx_review_versions_submission",
                columnList = "submission_id, version_number")
})
public class ReviewVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private long versionNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int characterCount;

    @Column(nullable = false, length = 20)
    private String triggerType;

    @Column(nullable = false)
    private Instant createdAt;

    protected ReviewVersion() {
        // JPA 需要的无参构造方法
    }

    public ReviewVersion(Long submissionId, long versionNumber, String title, String content,
                         int characterCount, String triggerType) {
        this.submissionId = submissionId;
        this.versionNumber = versionNumber;
        this.title = title;
        this.content = content;
        this.characterCount = characterCount;
        this.triggerType = triggerType;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public long getVersionNumber() {
        return versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

