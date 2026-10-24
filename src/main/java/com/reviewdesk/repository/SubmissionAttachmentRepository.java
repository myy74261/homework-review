package com.reviewdesk.repository;

import com.reviewdesk.domain.SubmissionAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 学生提交附件的数据访问接口。
 */
public interface SubmissionAttachmentRepository
        extends JpaRepository<SubmissionAttachment, Long> {

    List<SubmissionAttachment> findBySubmissionIdOrderByIdAsc(Long submissionId);

    Optional<SubmissionAttachment> findByIdAndSubmissionId(
            Long id, Long submissionId);
}

