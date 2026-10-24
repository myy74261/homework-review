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
 * 教师记录的可疑学术诚信问题。
 *
 * <p>该记录表示待核实线索，只有教师确认后才能向学生展示。</p>
 */
@Entity
@Table(name = "integrity_flags", indexes = {
        @Index(name = "idx_integrity_flags_submission", columnList = "submission_id")
})
public class IntegrityFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 20)
    private String severity;

    @Lob
    @Column(nullable = false)
    private String evidence;

    @Lob
    @Column(nullable = false)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    protected IntegrityFlag() {
        // JPA 需要的无参构造方法
    }

    public IntegrityFlag(Long submissionId, String category, String severity,
                         String evidence, String reason) {
        this.submissionId = submissionId;
        this.category = category;
        this.severity = severity;
        this.evidence = evidence;
        this.reason = reason;
        this.status = "SUSPECTED";
        this.createdAt = Instant.now();
    }

    public void resolve(String status) {
        this.status = status;
        this.resolvedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getCategory() {
        return category;
    }

    public String getSeverity() {
        return severity;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}

