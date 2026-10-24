package com.reviewdesk.service;

import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.AssignmentRubric;
import com.reviewdesk.repository.AssignmentRepository;
import com.reviewdesk.repository.AssignmentRubricRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师发布作业与读取评分量规的业务入口。
 */
@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentRubricRepository rubricRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             AssignmentRubricRepository rubricRepository) {
        this.assignmentRepository = assignmentRepository;
        this.rubricRepository = rubricRepository;
    }

    public Assignment requireAssignment(long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
    }

    public List<AssignmentRubric> listRubrics(long assignmentId) {
        requireAssignment(assignmentId);
        return rubricRepository.findByAssignmentIdOrderByDisplayOrderAsc(assignmentId);
    }

    /**
     * 发布作业并保存可解释的评分量规。
     */
    @Transactional
    public Assignment publish(Long classroomId, String title, String courseName,
                              String instructions, Instant dueAt,
                              List<RubricDefinition> rubrics) {
        return publish(classroomId, title, courseName, instructions, dueAt,
                "DOCUMENT", "ASSISTED_ALLOWED", true, true, true, rubrics);
    }

    /**
     * 发布作业并保存作业形式、AI 政策和诚信声明要求。
     */
    @Transactional
    public Assignment publish(Long classroomId, String title, String courseName,
                              String instructions, Instant dueAt,
                              String submissionFormat, String aiPolicy,
                              boolean aiDeclarationRequired,
                              boolean sourceDeclarationRequired,
                              boolean appealAllowed,
                              List<RubricDefinition> rubrics) {
        if (rubrics.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个评分维度");
        }
        int totalPoints = rubrics.stream().mapToInt(RubricDefinition::points).sum();
        if (totalPoints != 100) {
            throw new IllegalArgumentException("评分量规总分必须为 100 分");
        }
        if (rubrics.stream().anyMatch(rubric -> rubric.points() <= 0)) {
            throw new IllegalArgumentException("每个评分维度必须大于 0 分");
        }

        Assignment assignment = assignmentRepository.save(new Assignment(
                classroomId,
                title.strip(),
                courseName.strip(),
                instructions.strip(),
                dueAt,
                submissionFormat,
                aiPolicy,
                aiDeclarationRequired,
                sourceDeclarationRequired,
                appealAllowed));
        for (int index = 0; index < rubrics.size(); index++) {
            RubricDefinition rubric = rubrics.get(index);
            rubricRepository.save(new AssignmentRubric(
                    assignment.getId(),
                    rubric.name().strip(),
                    rubric.points(),
                    rubric.description().strip(),
                    index));
        }
        return assignment;
    }

    public record RubricDefinition(String name, int points, String description) {
    }

    public List<Assignment> listForClassrooms(List<Long> classroomIds) {
        return classroomIds.isEmpty()
                ? List.of()
                : assignmentRepository.findByClassroomIdInOrderByPublishedAtDesc(classroomIds);
    }
}

