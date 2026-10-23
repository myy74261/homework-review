package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 作业评分量规中的一个维度。
 */
@Entity
@Table(name = "assignment_rubrics", indexes = {
        @Index(name = "idx_assignment_rubrics_assignment",
                columnList = "assignment_id, display_order")
})
public class AssignmentRubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long assignmentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false, length = 400)
    private String description;

    @Column(nullable = false)
    private int displayOrder;

    protected AssignmentRubric() {
        // JPA 需要的无参构造方法
    }

    public AssignmentRubric(Long assignmentId, String name, int points,
                            String description, int displayOrder) {
        this.assignmentId = assignmentId;
        this.name = name;
        this.points = points;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public String getDescription() {
        return description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}

