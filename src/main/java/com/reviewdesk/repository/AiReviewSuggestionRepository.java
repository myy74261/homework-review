package com.reviewdesk.repository;

import com.reviewdesk.domain.AiReviewSuggestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AI 预审建议的数据访问接口。
 */
public interface AiReviewSuggestionRepository
        extends JpaRepository<AiReviewSuggestion, Long> {

    List<AiReviewSuggestion> findByRunIdOrderByIdAsc(Long runId);

    List<AiReviewSuggestion> findBySubmissionIdAndStatus(
            Long submissionId, String status);
}
