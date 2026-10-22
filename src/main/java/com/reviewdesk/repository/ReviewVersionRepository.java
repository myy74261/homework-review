package com.reviewdesk.repository;

import com.reviewdesk.domain.ReviewVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewVersionRepository extends JpaRepository<ReviewVersion, Long> {

    List<ReviewVersion> findBySubmissionIdOrderByVersionNumberDesc(Long submissionId);

    Optional<ReviewVersion> findByIdAndSubmissionId(Long id, Long submissionId);

    Optional<ReviewVersion> findFirstBySubmissionIdOrderByVersionNumberDesc(Long submissionId);

    long countBySubmissionId(Long submissionId);
}
