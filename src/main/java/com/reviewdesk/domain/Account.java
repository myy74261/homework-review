package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 平台账号。教师与学生共用账号表，通过角色区分工作区。
 */
@Entity
@Table(name = "accounts")
public class Account {

    public enum Role {
        TEACHER,
        STUDENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(length = 40)
    private String studentNumber;

    @Column(length = 120)
    private String email;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String professionalTitle;

    @Column(nullable = false)
    private Instant createdAt;

    protected Account() {
        // JPA 需要的无参构造方法
    }

    public Account(String username, String passwordHash, String displayName,
                   Role role, String studentNumber) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role.name();
        this.studentNumber = studentNumber;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public String getProfessionalTitle() {
        return professionalTitle;
    }

    public boolean isTeacher() {
        return Role.TEACHER.name().equals(role);
    }

    /**
     * 更新密码哈希。明文密码不得进入实体。
     */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateProfile(String displayName, String email,
                              String department, String professionalTitle) {
        this.displayName = displayName;
        this.email = email;
        this.department = department;
        this.professionalTitle = professionalTitle;
    }
}

