package com.reviewdesk.repository;

import com.reviewdesk.domain.ReviewDraft;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 当前审核意见的数据访问接口。
 */
public interface ReviewDraftRepository extends JpaRepository<ReviewDraft, Long> {
}

