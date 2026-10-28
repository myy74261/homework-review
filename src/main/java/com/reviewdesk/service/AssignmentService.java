package com.reviewdesk.service;

import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.AssignmentRubric;
import com.reviewdesk.repository.AssignmentRepository;
import com.reviewdesk.repository.AssignmentRubricRepository;
import com.reviewdesk.repository.SubmissionRepository;
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
    private final SubmissionRepository submissionRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             AssignmentRubricRepository rubricRepository,
                             SubmissionRepository submissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.rubricRepository = rubricRepository;
        this.submissionRepository = submissionRepository;
    }

    public Assignment requireAssignment(long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
    }

    public Assignment requirePublishedAssignment(long assignmentId) {
        Assignment assignment = requireAssignment(assignmentId);
        if (!"PUBLISHED".equals(assignment.getStatus())) {
            throw new AssignmentNotFoundException(assignmentId);
        }
        return assignment;
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
        validateRubrics(rubrics);

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
        replaceRubrics(assignment.getId(), rubrics);
        return assignment;
    }

    /**
     * 撤回已发布作业，使其暂时从学生端隐藏并允许教师继续编辑。
     */
    @Transactional
    public Assignment withdraw(long assignmentId) {
        Assignment assignment = requireAssignment(assignmentId);
        if (!"PUBLISHED".equals(assignment.getStatus())) {
            throw new IllegalStateException("只有已发布的作业可以撤回");
        }
        assignment.withdraw();
        return assignmentRepository.save(assignment);
    }

    /**
     * 仅调整已发布作业的截止时间，不改变作业内容和状态。
     */
    @Transactional
    public Assignment updateDueAt(long assignmentId, Instant dueAt) {
        Assignment assignment = requireAssignment(assignmentId);
        if (!"PUBLISHED".equals(assignment.getStatus())) {
            throw new IllegalStateException("只有已发布作业可以直接调整截止时间");
        }
        if (dueAt == null || !dueAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("截止时间必须晚于当前时间");
        }
        assignment.updateDueAt(dueAt);
        return assignmentRepository.save(assignment);
    }

    /**
     * 保存撤回作业的修改并重新发布，已有提交记录保持不变。
     */
    @Transactional
    public Assignment updateAndRepublish(
            long assignmentId,
            Long classroomId,
            String title,
            String courseName,
            String instructions,
            Instant dueAt,
            String submissionFormat,
            String aiPolicy,
            boolean aiDeclarationRequired,
            boolean sourceDeclarationRequired,
            boolean appealAllowed,
            List<RubricDefinition> rubrics) {
        Assignment assignment = requireAssignment(assignmentId);
        if (!"WITHDRAWN".equals(assignment.getStatus())) {
            throw new IllegalStateException("请先撤回作业，再修改内容");
        }
        validateRubrics(rubrics);
        boolean hasSubmissions =
                submissionRepository.countByAssignmentId(assignmentId) > 0;
        if (hasSubmissions && rubricsChanged(assignmentId, rubrics)) {
            throw new IllegalStateException("已有学生提交，评分维度不能再修改");
        }
        assignment.updateAndRepublish(
                classroomId,
                title.strip(),
                courseName.strip(),
                instructions.strip(),
                dueAt,
                submissionFormat,
                aiPolicy,
                aiDeclarationRequired,
                sourceDeclarationRequired,
                appealAllowed);
        assignmentRepository.save(assignment);

        if (!hasSubmissions) {
            rubricRepository.deleteByAssignmentId(assignmentId);
            replaceRubrics(assignmentId, rubrics);
        }
        return assignment;
    }

    private boolean rubricsChanged(long assignmentId,
                                   List<RubricDefinition> rubrics) {
        List<AssignmentRubric> existing =
                rubricRepository.findByAssignmentIdOrderByDisplayOrderAsc(assignmentId);
        if (existing.size() != rubrics.size()) {
            return true;
        }
        for (int index = 0; index < existing.size(); index++) {
            AssignmentRubric current = existing.get(index);
            RubricDefinition updated = rubrics.get(index);
            if (!current.getName().equals(updated.name().strip())
                    || current.getPoints() != updated.points()
                    || !current.getDescription().equals(updated.description().strip())) {
                return true;
            }
        }
        return false;
    }

    private void validateRubrics(List<RubricDefinition> rubrics) {
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
    }

    private void replaceRubrics(long assignmentId, List<RubricDefinition> rubrics) {
        for (int index = 0; index < rubrics.size(); index++) {
            RubricDefinition rubric = rubrics.get(index);
            rubricRepository.save(new AssignmentRubric(
                    assignmentId,
                    rubric.name().strip(),
                    rubric.points(),
                    rubric.description().strip(),
                    index));
        }
    }

    public record RubricDefinition(String name, int points, String description) {
    }

    public List<Assignment> listForClassrooms(List<Long> classroomIds) {
        return classroomIds.isEmpty()
                ? List.of()
                : assignmentRepository.findByClassroomIdInAndStatusOrderByPublishedAtDesc(
                        classroomIds, "PUBLISHED");
    }
}

