package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 学生的一次课程作业提交。
 *
 * <p>提交内容在审核阶段只读，教师反馈单独保存在 {@link ReviewDraft} 中。</p>
 */
@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long assignmentId;

    @Column(nullable = false)
    private Long studentAccountId;

    @Column(nullable = false, length = 240)
    private String assignmentTitle;

    @Column(nullable = false, length = 100)
    private String courseName;

    @Column(nullable = false, length = 80)
    private String studentName;

    @Column(nullable = false, length = 40)
    private String studentNumber;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int wordCount;

    @Column(nullable = false)
    private int attemptNumber;

    @Column(nullable = false, length = 160)
    private String attachmentName;

    @Column(nullable = false)
    private Instant dueAt;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false, length = 20)
    private String reviewStatus;

    @Column
    private Instant reviewedAt;

    protected Submission() {
        // JPA 需要的无参构造方法
    }

    public Submission(Long id, Long assignmentId, Long studentAccountId,
                      String assignmentTitle, String courseName, String studentName,
                      String studentNumber, String content, int wordCount, int attemptNumber,
                      String attachmentName, Instant dueAt, Instant submittedAt) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentAccountId = studentAccountId;
        this.assignmentTitle = assignmentTitle;
        this.courseName = courseName;
        this.studentName = studentName;
        this.studentNumber = studentNumber;
        this.content = content;
        this.wordCount = wordCount;
        this.attemptNumber = attemptNumber;
        this.attachmentName = attachmentName;
        this.dueAt = dueAt;
        this.submittedAt = submittedAt;
        this.reviewStatus = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public Long getStudentAccountId() {
        return studentAccountId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getContent() {
        return content;
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    /**
     * 标记教师已经完成本轮审核。
     */
    public void markReviewed() {
        this.reviewStatus = "REVIEWED";
        this.reviewedAt = Instant.now();
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}

