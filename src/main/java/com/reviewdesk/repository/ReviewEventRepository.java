package com.reviewdesk.repository;

import com.reviewdesk.domain.ReviewEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewEventRepository extends JpaRepository<ReviewEvent, Long> {

    List<ReviewEvent> findBySubmissionIdOrderByOccurredAtDesc(Long submissionId);

    long countBySubmissionIdAndEventType(Long submissionId, String eventType);
}
