package com.reviewdesk.bootstrap;

import com.reviewdesk.config.DemoAccountProperties;
import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.ReviewIssue;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.AccountRepository;
import com.reviewdesk.repository.ReviewDraftRepository;
import com.reviewdesk.repository.ReviewIssueRepository;
import com.reviewdesk.repository.SubmissionRepository;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ClassroomService;
import com.reviewdesk.service.ReviewService;
import java.time.Instant;
import java.util.List;
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
    private final AssignmentService assignmentService;
    private final ReviewIssueRepository issueRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final ClassroomService classroomService;
    private final DemoAccountProperties demoAccounts;

    public DemoDataInitializer(SubmissionRepository submissionRepository,
                               ReviewDraftRepository draftRepository,
                               ReviewService reviewService,
                               AssignmentService assignmentService,
                               ReviewIssueRepository issueRepository,
                               AccountRepository accountRepository,
                               AccountService accountService,
                               ClassroomService classroomService,
                               DemoAccountProperties demoAccounts) {
        this.submissionRepository = submissionRepository;
        this.draftRepository = draftRepository;
        this.reviewService = reviewService;
        this.assignmentService = assignmentService;
        this.issueRepository = issueRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.classroomService = classroomService;
        this.demoAccounts = demoAccounts;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Account teacher = accountRepository.findByUsername(demoAccounts.teacherUsername())
                .orElseGet(() -> accountService.register(
                        demoAccounts.teacherUsername(),
                        demoAccounts.teacherPassword(),
                        demoAccounts.teacherName(),
                        Account.Role.TEACHER,
                        null));
        Account student = accountRepository.findByUsername(demoAccounts.studentUsername())
                .orElseGet(() -> accountService.register(
                        demoAccounts.studentUsername(),
                        demoAccounts.studentPassword(),
                        demoAccounts.studentName(),
                        Account.Role.STUDENT,
                        demoAccounts.studentNumber()));
        Classroom classroom = classroomService.teacherClassrooms(teacher).stream()
                .findFirst()
                .orElseGet(() -> classroomService.create(
                        teacher, "传播学方法课 · 2026 秋", "传播学方法课"));
        classroomService.join(student, classroom.getJoinCode());

        if (submissionRepository.existsById(ReviewService.DEMO_SUBMISSION_ID)) {
            seedIssues();
            return;
        }

        var assignment = assignmentService.listForClassrooms(
                        List.of(classroom.getId())).stream()
                .findFirst()
                .orElseGet(() -> assignmentService.publish(
                        classroom.getId(),
                        "短视频使用与大学生注意力恢复的关系研究",
                        "传播学方法课",
                        "完成一篇 3,000 字左右的调查报告，说明研究问题、研究方法、"
                                + "数据来源与主要发现，并附参考文献。",
                        Instant.parse("2026-10-24T15:59:00Z"),
                        List.of(
                                new AssignmentService.RubricDefinition(
                                        "研究问题与目标", 25, "问题、目标与研究边界是否清楚"),
                                new AssignmentService.RubricDefinition(
                                        "证据与引用", 25, "结论是否有可追溯的数据或文献支持"),
                                new AssignmentService.RubricDefinition(
                                        "方法与数据", 30, "样本、抽样、回收率与统计过程是否透明"),
                                new AssignmentService.RubricDefinition(
                                        "结构与表达", 20, "论证顺序、段落衔接与学术表达是否清晰")
                        )));
        Submission submission = new Submission(
                ReviewService.DEMO_SUBMISSION_ID,
                assignment.getId(),
                student.getId(),
                assignment.getTitle(),
                assignment.getCourseName(),
                demoAccounts.studentName(),
                demoAccounts.studentNumber(),
                SUBMISSION_CONTENT,
                2846,
                2,
                "问卷与编码表.pdf",
                assignment.getDueAt(),
                Instant.parse("2026-10-21T14:48:00Z"));
        submissionRepository.save(submission);
        draftRepository.save(new ReviewDraft(
                submission.getId(),
                "课程论文第二次提交审核意见",
                INITIAL_FEEDBACK));
        reviewService.createVersion(submission.getId(), "INITIAL");
        seedIssues();
    }

    private void seedIssues() {
        if (issueRepository.count() > 0) {
            return;
        }
        issueRepository.save(new ReviewIssue(
                ReviewService.DEMO_SUBMISSION_ID,
                "证据与引用",
                "已有研究表明，短视频使用会影响注意力恢复。",
                "请为“已有研究表明”补充具体文献来源，并在正文中标注引用。"));
        issueRepository.save(new ReviewIssue(
                ReviewService.DEMO_SUBMISSION_ID,
                "方法与数据",
                "问卷通过班级群发放，最终获得有效样本 326 份。",
                "请补充问卷回收率、有效样本筛选标准和受访者年级分布。"));
    }
}
