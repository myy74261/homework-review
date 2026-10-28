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
 * 教师为课堂导入的学生名单。学生注册或已有账号后会自动关联。
 */
@Entity
@Table(name = "classroom_roster_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roster_classroom_student_number",
                columnNames = {"classroom_id", "student_number"}),
        indexes = @Index(name = "idx_roster_student_number",
                columnList = "student_number"))
public class ClassroomRosterEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "student_number", nullable = false, length = 40)
    private String studentNumber;

    @Column(nullable = false, length = 80)
    private String studentName;

    @Column(name = "student_account_id")
    private Long studentAccountId;

    @Column(nullable = false)
    private Instant importedAt;

    protected ClassroomRosterEntry() {
        // JPA 需要的无参构造方法
    }

    public ClassroomRosterEntry(Long classroomId, String studentNumber,
                                String studentName) {
        this.classroomId = classroomId;
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.importedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public Long getStudentAccountId() {
        return studentAccountId;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void updateName(String studentName) {
        this.studentName = studentName;
    }

    public void linkAccount(Long studentAccountId) {
        this.studentAccountId = studentAccountId;
    }
}
