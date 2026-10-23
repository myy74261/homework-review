package com.reviewdesk.repository;

import com.reviewdesk.domain.Submission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findAllByOrderBySubmittedAtDesc();

    List<Submission> findByStudentNumberOrderBySubmittedAtDesc(String studentNumber);

    long countByAssignmentId(Long assignmentId);

    Optional<Submission> findFirstByAssignmentIdAndStudentNumberOrderByAttemptNumberDesc(
            Long assignmentId, String studentNumber);

    Optional<Submission> findTopByOrderByIdDesc();
}

