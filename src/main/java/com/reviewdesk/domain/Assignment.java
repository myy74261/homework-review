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

    protected Assignment() {
        // JPA 需要的无参构造方法
    }

    public Assignment(String title, String courseName, String instructions,
                      Instant dueAt) {
        this.title = title;
        this.courseName = courseName;
        this.instructions = instructions;
        this.dueAt = dueAt;
        this.publishedAt = Instant.now();
        this.status = "PUBLISHED";
    }

    public Long getId() {
        return id;
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
}

