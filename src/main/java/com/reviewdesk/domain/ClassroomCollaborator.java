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
 * 课堂协作者使用自己的教师账号参与课堂审核，不共享登录凭据。
 */
@Entity
@Table(name = "classroom_collaborators",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_classroom_collaborator_teacher",
                columnNames = {"classroom_id", "teacher_id"}),
        indexes = @Index(name = "idx_collaborators_teacher",
                columnList = "teacher_id"))
public class ClassroomCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "invited_by_teacher_id", nullable = false)
    private Long invitedByTeacherId;

    @Column(nullable = false)
    private Instant createdAt;

    protected ClassroomCollaborator() {
        // JPA 需要的无参构造方法
    }

    public ClassroomCollaborator(Long classroomId, Long teacherId,
                                 Long invitedByTeacherId) {
        this.classroomId = classroomId;
        this.teacherId = teacherId;
        this.invitedByTeacherId = invitedByTeacherId;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public Long getInvitedByTeacherId() {
        return invitedByTeacherId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
