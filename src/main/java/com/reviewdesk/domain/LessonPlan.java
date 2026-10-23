package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 教师根据共性问题编辑的备课稿。
 *
 * <p>一个作业对应一份当前备课稿；重新生成与人工编辑都会递增版本号。</p>
 */
@Entity
@Table(name = "lesson_plans")
public class LessonPlan {

    @Id
    private Long assignmentId;

    @Column(nullable = false, length = 240)
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

    protected LessonPlan() {
        // JPA 需要的无参构造方法
    }

    public LessonPlan(Long assignmentId, String title, String content) {
        this.assignmentId = assignmentId;
        this.title = title;
        this.content = content;
        this.revision = 0;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void update(String title, String content, long expectedRevision) {
        if (revision != expectedRevision) {
            throw new StaleLessonPlanException(revision);
        }
        this.title = title;
        this.content = content;
        this.revision++;
        this.updatedAt = Instant.now();
    }

    public void replaceGenerated(String title, String content) {
        this.title = title;
        this.content = content;
        this.revision++;
        this.updatedAt = Instant.now();
    }

    public Long getAssignmentId() {
        return assignmentId;
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

