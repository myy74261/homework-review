package com.reviewdesk.bootstrap;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.ReviewIssue;
import com.reviewdesk.domain.ReviewScore;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.AccountRepository;
import com.reviewdesk.repository.ReviewIssueRepository;
import com.reviewdesk.service.AppealService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ClassroomService;
import com.reviewdesk.service.IntegrityService;
import com.reviewdesk.service.ReviewScoreService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.StudentSubmissionService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 为文科、理科和工科补充可操作的演示数据。
 */
@Component
@Order(2)
@ConditionalOnProperty(
        name = "review-desk.demo.cross-discipline-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class CrossDisciplineDataInitializer implements ApplicationRunner {

    private static final String TEACHER_PASSWORD = "Teach2026!";
    private static final String STUDENT_PASSWORD = "Student2026!";

    private final AccountRepository accountRepository;
    private final com.reviewdesk.service.AccountService accountService;
    private final ClassroomService classroomService;
    private final AssignmentService assignmentService;
    private final StudentSubmissionService submissionService;
    private final ReviewService reviewService;
    private final ReviewIssueRepository issueRepository;
    private final ReviewScoreService scoreService;
    private final IntegrityService integrityService;
    private final AppealService appealService;

    public CrossDisciplineDataInitializer(
            AccountRepository accountRepository,
            com.reviewdesk.service.AccountService accountService,
            ClassroomService classroomService,
            AssignmentService assignmentService,
            StudentSubmissionService submissionService,
            ReviewService reviewService,
            ReviewIssueRepository issueRepository,
            ReviewScoreService scoreService,
            IntegrityService integrityService,
            AppealService appealService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.classroomService = classroomService;
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.reviewService = reviewService;
        this.issueRepository = issueRepository;
        this.scoreService = scoreService;
        this.integrityService = integrityService;
        this.appealService = appealService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Account humanitiesTeacher = teacher(
                "chen", "陈老师", "新闻传播学院", "副教授");
        Account scienceTeacher = teacher(
                "zhou", "周老师", "物理学院", "讲师");
        Account engineeringTeacher = teacher(
                "wujian", "吴老师", "计算机学院", "副教授");

        Classroom humanities = classroom(
                humanitiesTeacher,
                "传播学方法课 · 2026 秋",
                "传播学方法课",
                Instant.now().minus(Duration.ofDays(90)),
                Instant.now().plus(Duration.ofDays(30)));
        Classroom science = classroom(
                scienceTeacher,
                "普通物理实验 · 2026 秋",
                "普通物理实验",
                Instant.now().minus(Duration.ofDays(60)),
                Instant.now().plus(Duration.ofDays(45)));
        Classroom engineering = classroom(
                engineeringTeacher,
                "软件工程实践 · 2026 秋",
                "软件工程实践",
                Instant.now().minus(Duration.ofDays(75)),
                Instant.now().plus(Duration.ofDays(35)));

        List<Account> humanitiesStudents = students(
                humanities,
                List.of(
                        student("humanities_01", "赵明", "20230117"),
                        student("humanities_02", "孙悦", "20230118"),
                        student("humanities_03", "周宁", "20230119"),
                        student("humanities_04", "许文", "20230120"),
                        student("humanities_05", "郑晓", "20230121")));
        List<Account> scienceStudents = students(
                science,
                List.of(
                        student("science_01", "李然", "20230201"),
                        student("science_02", "王珂", "20230202"),
                        student("science_03", "陈曦", "20230203"),
                        student("science_04", "唐宇", "20230204"),
                        student("science_05", "韩梅", "20230205")));
        List<Account> engineeringStudents = students(
                engineering,
                List.of(
                        student("engineering_01", "张航", "20230301"),
                        student("engineering_02", "刘洋", "20230302"),
                        student("engineering_03", "何雨", "20230303"),
                        student("engineering_04", "顾辰", "20230304"),
                        student("engineering_05", "罗嘉", "20230305")));

        Assignment interview = assignment(
                humanities,
                "社交媒体访谈资料编码与结论表达",
                "选取 6 份公开访谈材料，完成主题编码、证据摘录和 1,500 字分析，说明样本边界与结论限制。",
                Instant.now().minus(Duration.ofDays(3)),
                "DOCUMENT",
                rubrics(
                        rubric("研究问题", 20, "问题、对象和分析边界是否明确"),
                        rubric("编码与证据", 35, "主题编码是否可追溯，原文证据是否充分"),
                        rubric("分析与讨论", 30, "结论是否由材料支持，是否说明限制"),
                        rubric("学术表达", 15, "结构、引用和语言是否清楚")));
        seedReviewedAndPending(
                humanitiesTeacher,
                interview,
                humanitiesStudents,
                "访谈材料主题编码已提交，包含编码表、代表性引文和分析结论。",
                "本次分析已形成主题编码和代表性证据。建议补充样本筛选过程，并区分材料能够支持的结论与推测。",
                "编码与证据",
                "访谈摘要提到“受访者普遍焦虑”，但正文没有给出对应样本数量。",
                "请补充该判断对应的访谈编号与出现频次。",
                true,
                true);

        Assignment literature = assignment(
                humanities,
                "文献综述：平台劳动与数字身份",
                "围绕平台劳动与数字身份完成 2,000 字综述，至少引用 8 篇文献并制作观点对照表。",
                Instant.now().plus(Duration.ofDays(9)),
                "MIXED",
                rubrics(
                        rubric("问题意识", 20, "综述问题是否聚焦"),
                        rubric("文献覆盖", 30, "文献数量和代表性是否充分"),
                        rubric("综合能力", 35, "是否形成比较而非逐篇罗列"),
                        rubric("引用规范", 15, "引用和参考文献是否一致")));
        seedPending(
                literature,
                humanitiesStudents.subList(0, 3),
                "正在整理平台劳动与数字身份的文献观点对照表，并补充代表性案例与引用来源。");

        Assignment pendulum = assignment(
                science,
                "单摆周期测量实验报告",
                "完成单摆周期测量，记录仪器误差、原始数据、不确定度处理和实验结论。",
                Instant.now().minus(Duration.ofDays(5)),
                "DOCUMENT",
                rubrics(
                        rubric("实验设计", 20, "变量控制与测量步骤是否合理"),
                        rubric("数据记录", 25, "原始数据、单位和有效数字是否规范"),
                        rubric("误差分析", 35, "不确定度计算和误差来源是否完整"),
                        rubric("结论表达", 20, "结论是否与数据和误差范围一致")));
        seedReviewedAndPending(
                scienceTeacher,
                pendulum,
                scienceStudents,
                "单摆长度分别设置为 0.5 至 1.2 米，每组测量 20 个周期并取平均值。",
                "实验步骤和数据记录完整。误差分析仍需区分 A 类与 B 类不确定度，并补写仪器分辨力的影响。",
                "误差分析",
                "只给出了标准差，没有说明秒表分辨力和摆长测量误差如何合成。",
                "请补充不确定度传播公式、各来源分量和合成结果。",
                true,
                true);

        Assignment errorAnalysis = assignment(
                science,
                "误差传播与数据处理练习",
                "根据给定数据计算均值、标准差和合成不确定度，提交计算过程与结果解释。",
                Instant.now().plus(Duration.ofDays(7)),
                "MIXED",
                rubrics(
                        rubric("公式选择", 25, "误差传播公式是否与测量模型匹配"),
                        rubric("计算过程", 35, "单位、有效数字和计算步骤是否正确"),
                        rubric("结果解释", 25, "是否解释误差范围和实验可信度"),
                        rubric("规范表达", 15, "表格和计算过程是否便于复核")));
        seedPending(
                errorAnalysis,
                scienceStudents.subList(0, 3),
                "已完成误差传播、平均值和标准差计算，正在补充各误差来源与结果可信度解释。");

        Assignment apiProject = assignment(
                engineering,
                "校园服务 API 设计与实现",
                "设计并实现一个校园服务 REST API，提交源代码、接口说明、测试用例和运行截图。",
                Instant.now().minus(Duration.ofDays(4)),
                "CODE",
                rubrics(
                        rubric("接口设计", 25, "资源建模、状态码和错误结构是否合理"),
                        rubric("实现质量", 30, "代码结构、边界条件和安全性是否可靠"),
                        rubric("测试覆盖", 30, "关键路径和异常路径是否测试"),
                        rubric("文档与演示", 15, "接口文档和运行证据是否完整")));
        seedReviewedAndPending(
                engineeringTeacher,
                apiProject,
                engineeringStudents,
                "项目使用 Spring Boot 实现课程、教室和预约接口，附 Postman 测试集与运行截图。",
                "接口结构和错误返回整体清楚。当前缺少并发预约冲突测试，部分异常分支没有验证。",
                "测试覆盖",
                "测试集只覆盖成功创建预约，没有覆盖重复时间和资源不存在的情况。",
                "请补充冲突预约、资源不存在和未授权访问测试。",
                false,
                true);

        Assignment refactor = assignment(
                engineering,
                "代码评审与单元测试改进",
                "选择一段已有代码完成静态评审，重构重复逻辑并补充单元测试，说明风险变化。",
                Instant.now().plus(Duration.ofDays(12)),
                "CODE",
                rubrics(
                        rubric("问题定位", 25, "是否识别真实结构和可靠性问题"),
                        rubric("重构质量", 30, "改动是否降低复杂度且保持行为"),
                        rubric("测试设计", 30, "正常与异常路径覆盖是否充分"),
                        rubric("评审表达", 15, "证据、风险和建议是否清楚")));
        seedPending(
                refactor,
                engineeringStudents.subList(0, 3),
                "已定位重复校验逻辑和异常处理问题，正在补充边界条件、空值和并发场景测试。");
    }

    private void seedReviewedAndPending(
            Account teacher,
            Assignment assignment,
            List<Account> students,
            String submissionContent,
            String feedback,
            String issueRubric,
            String evidence,
            String teacherNote,
            boolean seedIntegrity,
            boolean seedAppeal) {
        List<Submission> submissions = students.stream()
                .map(student -> submit(assignment, student, submissionContent))
                .toList();
        for (int index = 0; index < Math.min(3, submissions.size()); index++) {
            Submission submission = submissions.get(index);
            completeReview(teacher, submission, feedback);
            if (index == 0 && issueRepository.countBySubmissionIdAndRubricName(
                    submission.getId(), issueRubric) == 0) {
                issueRepository.save(new ReviewIssue(
                        submission.getId(), issueRubric, evidence, teacherNote));
            }
            saveScores(submission);
            if (index == 0 && seedIntegrity
                    && integrityService.list(submission.getId()).isEmpty()) {
                var flag = integrityService.create(
                        teacher.getUsername(),
                        submission.getId(),
                        "来源与协作",
                        "MEDIUM",
                        evidence,
                        "需核对原始材料与提交说明是否一致。");
                integrityService.resolve(teacher.getUsername(), flag.getId(), "CONFIRMED");
            }
            if (index == 0 && seedAppeal
                    && appealService.listForSubmission(submission.getId()).isEmpty()) {
                var appeal = appealService.submit(
                        submission.getId(),
                        students.get(index).getId(),
                        "我认为结论已经说明样本限制，希望教师复核评分依据。",
                        "请复核“分析与讨论”维度的扣分。");
                appealService.respond(
                        teacher.getUsername(),
                        appeal.getId(),
                        "UNDER_REVIEW",
                        "已收到申诉，正在对照原文证据重新复核。");
            }
        }
    }

    private void seedPending(Assignment assignment, List<Account> students, String content) {
        students.forEach(student -> submit(assignment, student, content));
    }

    private Submission submit(Assignment assignment, Account student, String content) {
        return submissionService.findLatest(assignment.getId(), student)
                .orElseGet(() -> {
                    try {
                        return submissionService.submit(
                                assignment.getId(),
                                student,
                                content,
                                "ASSISTED",
                                "文本编辑与语法检查工具",
                                "用于检查段落表达，主要分析由本人完成",
                                "课程材料、公开数据与项目仓库",
                                "无",
                                true,
                                new MultipartFile[0]);
                    } catch (Exception exception) {
                        throw new IllegalStateException("仿真提交创建失败", exception);
                    }
                });
    }

    private void completeReview(Account teacher, Submission submission, String feedback) {
        if ("REVIEWED".equals(submission.getReviewStatus())) {
            return;
        }
        var draft = reviewService.requireDraft(submission.getId());
        reviewService.save(
                submission.getId(),
                submission.getAssignmentTitle() + "审核意见",
                feedback,
                draft.getRevision());
        reviewService.completeReview(submission.getId());
    }

    private void saveScores(Submission submission) {
        List<ReviewScore> scores = scoreService.getOrCreate(submission.getId());
        Map<Long, Integer> values = scores.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ReviewScore::getId,
                        score -> Math.max(0, score.getMaxPoints() - 2)));
        scoreService.saveFinalScores(submission.getId(), values);
    }

    private Account teacher(String username, String name, String department, String title) {
        Account account = accountRepository.findByUsername(username)
                .orElseGet(() -> accountService.registerTrusted(
                        username,
                        TEACHER_PASSWORD,
                        name,
                        Account.Role.TEACHER,
                        null));
        if (!account.isApproved()) {
            account.approve(0L, "系统仿真数据账号");
            accountRepository.save(account);
        }
        accountService.updateProfile(username, name, null, department, title);
        return account;
    }

    private StudentSeed student(String username, String name, String number) {
        return new StudentSeed(username, name, number);
    }

    private List<Account> students(Classroom classroom, List<StudentSeed> seeds) {
        List<Account> accounts = seeds.stream()
                .map(seed -> accountRepository.findByUsername(seed.username())
                        .orElseGet(() -> accountService.register(
                                seed.username(),
                                STUDENT_PASSWORD,
                                seed.name(),
                                Account.Role.STUDENT,
                                seed.number())))
                .toList();
        classroomService.importRoster(
                accountRepository.findById(classroom.getTeacherId()).orElseThrow(),
                classroom.getId(),
                seeds.stream()
                        .map(seed -> new ClassroomService.RosterEntry(
                                seed.number(), seed.name()))
                        .toList());
        accounts.forEach(student -> classroomService.join(student, classroom.getJoinCode()));
        return accounts;
    }

    private Classroom classroom(
            Account teacher,
            String name,
            String course,
            Instant startAt,
            Instant endAt) {
        return classroomService.teacherClassrooms(teacher).stream()
                .filter(item -> item.getName().equals(name))
                .findFirst()
                .orElseGet(() -> classroomService.create(
                        teacher, name, course, startAt, endAt, false));
    }

    private Assignment assignment(
            Classroom classroom,
            String title,
            String instructions,
            Instant dueAt,
            String format,
            List<AssignmentService.RubricDefinition> rubrics) {
        return assignmentService.listForClassrooms(List.of(classroom.getId())).stream()
                .filter(item -> item.getTitle().equals(title))
                .findFirst()
                .orElseGet(() -> assignmentService.publish(
                        classroom.getId(),
                        title,
                        classroom.getCourseName(),
                        instructions,
                        dueAt,
                        format,
                        "ASSISTED_ALLOWED",
                        true,
                        true,
                        true,
                        rubrics));
    }

    private List<AssignmentService.RubricDefinition> rubrics(
            AssignmentService.RubricDefinition... rubrics) {
        return List.of(rubrics);
    }

    private AssignmentService.RubricDefinition rubric(
            String name, int points, String description) {
        return new AssignmentService.RubricDefinition(name, points, description);
    }

    private record StudentSeed(String username, String name, String number) {
    }
}
