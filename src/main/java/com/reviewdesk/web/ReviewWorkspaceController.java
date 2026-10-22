package com.reviewdesk.web;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 渲染教师作业审核工作台。
 *
 * <p>P0 使用演示提交验证布局与信息层级；P1 再将教师反馈接入服务端保存。</p>
 */
@Controller
public class ReviewWorkspaceController {

    private static final String SUBMISSION_EXCERPT = """
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

    @GetMapping({"/", "/reviews/{submissionId}"})
    public String workspace(@PathVariable(required = false) Long submissionId, Model model) {
        long currentSubmissionId = submissionId == null ? 1L : submissionId;
        model.addAttribute("submissionId", currentSubmissionId);
        model.addAttribute("assignmentTitle", "短视频使用与大学生注意力恢复的关系研究");
        model.addAttribute("courseName", "传播学方法课");
        model.addAttribute("studentName", "林清和");
        model.addAttribute("studentNumber", "20230116");
        model.addAttribute("dueAt", "10 月 24 日 23:59");
        model.addAttribute("submittedAt", "10 月 21 日 22:48");
        model.addAttribute("attempt", "第 2 次提交");
        model.addAttribute("wordCount", "2,846");
        model.addAttribute("submissionExcerpt", SUBMISSION_EXCERPT);
        model.addAttribute("feedbackTitle", "课程论文第二次提交审核意见");
        model.addAttribute("feedbackContent", INITIAL_FEEDBACK);
        model.addAttribute("rubric", List.of(
                new RubricCriterion("研究问题与目标", 25, "达成", "问题、目标与研究边界是否清楚"),
                new RubricCriterion("证据与引用", 25, "需补充", "结论是否有可追溯的数据或文献支持"),
                new RubricCriterion("方法与数据", 30, "需补充", "样本、抽样、回收率与统计过程是否透明"),
                new RubricCriterion("结构与表达", 20, "达成", "论证顺序、段落衔接与学术表达是否清晰")
        ));
        return "review-workspace";
    }
}

