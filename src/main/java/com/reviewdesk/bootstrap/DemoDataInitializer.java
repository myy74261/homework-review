package com.reviewdesk.bootstrap;

import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.ReviewDraftRepository;
import com.reviewdesk.repository.SubmissionRepository;
import com.reviewdesk.service.ReviewService;
import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首次启动时写入一份完整的教师审核演示数据。
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final String SUBMISSION_CONTENT = """
            摘要

            本研究考察短视频使用强度与大学生注意力恢复之间的关系。通过对在校本科生进行问卷调查，研究发现二者存在显著相关。

            二、研究方法

            本研究采用问卷调查法，以某高校在校本科生为研究对象。问卷通过班级群发放，最终获得有效样本 326 份。

            三、结果与分析

            相关分析结果显示，短视频使用强度与注意力恢复水平之间存在显著负相关。
            """;

    private static final String INITIAL_FEEDBACK = """
            总体判断

            本次提交已经形成清晰的研究问题，并补充了样本来源与调查流程，整体结构完整。修改后可以进入下一轮定稿。

            需要优先补充的地方

            1. 在“结果与分析”中补充样本量、相关系数和显著性水平，避免只给出概括性结论。
            2. 将“已有研究表明”对应的文献补入参考文献，并在正文中标注来源。
            3. “讨论”部分需要区分数据能够支持的结论与推测，减少过度概括。

            修改建议

            请优先处理第 1、2 项，并在回复区说明修改位置。完成后再提交终稿。
            """;

    private final SubmissionRepository submissionRepository;
    private final ReviewDraftRepository draftRepository;
    private final ReviewService reviewService;

    public DemoDataInitializer(SubmissionRepository submissionRepository,
                               ReviewDraftRepository draftRepository,
                               ReviewService reviewService) {
        this.submissionRepository = submissionRepository;
        this.draftRepository = draftRepository;
        this.reviewService = reviewService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (submissionRepository.existsById(ReviewService.DEMO_SUBMISSION_ID)) {
            return;
        }

        Submission submission = new Submission(
                ReviewService.DEMO_SUBMISSION_ID,
                "短视频使用与大学生注意力恢复的关系研究",
                "传播学方法课",
                "林清和",
                "20230116",
                SUBMISSION_CONTENT,
                2846,
                2,
                "问卷与编码表.pdf",
                Instant.parse("2026-10-24T15:59:00Z"),
                Instant.parse("2026-10-21T14:48:00Z"));
        submissionRepository.save(submission);
        draftRepository.save(new ReviewDraft(
                submission.getId(),
                "课程论文第二次提交审核意见",
                INITIAL_FEEDBACK));
        reviewService.createVersion(submission.getId(), "INITIAL");
    }
}
