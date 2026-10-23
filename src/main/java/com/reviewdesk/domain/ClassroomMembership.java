package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 学生与课堂的成员关系。
 */
@Entity
@Table(name = "classroom_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_classroom_student",
                columnNames = {"classroom_id", "student_id"}),
        indexes = @Index(name = "idx_memberships_student", columnList = "student_id"))
public class ClassroomMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Instant joinedAt;

    protected ClassroomMembership() {
        // JPA 需要的无参构造方法
    }

    public ClassroomMembership(Long classroomId, Long studentId) {
        this.classroomId = classroomId;
        this.studentId = studentId;
        this.joinedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
