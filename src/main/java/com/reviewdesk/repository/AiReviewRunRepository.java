package com.reviewdesk.repository;

import com.reviewdesk.domain.AiReviewRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AI 预审运行记录的数据访问接口。
 */
public interface AiReviewRunRepository extends JpaRepository<AiReviewRun, Long> {

    List<AiReviewRun> findBySubmissionIdOrderByCreatedAtDesc(Long submissionId);
}
