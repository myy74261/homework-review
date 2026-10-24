package com.reviewdesk.service;

import com.reviewdesk.domain.ReviewEvent;
import com.reviewdesk.repository.ReviewEventRepository;
import com.reviewdesk.repository.SubmissionRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 记录教师审核过程中的关键动作。
 */
@Service
@Transactional(readOnly = true)
public class ReviewActivityService {

    private static final Set<String> ALLOWED_EVENT_TYPES = Set.of(
            "SUBMISSION_VIEWED",
            "SUBMISSION_CREATED",
            "FEEDBACK_SAVED",
            "VERSION_CREATED",
            "VERSION_RESTORED",
            "SUGGESTION_ADOPTED",
            "SUGGESTION_DISMISSED",
            "REVIEW_COMPLETED",
            "INTEGRITY_FLAGGED",
            "APPEAL_SUBMITTED",
            "APPEAL_RESOLVED");

    private final ReviewEventRepository eventRepository;
    private final SubmissionRepository submissionRepository;

    public ReviewActivityService(ReviewEventRepository eventRepository,
                                 SubmissionRepository submissionRepository) {
        this.eventRepository = eventRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public void record(long submissionId, String eventType, String detail) {
        requireSubmission(submissionId);
        if (!ALLOWED_EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("不支持的审核事件");
        }
        String safeDetail = detail == null ? "" : detail.strip();
        if (safeDetail.length() > 240) {
            safeDetail = safeDetail.substring(0, 240);
        }
        eventRepository.save(new ReviewEvent(submissionId, eventType, safeDetail));
    }

    private void requireSubmission(long submissionId) {
        if (!submissionRepository.existsById(submissionId)) {
            throw new SubmissionNotFoundException(submissionId);
        }
    }
}

