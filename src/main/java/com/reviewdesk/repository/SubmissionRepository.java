package com.reviewdesk.repository;

import com.reviewdesk.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
}

