package com.reviewdesk.repository;

import com.reviewdesk.domain.Submission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 学生作业提交的数据访问接口。
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentIdInOrderBySubmittedAtDesc(
            List<Long> assignmentIds);

    long countByAssignmentId(Long assignmentId);

    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);

    Optional<Submission> findFirstByAssignmentIdAndStudentAccountIdOrderByAttemptNumberDesc(
            Long assignmentId, Long studentAccountId);

    Optional<Submission> findTopByOrderByIdDesc();
}

