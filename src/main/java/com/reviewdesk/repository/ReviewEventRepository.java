package com.reviewdesk.repository;

import com.reviewdesk.domain.ReviewEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 审核过程事件的数据访问接口。
 */
public interface ReviewEventRepository extends JpaRepository<ReviewEvent, Long> {

    long countBySubmissionIdAndEventType(Long submissionId, String eventType);
}
