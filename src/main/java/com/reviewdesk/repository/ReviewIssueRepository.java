package com.reviewdesk.repository;

import com.reviewdesk.domain.ReviewIssue;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 教师确认问题的数据访问接口。
 */
public interface ReviewIssueRepository extends JpaRepository<ReviewIssue, Long> {

    List<ReviewIssue> findBySubmissionIdInOrderByCreatedAtDesc(Collection<Long> submissionIds);

    long countBySubmissionIdAndRubricName(Long submissionId, String rubricName);
}

