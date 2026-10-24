package com.reviewdesk.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.repository.AccountRepository;
import com.reviewdesk.repository.AppealRepository;
import com.reviewdesk.repository.AssignmentRepository;
import com.reviewdesk.repository.IntegrityFlagRepository;
import com.reviewdesk.repository.SubmissionAttachmentRepository;
import com.reviewdesk.repository.SubmissionRepository;
import com.reviewdesk.service.ReviewScoreService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证混合提交、动态量规、诚信核验、可解释评分与学生申诉。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "review-desk.storage.root=target/test-uploads")
class AdvancedReviewFeaturesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionAttachmentRepository attachmentRepository;

    @Autowired
    private IntegrityFlagRepository integrityFlagRepository;

    @Autowired
    private AppealRepository appealRepository;

    @Autowired
    private ReviewScoreService scoreService;

    @Test
    void mixedSubmissionSupportsDynamicRubricAndScreenshotAttachment() throws Exception {
        mockMvc.perform(post("/teacher/assignments")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .param("title", "交互设计复盘")
                        .param("courseName", "产品设计")
                        .param("classroomId", "1")
                        .param("instructions", "提交设计说明、关键界面截图与可复现的原型文件。")
                        .param("dueAt", "2027-02-10T23:59")
                        .param("submissionFormat", "MIXED")
                        .param("aiPolicy", "ASSISTED_ALLOWED")
                        .param("aiDeclarationRequired", "true")
                        .param("sourceDeclarationRequired", "true")
                        .param("appealAllowed", "true")
                        .param("rubrics[0].name", "设计依据")
                        .param("rubrics[0].points", "40")
                        .param("rubrics[0].description", "是否说明用户与场景证据")
                        .param("rubrics[1].name", "交互完成度")
                        .param("rubrics[1].points", "60")
                        .param("rubrics[1].description", "关键流程是否完整可验证"))
                .andExpect(status().is3xxRedirection());

        Assignment assignment = assignmentRepository.findById(2L).orElseThrow();
        assertThat(assignment.getSubmissionFormat()).isEqualTo("MIXED");

        MockMultipartFile screenshot = new MockMultipartFile(
                "files",
                "prototype.png",
                "image/png",
                "fake-png-content".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/student/assignments/{id}/submissions", assignment.getId())
                        .file(screenshot)
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf())
                        .param("content", "这是设计说明正文，包含用户场景、关键流程与验证结论。")
                        .param("aiUsage", "ASSISTED")
                        .param("aiTools", "图像编辑工具")
                        .param("aiScope", "仅用于草图排版")
                        .param("sourceDeclaration", "课堂访谈记录与自建原型")
                        .param("collaborationDeclaration", "无")
                        .param("integrityConfirmed", "true"))
                .andExpect(status().is3xxRedirection());

        var submission = submissionRepository
                .findFirstByAssignmentIdAndStudentAccountIdOrderByAttemptNumberDesc(
                        assignment.getId(), student().getId())
                .orElseThrow();
        assertThat(attachmentRepository.findBySubmissionIdOrderByIdAsc(submission.getId()))
                .singleElement()
                .satisfies(attachment -> {
                    assertThat(attachment.getAttachmentKind()).isEqualTo("SCREENSHOT");
                    assertThat(attachment.getOriginalName()).isEqualTo("prototype.png");
                });
    }

    @Test
    void requiredDeclarationsAndDisallowedAiUsageAreRejected() throws Exception {
        long before = submissionRepository.count();
        String content = "这是一份用于验证诚信声明和 AI 使用政策的学生作业正文。";

        mockMvc.perform(post("/student/assignments/1/submissions")
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf())
                        .param("content", content)
                        .param("aiUsage", "NONE")
                        .param("integrityConfirmed", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(submissionRepository.count()).isEqualTo(before);

        mockMvc.perform(post("/student/assignments/1/submissions")
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf())
                        .param("content", content)
                        .param("aiUsage", "ASSISTED")
                        .param("sourceDeclaration", "课程材料")
                        .param("integrityConfirmed", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(submissionRepository.count()).isEqualTo(before);

        mockMvc.perform(post("/student/assignments/1/submissions")
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf())
                        .param("content", content)
                        .param("aiUsage", "ASSISTED")
                        .param("aiTools", "文本编辑器")
                        .param("sourceDeclaration", "课程材料")
                        .param("integrityConfirmed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/assignments/1"));
        assertThat(submissionRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void refreshingSubmissionEndpointReturnsToAssignmentInsteadOf405() throws Exception {
        mockMvc.perform(get("/student/assignments/1/submissions")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/assignments/1"));
    }

    @Test
    void errorPageUsesFriendlyChineseFallback() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr("jakarta.servlet.error.status_code", 405)
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("操作方式不正确")))
                .andExpect(content().string(containsString("返回我的工作台")));
    }

    @Test
    void suggestionsAreExplainableAndTeacherFinalScoresAreIndependent() throws Exception {
        mockMvc.perform(get("/reviews/1").with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("建议 21 / 25")))
                .andExpect(content().string(containsString("已确认 1 处问题")))
                .andExpect(content().string(containsString("教师最终分")));

        var scores = scoreService.getOrCreate(1L);
        Map<String, Integer> finals = Map.of(
                "证据与引用", 20,
                "方法与数据", 25,
                "研究问题与目标", 22,
                "结构与表达", 16);
        var request = post("/teacher/submissions/1/scores")
                .with(user("chen").roles("TEACHER"))
                .with(csrf());
        scores.forEach(score -> request.param(
                "score_" + score.getId(),
                String.valueOf(finals.get(score.getRubricName()))));
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/1#scores"));
        assertThat(scoreService.finalTotal(1L)).isEqualTo(83);
    }

    @Test
    void suspiciousFlagStaysPrivateUntilConfirmedAndStudentCanAppeal() throws Exception {
        mockMvc.perform(post("/teacher/submissions/1/integrity-flags")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .param("category", "引用来源")
                        .param("severity", "MEDIUM")
                        .param("evidence", "已有研究表明，短视频使用会影响注意力恢复。")
                        .param("reason", "该结论缺少可追溯的参考文献。"))
                .andExpect(status().is3xxRedirection());

        var flag = integrityFlagRepository
                .findBySubmissionIdOrderByCreatedAtDesc(1L).get(0);
        assertThat(flag.getStatus()).isEqualTo("SUSPECTED");

        mockMvc.perform(get("/student/assignments/1").with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("该结论缺少可追溯的参考文献"))));

        mockMvc.perform(post("/teacher/integrity-flags/{id}/resolve", flag.getId())
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .param("status", "CONFIRMED")
                        .param("submissionId", "1"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/api/submissions/1/review/complete")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/student/assignments/1").with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("教师已确认的诚信核验结果")))
                .andExpect(content().string(containsString("该结论缺少可追溯的参考文献")))
                .andExpect(content().string(containsString("我的申诉")));

        mockMvc.perform(post("/student/submissions/1/appeals")
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf())
                        .param("reason", "原文已在附注中标注课程讲义来源，希望教师重新核对。")
                        .param("requestedResolution", "重新核对引用证据并更新诚信结论。"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/assignments/1#appeals"));
        assertThat(appealRepository.findBySubmissionIdOrderByCreatedAtDesc(1L))
                .hasSize(1);

        mockMvc.perform(get("/teacher").with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("学生申诉队列")))
                .andExpect(content().string(containsString("原文已在附注中标注课程讲义来源")));

        long appealId = appealRepository.findBySubmissionIdOrderByCreatedAtDesc(1L)
                .get(0).getId();
        mockMvc.perform(post("/teacher/appeals/{id}/respond", appealId)
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .param("status", "ACCEPTED")
                        .param("teacherResponse", "已核对讲义附注，采纳申诉并更新说明。"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher#appeal-" + appealId));
        assertThat(appealRepository.findById(appealId).orElseThrow().getStatus())
                .isEqualTo("ACCEPTED");
    }

    private Account student() {
        return accountRepository.findByUsername("lin").orElseThrow();
    }
}
