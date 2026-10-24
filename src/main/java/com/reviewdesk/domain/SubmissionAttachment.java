package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 学生提交中的一个文档、代码或截图附件。
 */
@Entity
@Table(name = "submission_attachments", indexes = {
        @Index(name = "idx_submission_attachments_submission",
                columnList = "submission_id")
})
public class SubmissionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false, length = 240)
    private String originalName;

    @Column(nullable = false, length = 240)
    private String storedName;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 20)
    private String attachmentKind;

    @Column(nullable = false)
    private Instant createdAt;

    protected SubmissionAttachment() {
        // JPA 需要的无参构造方法
    }

    public SubmissionAttachment(Long submissionId, String originalName, String storedName,
                                String contentType, long fileSize, String attachmentKind) {
        this.submissionId = submissionId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.attachmentKind = attachmentKind;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getAttachmentKind() {
        return attachmentKind;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

