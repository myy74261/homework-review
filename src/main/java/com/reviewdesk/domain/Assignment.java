package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 教师发布的一次课程作业。
 */
@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long classroomId;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 100)
    private String courseName;

    @Lob
    @Column(nullable = false)
    private String instructions;

    @Column(nullable = false)
    private Instant dueAt;

    @Column(nullable = false)
    private Instant publishedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 20)
    private String submissionFormat;

    @Column(length = 30)
    private String aiPolicy;

    private Boolean aiDeclarationRequired;

    private Boolean sourceDeclarationRequired;

    private Boolean appealAllowed;

    protected Assignment() {
        // JPA 需要的无参构造方法
    }

    public Assignment(Long classroomId, String title, String courseName,
                      String instructions, Instant dueAt) {
        this(classroomId, title, courseName, instructions, dueAt,
                "DOCUMENT", "ASSISTED_ALLOWED", true, true, true);
    }

    public Assignment(Long classroomId, String title, String courseName,
                      String instructions, Instant dueAt, String submissionFormat,
                      String aiPolicy, boolean aiDeclarationRequired,
                      boolean sourceDeclarationRequired, boolean appealAllowed) {
        this.classroomId = classroomId;
        this.title = title;
        this.courseName = courseName;
        this.instructions = instructions;
        this.dueAt = dueAt;
        this.publishedAt = Instant.now();
        this.status = "PUBLISHED";
        this.submissionFormat = submissionFormat;
        this.aiPolicy = aiPolicy;
        this.aiDeclarationRequired = aiDeclarationRequired;
        this.sourceDeclarationRequired = sourceDeclarationRequired;
        this.appealAllowed = appealAllowed;
    }

    public Long getId() {
        return id;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public String getTitle() {
        return title;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getInstructions() {
        return instructions;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getStatus() {
        return status;
    }

    public String getSubmissionFormat() {
        return submissionFormat == null ? "DOCUMENT" : submissionFormat;
    }

    public String getAiPolicy() {
        return aiPolicy == null ? "ASSISTED_ALLOWED" : aiPolicy;
    }

    public boolean isAiDeclarationRequired() {
        return Boolean.TRUE.equals(aiDeclarationRequired);
    }

    public boolean isSourceDeclarationRequired() {
        return Boolean.TRUE.equals(sourceDeclarationRequired);
    }

    public boolean isAppealAllowed() {
        return !Boolean.FALSE.equals(appealAllowed);
    }

    /**
     * 撤回已发布作业。学生端在重新发布前不再展示该作业。
     */
    public void withdraw() {
        this.status = "WITHDRAWN";
    }

    /**
     * 使用教师修改后的内容重新发布，并保留原作业编号。
     */
    public void updateAndRepublish(Long classroomId, String title, String courseName,
                                   String instructions, Instant dueAt,
                                   String submissionFormat, String aiPolicy,
                                   boolean aiDeclarationRequired,
                                   boolean sourceDeclarationRequired,
                                   boolean appealAllowed) {
        this.classroomId = classroomId;
        this.title = title;
        this.courseName = courseName;
        this.instructions = instructions;
        this.dueAt = dueAt;
        this.publishedAt = Instant.now();
        this.status = "PUBLISHED";
        this.submissionFormat = submissionFormat;
        this.aiPolicy = aiPolicy;
        this.aiDeclarationRequired = aiDeclarationRequired;
        this.sourceDeclarationRequired = sourceDeclarationRequired;
        this.appealAllowed = appealAllowed;
    }
}

