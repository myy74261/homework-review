package com.reviewdesk.service;

import com.reviewdesk.domain.AssignmentRubric;
import com.reviewdesk.domain.ReviewScore;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.ReviewIssueRepository;
import com.reviewdesk.repository.ReviewScoreRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 根据教师确认的问题生成透明的规则评分建议，并保存教师最终分。
 */
@Service
@Transactional(readOnly = true)
public class ReviewScoreService {

    private final ReviewService reviewService;
    private final AssignmentService assignmentService;
    private final ReviewIssueRepository issueRepository;
    private final ReviewScoreRepository scoreRepository;

    public ReviewScoreService(ReviewService reviewService,
                              AssignmentService assignmentService,
                              ReviewIssueRepository issueRepository,
                              ReviewScoreRepository scoreRepository) {
        this.reviewService = reviewService;
        this.assignmentService = assignmentService;
        this.issueRepository = issueRepository;
        this.scoreRepository = scoreRepository;
    }

    @Transactional
    public List<ReviewScore> getOrCreate(long submissionId) {
        Submission submission = reviewService.requireSubmission(submissionId);
        List<AssignmentRubric> rubrics =
                assignmentService.listRubrics(submission.getAssignmentId());
        Map<Long, ReviewScore> existing = scoreRepository
                .findBySubmissionIdOrderByIdAsc(submissionId).stream()
                .collect(Collectors.toMap(ReviewScore::getRubricId, Function.identity()));

        for (AssignmentRubric rubric : rubrics) {
            long issueCount = issueRepository.countBySubmissionIdAndRubricName(
                    submissionId, rubric.getName());
            int penalty = issueCount == 0
                    ? 0
                    : (int) Math.ceil(rubric.getPoints() * 0.15 * issueCount);
            int suggested = Math.max(0, rubric.getPoints() - penalty);
            String suggestion = issueCount == 0
                    ? "暂未确认该维度问题；建议教师核对达标证据后给分。"
                    : "已确认 " + issueCount + " 处问题。规则建议 "
                            + suggested + " 分，仅供参考，教师应结合证据调整。";
            ReviewScore score = existing.get(rubric.getId());
            if (score == null) {
                score = new ReviewScore(
                        submissionId,
                        rubric.getId(),
                        rubric.getName(),
                        rubric.getPoints(),
                        suggested,
                        suggestion);
            } else {
                score.updateSuggestion(suggested, suggestion);
            }
            existing.put(rubric.getId(), scoreRepository.save(score));
        }
        return rubrics.stream()
                .map(rubric -> existing.get(rubric.getId()))
                .toList();
    }

    public List<ReviewScore> list(long submissionId) {
        reviewService.requireSubmission(submissionId);
        return scoreRepository.findBySubmissionIdOrderByIdAsc(submissionId);
    }

    @Transactional
    public List<ReviewScore> saveFinalScores(long submissionId,
                                             Map<Long, Integer> finalScores) {
        List<ReviewScore> scores = getOrCreate(submissionId);
        for (ReviewScore score : scores) {
            Integer points = finalScores.get(score.getId());
            if (points == null) {
                continue;
            }
            if (points < 0 || points > score.getMaxPoints()) {
                throw new IllegalArgumentException(
                        score.getRubricName() + " 的分数必须在 0–"
                                + score.getMaxPoints() + " 之间");
            }
            score.setFinalPoints(points);
            scoreRepository.save(score);
        }
        return scores;
    }

    public Integer finalTotal(long submissionId) {
        List<ReviewScore> scores = list(submissionId);
        if (scores.isEmpty() || scores.stream().anyMatch(score -> score.getFinalPoints() == null)) {
            return null;
        }
        return scores.stream().mapToInt(ReviewScore::getFinalPoints).sum();
    }
}
