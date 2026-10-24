package com.reviewdesk.repository;

import com.reviewdesk.domain.Appeal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 学生申诉与教师回复的数据访问接口。
 */
public interface AppealRepository extends JpaRepository<Appeal, Long> {

    List<Appeal> findBySubmissionIdOrderByCreatedAtDesc(Long submissionId);

    List<Appeal> findBySubmissionIdInOrderByCreatedAtDesc(Collection<Long> submissionIds);

    boolean existsBySubmissionIdAndStatusIn(Long submissionId, Collection<String> statuses);
}
