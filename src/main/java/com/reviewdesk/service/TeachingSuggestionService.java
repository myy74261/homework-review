package com.reviewdesk.service;

import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.AssignmentRubric;
import com.reviewdesk.domain.LessonPlan;
import com.reviewdesk.domain.ReviewIssue;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.LessonPlanRepository;
import com.reviewdesk.repository.ReviewIssueRepository;
import com.reviewdesk.repository.SubmissionRepository;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 把教师确认的问题通过确定性规则转换为课堂讲解建议。
 *
 * <p>规则只整理已确认的学生原文和教师意见，不生成新的学生评价，
 * 也不推断学生是否使用外部帮助。</p>
 */
@Service
@Transactional(readOnly = true)
public class TeachingSuggestionService {

    private final AssignmentService assignmentService;
    private final SubmissionRepository submissionRepository;
    private final ReviewIssueRepository issueRepository;
    private final LessonPlanRepository planRepository;
    private final AiSuggestionClient aiSuggestionClient;
    private final AiSettingsService aiSettingsService;

    public TeachingSuggestionService(AssignmentService assignmentService,
                                     SubmissionRepository submissionRepository,
                                     ReviewIssueRepository issueRepository,
                                     LessonPlanRepository planRepository,
                                     AiSuggestionClient aiSuggestionClient,
                                     AiSettingsService aiSettingsService) {
        this.assignmentService = assignmentService;
        this.submissionRepository = submissionRepository;
        this.issueRepository = issueRepository;
        this.planRepository = planRepository;
        this.aiSuggestionClient = aiSuggestionClient;
        this.aiSettingsService = aiSettingsService;
    }

    @Transactional
    public LessonPlanWorkspace getOrCreateWorkspace(long assignmentId) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        List<AssignmentRubric> rubrics = assignmentService.listRubrics(assignmentId);
        List<EvidenceView> evidence = loadEvidence(assignmentId);
        LessonPlan plan = planRepository.findById(assignmentId)
                .orElseGet(() -> {
                    String title = assignment.getTitle() + " · 课堂讲解建议";
                    String content = generate(assignment, rubrics, evidence);
                    return planRepository.save(new LessonPlan(
                            assignmentId, title, content));
                });
        return new LessonPlanWorkspace(
                assignment,
                plan,
                evidence,
                summarize(rubrics, evidence));
    }

    @Transactional
    public LessonPlan save(long assignmentId, String title, String content,
                           long baseRevision) {
        LessonPlan plan = requirePlan(assignmentId);
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new IllegalArgumentException("备课稿标题和正文不能为空");
        }
        plan.update(title.strip(), content.strip(), baseRevision);
        return planRepository.save(plan);
    }

    /**
     * 重新读取当前共性问题并覆盖备课稿，形成新版本。
     */
    @Transactional
    public LessonPlan regenerate(long assignmentId) {
        return regenerate(assignmentId, aiSuggestionClient.defaultProvider());
    }

    @Transactional
    public LessonPlan regenerate(long assignmentId, String teacherUsername) {
        return regenerate(assignmentId, aiSettingsService.resolve(teacherUsername));
    }

    private LessonPlan regenerate(
            long assignmentId, AiSuggestionClient.ProviderConfig provider) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        List<AssignmentRubric> rubrics = assignmentService.listRubrics(assignmentId);
        List<EvidenceView> evidence = loadEvidence(assignmentId);
        LessonPlan plan = requirePlan(assignmentId);
        String deterministic = generate(assignment, rubrics, evidence);
        String content = aiSuggestionClient.enhanceLessonPlan(
                        provider,
                        assignment.getTitle(),
                        assignment.getSubmissionFormat(),
                        deterministic,
                        evidence.stream()
                                .limit(12)
                                .map(item -> "维度：" + item.issue().getRubricName()
                                        + "；原文：" + singleLine(item.issue().getEvidenceQuote())
                                        + "；教师判断：" + singleLine(item.issue().getTeacherNote()))
                                .collect(Collectors.joining("\n")))
                .orElse(deterministic);
        plan.replaceGenerated(
                assignment.getTitle() + " · 课堂讲解建议",
                content);
        return planRepository.save(plan);
    }

    public boolean isAiConfigured(String teacherUsername) {
        return aiSettingsService.resolve(teacherUsername).configured();
    }

    private LessonPlan requirePlan(long assignmentId) {
        return planRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("备课稿尚未生成"));
    }

    private List<EvidenceView> loadEvidence(long assignmentId) {
        List<Submission> submissions = submissionRepository
                .findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
        if (submissions.isEmpty()) {
            return List.of();
        }
        Map<Long, Submission> byId = submissions.stream()
                .collect(Collectors.toMap(Submission::getId, Function.identity()));
        return issueRepository.findBySubmissionIdInOrderByCreatedAtDesc(byId.keySet())
                .stream()
                .filter(issue -> byId.containsKey(issue.getSubmissionId()))
                .map(issue -> new EvidenceView(issue, byId.get(issue.getSubmissionId())))
                .toList();
    }

    private List<IssueGroup> summarize(List<AssignmentRubric> rubrics,
                                       List<EvidenceView> evidence) {
        Map<String, List<EvidenceView>> grouped = new LinkedHashMap<>();
        for (AssignmentRubric rubric : rubrics) {
            grouped.put(rubric.getName(), new ArrayList<>());
        }
        for (EvidenceView item : evidence) {
            grouped.computeIfAbsent(item.issue().getRubricName(), key -> new ArrayList<>())
                    .add(item);
        }
        return grouped.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> new IssueGroup(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(item -> item.issue().getTeacherNote())
                                .distinct()
                                .toList()))
                .toList();
    }

    private String generate(Assignment assignment, List<AssignmentRubric> rubrics,
                            List<EvidenceView> evidence) {
        List<IssueGroup> groups = summarize(rubrics, evidence);
        StringBuilder content = new StringBuilder();
        content.append("一、课堂目标\n\n")
                .append("围绕《").append(assignment.getTitle()).append("》，")
                .append("处理本班已确认的 ").append(evidence.size())
                .append(" 处作业问题，让学生能够说明证据来源、方法边界和结论成立条件。\n\n");

        content.append("二、共性问题\n\n");
        if (groups.isEmpty()) {
            content.append("当前还没有教师确认的共性问题。")
                    .append("请先在审核工作台确认问题，再重新生成备课建议。\n\n");
        } else {
            for (IssueGroup group : groups) {
                RuleGuide guide = guideFor(group.rubricName());
                content.append("【").append(group.rubricName()).append("】")
                        .append(group.count()).append(" 处\n")
                        .append("共性表现：").append(guide.commonPattern()).append("\n")
                        .append("讲解动作：").append(guide.teachingMove()).append("\n")
                        .append("建议追问：").append(guide.question()).append("\n")
                        .append("对应练习：").append(guide.practice()).append("\n\n");
            }
        }

        content.append("三、典型错例与证据\n\n");
        if (evidence.isEmpty()) {
            content.append("暂无可引用的教师确认问题。\n\n");
        } else {
            evidence.stream().limit(3).forEach(item -> {
                content.append("学生：").append(item.submission().getStudentName())
                        .append("｜维度：").append(item.issue().getRubricName()).append("\n")
                        .append("原文：“").append(singleLine(item.issue().getEvidenceQuote()))
                        .append("”\n")
                        .append("教师判断：").append(item.issue().getTeacherNote()).append("\n\n");
            });
        }

        content.append("四、课堂讲解流程（建议 15 分钟）\n\n")
                .append("1. 用一条典型原文开场，请学生判断结论需要哪些证据。\n")
                .append("2. 对照评分量规标注证据、方法和表达上的缺口。\n")
                .append("3. 示范一次可执行的修改，再让学生独立修改另一条问题。\n")
                .append("4. 回收修改结果，只比较证据是否更清楚、表达是否更准确。\n\n");

        content.append("五、课后练习\n\n");
        if (groups.isEmpty()) {
            content.append("选择一个研究结论，补写其数据来源、样本信息与适用边界。\n\n");
        } else {
            groups.forEach(group -> content.append("· ")
                    .append(guideFor(group.rubricName()).practice()).append("\n"));
            content.append("\n");
        }

        content.append("六、判断边界\n\n")
                .append("本建议只整理教师已经确认的问题和证据，不评价学生是否独立完成作业。")
                .append("课堂材料应由教师审核后再使用。\n");
        return content.toString();
    }

    private RuleGuide guideFor(String rubricName) {
        if (rubricName.contains("证据") || rubricName.contains("引用")) {
            return new RuleGuide(
                    "结论与证据之间缺少清晰对应，来源信息不足。",
                    "把结论、来源、样本和效应量并排展示，演示如何逐项核对。",
                    "这条结论需要哪一项数据或文献才能成立？",
                    "为一条已有结论补充来源、样本量和适用范围。");
        }
        if (rubricName.contains("方法") || rubricName.contains("数据")) {
            return new RuleGuide(
                    "研究方法或数据过程说明不完整，读者难以复现。",
                    "用流程图呈现抽样、回收和筛选步骤，标出仍缺少的信息。",
                    "如果换一位读者，他能否按现有说明重复这项研究？",
                    "为一份调查补写抽样方式、回收率和有效样本筛选标准。");
        }
        if (rubricName.contains("问题") || rubricName.contains("目标")) {
            return new RuleGuide(
                    "研究问题、研究目标与正文内容之间需要更明确的对应。",
                    "让学生把研究问题改写成一个可检验的句子，并圈出对应的研究动作。",
                    "这段内容回答了研究问题中的哪一部分？",
                    "把一个宽泛主题改写为问题、目标和可检验变量。");
        }
        if (rubricName.contains("结构") || rubricName.contains("表达")) {
            return new RuleGuide(
                    "段落之间的论证关系不够清楚，核心判断不够突出。",
                    "用“观点—证据—解释—小结”的链条重新排列一个段落。",
                    "这一段的新信息是什么，它如何支持上一段？",
                    "为一段论证补充主题句、证据解释和过渡句。");
        }
        return new RuleGuide(
                "同类问题在多个提交中重复出现，需要形成统一修改方法。",
                "先展示正反例，再使用评分量规解释判断依据。",
                "怎样修改才能让读者更快看见证据和判断？",
                "根据评分量规修改一段代表性文字并说明修改理由。");
    }

    private String singleLine(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }

    public record EvidenceView(ReviewIssue issue, Submission submission) {
    }

    public record IssueGroup(String rubricName, long count, List<String> teacherNotes) {
    }

    public record LessonPlanWorkspace(
            Assignment assignment,
            LessonPlan plan,
            List<EvidenceView> evidence,
            List<IssueGroup> groups
    ) {
    }

    private record RuleGuide(String commonPattern, String teachingMove,
                             String question, String practice) {
    }
}
