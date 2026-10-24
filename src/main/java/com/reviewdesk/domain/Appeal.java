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
 * 学生对最终反馈、评分或诚信标记发起的申诉。
 */
@Entity
@Table(name = "appeals", indexes = {
        @Index(name = "idx_appeals_submission", columnList = "submission_id"),
        @Index(name = "idx_appeals_status", columnList = "status")
})
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private Long studentId;

    @Lob
    @Column(nullable = false)
    private String reason;

    @Lob
    private String requestedResolution;

    @Column(nullable = false, length = 20)
    private String status;

    @Lob
    private String teacherResponse;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant respondedAt;

    protected Appeal() {
        // JPA 需要的无参构造方法
    }

    public Appeal(Long submissionId, Long studentId, String reason,
                  String requestedResolution) {
        this.submissionId = submissionId;
        this.studentId = studentId;
        this.reason = reason;
        this.requestedResolution = requestedResolution;
        this.status = "SUBMITTED";
        this.createdAt = Instant.now();
    }

    public void respond(String status, String teacherResponse) {
        this.status = status;
        this.teacherResponse = teacherResponse;
        this.respondedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getReason() {
        return reason;
    }

    public String getRequestedResolution() {
        return requestedResolution;
    }

    public String getStatus() {
        return status;
    }

    public String getTeacherResponse() {
        return teacherResponse;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
