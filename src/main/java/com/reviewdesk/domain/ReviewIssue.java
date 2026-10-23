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
 * 教师确认的一处作业问题，用于后续汇总共性问题并生成讲解建议。
 */
@Entity
@Table(name = "review_issues", indexes = {
        @Index(name = "idx_review_issues_submission", columnList = "submission_id"),
        @Index(name = "idx_review_issues_rubric", columnList = "rubric_name")
})
public class ReviewIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false, length = 100)
    private String rubricName;

    @Lob
    @Column(nullable = false)
    private String evidenceQuote;

    @Lob
    @Column(nullable = false)
    private String teacherNote;

    @Column(nullable = false)
    private Instant createdAt;

    protected ReviewIssue() {
        // JPA 需要的无参构造方法
    }

    public ReviewIssue(Long submissionId, String rubricName, String evidenceQuote,
                       String teacherNote) {
        this.submissionId = submissionId;
        this.rubricName = rubricName;
        this.evidenceQuote = evidenceQuote;
        this.teacherNote = teacherNote;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
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

    public String getTeacherNote() {
        return teacherNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

