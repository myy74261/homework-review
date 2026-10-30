package com.reviewdesk.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reviewdesk.repository.AiBatchReviewJobRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证截止作业自动生成幂等的 AI 预审任务。
 */
@SpringBootTest
@Transactional
class AiBatchReviewServiceTest {

    @Autowired
    private AiBatchReviewService batchService;

    @Autowired
    private AiSettingsService settingsService;

    @Autowired
    private AiBatchReviewJobRepository jobRepository;

    @Autowired
    private AssignmentService assignmentService;

    @Test
    void dueSubmissionCreatesOneAutomaticAiReviewJob() {
        assertThat(batchService.enqueueDueSubmissions()).isZero();

        settingsService.save(
                "chen", "DEEPSEEK", "deepseek-chat", "test-key-1234");
        assertThat(batchService.enqueueDueSubmissions()).isEqualTo(1);
        assertThat(jobRepository.findBySubmissionId(1L)).isPresent();
        assertThat(jobRepository.findBySubmissionId(1L).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(batchService.progress(java.util.List.of(1L)).get(0).pending())
                .isEqualTo(1);

        assertThat(batchService.enqueueDueSubmissions()).isZero();
    }

    @Test
    void extendingDeadlinePostponesPendingAiJobs() {
        settingsService.save(
                "chen", "DEEPSEEK", "deepseek-chat", "test-key-1234");
        assertThat(batchService.enqueueDueSubmissions()).isEqualTo(1);

        Instant extendedDeadline = Instant.now().plus(2, ChronoUnit.DAYS);
        var assignment = assignmentService.updateDueAt(1L, extendedDeadline);
        batchService.alignPendingJobsWithDeadline(assignment);

        var job = jobRepository.findBySubmissionId(1L).orElseThrow();
        assertThat(job.getStatus()).isEqualTo("PENDING");
        assertThat(job.getNextAttemptAt()).isEqualTo(extendedDeadline);
        assertThat(batchService.claimNext()).isEmpty();
    }

    @Test
    void extendingDeadlineKeepsCompletedAiRunHistory() {
        settingsService.save(
                "chen", "DEEPSEEK", "deepseek-chat", "test-key-1234");
        assertThat(batchService.enqueueDueSubmissions()).isEqualTo(1);

        var job = jobRepository.findBySubmissionId(1L).orElseThrow();
        Instant completedAt = job.getNextAttemptAt();
        job.markRunning();
        job.complete();
        jobRepository.saveAndFlush(job);

        Instant extendedDeadline = Instant.now().plus(3, ChronoUnit.DAYS);
        var assignment = assignmentService.updateDueAt(1L, extendedDeadline);
        batchService.alignPendingJobsWithDeadline(assignment);

        var preservedJob = jobRepository.findBySubmissionId(1L).orElseThrow();
        assertThat(preservedJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(preservedJob.getAttemptCount()).isEqualTo(1);
        assertThat(preservedJob.getNextAttemptAt()).isEqualTo(completedAt);
    }

    @Test
    void claimedJobIsRequeuedWhenDeadlineIsExtendedBeforeModelCall() {
        settingsService.save(
                "chen", "DEEPSEEK", "deepseek-chat", "test-key-1234");
        assertThat(batchService.enqueueDueSubmissions()).isEqualTo(1);
        var claimed = batchService.claimNext().orElseThrow();
        assertThat(claimed.getStatus()).isEqualTo("RUNNING");

        Instant extendedDeadline = Instant.now().plus(4, ChronoUnit.DAYS);
        var assignment = assignmentService.updateDueAt(1L, extendedDeadline);
        batchService.alignPendingJobsWithDeadline(assignment);
        batchService.process(claimed.getId());

        var requeued = jobRepository.findBySubmissionId(1L).orElseThrow();
        assertThat(requeued.getStatus()).isEqualTo("PENDING");
        assertThat(requeued.getAttemptCount()).isZero();
        assertThat(requeued.getNextAttemptAt()).isEqualTo(extendedDeadline);
        assertThat(batchService.claimNext()).isEmpty();
    }
}
