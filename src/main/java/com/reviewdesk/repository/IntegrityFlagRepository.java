package com.reviewdesk.repository;

import com.reviewdesk.domain.IntegrityFlag;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 学术诚信问题标记的数据访问接口。
 */
public interface IntegrityFlagRepository extends JpaRepository<IntegrityFlag, Long> {

    List<IntegrityFlag> findBySubmissionIdOrderByCreatedAtDesc(Long submissionId);

    List<IntegrityFlag> findBySubmissionIdInOrderByCreatedAtDesc(
            Collection<Long> submissionIds);
}

