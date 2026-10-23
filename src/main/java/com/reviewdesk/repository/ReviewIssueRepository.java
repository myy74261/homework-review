package com.reviewdesk.repository;

import com.reviewdesk.domain.ReviewIssue;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewIssueRepository extends JpaRepository<ReviewIssue, Long> {

    List<ReviewIssue> findAllByOrderByCreatedAtDesc();

    List<ReviewIssue> findBySubmissionIdOrderByCreatedAtDesc(Long submissionId);

    List<ReviewIssue> findBySubmissionIdInOrderByCreatedAtDesc(Collection<Long> submissionIds);
}

