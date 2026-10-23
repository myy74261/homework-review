package com.reviewdesk.web;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 教师发布作业时填写的表单。
 */
public record PublishAssignmentForm(
        @NotBlank(message = "请填写作业名称")
        String title,

        @NotBlank(message = "请填写课程名称")
        String courseName,

        @NotBlank(message = "请填写作业要求")
        String instructions,

        @NotNull(message = "请设置截止时间")
        @Future(message = "截止时间必须晚于当前时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime dueAt,

        @NotBlank(message = "请填写评分维度")
        String criterion1Name,
        @Min(1) @Max(100) int criterion1Points,
        @NotBlank(message = "请填写评分说明")
        String criterion1Description,

        @NotBlank(message = "请填写评分维度")
        String criterion2Name,
        @Min(1) @Max(100) int criterion2Points,
        @NotBlank(message = "请填写评分说明")
        String criterion2Description,

        @NotBlank(message = "请填写评分维度")
        String criterion3Name,
        @Min(1) @Max(100) int criterion3Points,
        @NotBlank(message = "请填写评分说明")
        String criterion3Description,

        @NotBlank(message = "请填写评分维度")
        String criterion4Name,
        @Min(1) @Max(100) int criterion4Points,
        @NotBlank(message = "请填写评分说明")
        String criterion4Description
) {

    public static PublishAssignmentForm defaults() {
        return new PublishAssignmentForm(
                "短视频使用与大学生注意力恢复的关系研究",
                "传播学方法课",
                "完成一篇 3,000 字左右的调查报告，说明研究问题、研究方法、"
                        + "数据来源与主要发现，并附参考文献。",
                LocalDateTime.of(2026, 10, 24, 23, 59),
                "研究问题与目标", 25, "问题、目标与研究边界是否清楚",
                "证据与引用", 25, "结论是否有可追溯的数据或文献支持",
                "方法与数据", 30, "样本、抽样、回收率与统计过程是否透明",
                "结构与表达", 20, "论证顺序、段落衔接与学术表达是否清晰");
    }
}

