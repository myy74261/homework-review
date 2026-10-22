package com.reviewdesk.repository;

import com.reviewdesk.domain.ReviewDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDraftRepository extends JpaRepository<ReviewDraft, Long> {
}

