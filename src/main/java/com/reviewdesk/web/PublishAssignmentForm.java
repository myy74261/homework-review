package com.reviewdesk.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 教师发布作业时填写的表单。
 *
 * <p>使用可变 JavaBean 而非 record，以支持动态评分量规的逐项绑定。</p>
 */
public class PublishAssignmentForm {

    @NotBlank(message = "请填写作业名称")
    private String title;

    @NotBlank(message = "请填写课程名称")
    private String courseName;

    @NotNull(message = "请选择发布课堂")
    private Long classroomId;

    @NotBlank(message = "请填写作业要求")
    private String instructions;

    @NotNull(message = "请设置截止时间")
    @Future(message = "截止时间必须晚于当前时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dueAt;

    @NotBlank(message = "请选择作业形式")
    private String submissionFormat;

    @NotBlank(message = "请选择 AI 使用政策")
    private String aiPolicy;

    private boolean aiDeclarationRequired;
    private boolean sourceDeclarationRequired;
    private boolean appealAllowed;

    @NotNull(message = "请配置评分量规")
    @Size(min = 1, max = 10, message = "评分维度需要 1–10 项")
    private List<@Valid RubricForm> rubrics = new ArrayList<>();

    public PublishAssignmentForm() {
        // Spring MVC 表单绑定使用
    }

    public static PublishAssignmentForm defaults(Long classroomId) {
        PublishAssignmentForm form = new PublishAssignmentForm();
        form.title = "短视频使用与大学生注意力恢复的关系研究";
        form.courseName = "传播学方法课";
        form.classroomId = classroomId;
        form.instructions = "完成一篇 3,000 字左右的调查报告，说明研究问题、研究方法、"
                + "数据来源与主要发现，并附参考文献。";
        form.dueAt = LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);
        form.submissionFormat = "MIXED";
        form.aiPolicy = "ASSISTED_ALLOWED";
        form.aiDeclarationRequired = true;
        form.sourceDeclarationRequired = true;
        form.appealAllowed = true;
        form.rubrics = new ArrayList<>(List.of(
                new RubricForm(
                        "研究问题与目标", 25, "问题、目标与研究边界是否清楚"),
                new RubricForm(
                        "证据与引用", 25, "结论是否有可追溯的数据或文献支持"),
                new RubricForm(
                        "方法与数据", 30, "样本、抽样、回收率与统计过程是否透明"),
                new RubricForm(
                        "结构与表达", 20, "论证顺序、段落衔接与学术表达是否清晰")));
        return form;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Long classroomId) {
        this.classroomId = classroomId;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getSubmissionFormat() {
        return submissionFormat;
    }

    public void setSubmissionFormat(String submissionFormat) {
        this.submissionFormat = submissionFormat;
    }

    public String getAiPolicy() {
        return aiPolicy;
    }

    public void setAiPolicy(String aiPolicy) {
        this.aiPolicy = aiPolicy;
    }

    public boolean isAiDeclarationRequired() {
        return aiDeclarationRequired;
    }

    public void setAiDeclarationRequired(boolean aiDeclarationRequired) {
        this.aiDeclarationRequired = aiDeclarationRequired;
    }

    public boolean isSourceDeclarationRequired() {
        return sourceDeclarationRequired;
    }

    public void setSourceDeclarationRequired(boolean sourceDeclarationRequired) {
        this.sourceDeclarationRequired = sourceDeclarationRequired;
    }

    public boolean isAppealAllowed() {
        return appealAllowed;
    }

    public void setAppealAllowed(boolean appealAllowed) {
        this.appealAllowed = appealAllowed;
    }

    public List<RubricForm> getRubrics() {
        return rubrics;
    }

    public void setRubrics(List<RubricForm> rubrics) {
        this.rubrics = rubrics == null ? new ArrayList<>() : rubrics;
    }

    /**
     * 动态量规中的一行。
     */
    public static class RubricForm {

        @NotBlank(message = "请填写评分维度")
        private String name;

        @Min(value = 1, message = "分值必须大于 0")
        @Max(value = 100, message = "单个维度不能超过 100 分")
        private int points;

        @NotBlank(message = "请填写评分说明")
        private String description;

        public RubricForm() {
            // Spring MVC 动态绑定使用
        }

        public RubricForm(String name, int points, String description) {
            this.name = name;
            this.points = points;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
