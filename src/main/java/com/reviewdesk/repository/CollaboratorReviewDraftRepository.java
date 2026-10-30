package com.reviewdesk.repository;

import com.reviewdesk.domain.CollaboratorReviewDraft;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 协作者独立审核草稿的数据访问接口。
 */
public interface CollaboratorReviewDraftRepository
        extends JpaRepository<CollaboratorReviewDraft, Long> {

    Optional<CollaboratorReviewDraft> findBySubmissionIdAndReviewerAccountId(
            Long submissionId, Long reviewerAccountId);

    List<CollaboratorReviewDraft> findBySubmissionIdOrderByUpdatedAtDesc(
            Long submissionId);
}
