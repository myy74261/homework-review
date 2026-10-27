package com.reviewdesk.repository;

import com.reviewdesk.domain.AiBatchReviewJob;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 截止后自动 AI 预审任务的数据访问接口。
 */
public interface AiBatchReviewJobRepository
        extends JpaRepository<AiBatchReviewJob, Long> {

    boolean existsBySubmissionId(Long submissionId);

    Optional<AiBatchReviewJob> findBySubmissionId(Long submissionId);

    Optional<AiBatchReviewJob> findFirstByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
            String status, Instant now);

    List<AiBatchReviewJob> findByAssignmentIdIn(Collection<Long> assignmentIds);
}
