package com.reviewdesk.repository;

import com.reviewdesk.domain.ReviewScore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 审核评分建议与教师最终分的数据访问接口。
 */
public interface ReviewScoreRepository extends JpaRepository<ReviewScore, Long> {

    List<ReviewScore> findBySubmissionIdOrderByIdAsc(Long submissionId);

    Optional<ReviewScore> findBySubmissionIdAndRubricId(
            Long submissionId, Long rubricId);
}

