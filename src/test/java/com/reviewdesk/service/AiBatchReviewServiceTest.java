package com.reviewdesk.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reviewdesk.repository.AiBatchReviewJobRepository;
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
}
