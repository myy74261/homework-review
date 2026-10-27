package com.reviewdesk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时扫描截止作业，自动启动 AI 预审。最终成绩与反馈仍需教师确认。
 */
@Component
public class AiBatchReviewScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AiBatchReviewScheduler.class);

    private final AiBatchReviewService batchService;
    private final int batchSize;

    public AiBatchReviewScheduler(
            AiBatchReviewService batchService,
            @Value("${review-desk.ai.batch-size:3}") int batchSize) {
        this.batchService = batchService;
        this.batchSize = Math.max(1, Math.min(10, batchSize));
    }

    @Scheduled(
            initialDelayString = "${review-desk.ai.batch-initial-delay-ms:15000}",
            fixedDelayString = "${review-desk.ai.batch-delay-ms:60000}")
    public void run() {
        int created = batchService.enqueueDueSubmissions();
        if (created > 0) {
            LOGGER.info("自动创建 {} 个 AI 预审任务", created);
        }
        for (int index = 0; index < batchSize; index++) {
            var job = batchService.claimNext();
            if (job.isEmpty()) {
                break;
            }
            batchService.process(job.get().getId());
        }
    }
}
