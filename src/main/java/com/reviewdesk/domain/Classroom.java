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
 * 教师创建、学生通过课堂码加入的教学班级。
 */
@Entity
@Table(name = "classrooms", indexes = {
        @Index(name = "idx_classrooms_teacher", columnList = "teacher_id"),
        @Index(name = "idx_classrooms_join_code", columnList = "join_code", unique = true)
})
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String courseName;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "join_code", nullable = false, unique = true, length = 12)
    private String joinCode;

    @Column(nullable = false)
    private Instant createdAt;

    protected Classroom() {
        // JPA 需要的无参构造方法
    }

    public Classroom(String name, String courseName, Long teacherId, String joinCode) {
        this.name = name;
        this.courseName = courseName;
        this.teacherId = teacherId;
        this.joinCode = joinCode;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCourseName() {
        return courseName;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

