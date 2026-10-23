package com.reviewdesk.service;

import com.reviewdesk.domain.ReviewIssue;
import com.reviewdesk.repository.ReviewIssueRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 保存教师确认的问题并汇总评分量规中的共性问题。
 */
@Service
@Transactional(readOnly = true)
public class ReviewIssueService {

    private final ReviewIssueRepository issueRepository;
    private final ReviewService reviewService;
    private final ReviewActivityService activityService;

    public ReviewIssueService(ReviewIssueRepository issueRepository,
                              ReviewService reviewService,
                              ReviewActivityService activityService) {
        this.issueRepository = issueRepository;
        this.reviewService = reviewService;
        this.activityService = activityService;
    }

    @Transactional
    public ReviewIssue create(long submissionId, String rubricName, String evidenceQuote,
                              String teacherNote) {
        reviewService.requireDraft(submissionId);
        if (rubricName == null || rubricName.isBlank()
                || evidenceQuote == null || evidenceQuote.isBlank()
                || teacherNote == null || teacherNote.isBlank()) {
            throw new IllegalArgumentException("问题标记信息不完整");
        }
        ReviewIssue issue = issueRepository.save(new ReviewIssue(
                submissionId,
                rubricName.strip(),
                evidenceQuote.strip(),
                teacherNote.strip()));
        activityService.record(submissionId, "SUGGESTION_ADOPTED", rubricName.strip());
        return issue;
    }

    public List<IssueSummary> summarizeAll() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReviewIssue issue : issueRepository.findAllByOrderByCreatedAtDesc()) {
            counts.merge(issue.getRubricName(), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .map(entry -> new IssueSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(IssueSummary::count).reversed())
                .toList();
    }

    public record IssueSummary(String rubricName, long count) {
    }
}

