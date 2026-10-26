package com.reviewdesk.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewdesk.domain.AiReviewRun;
import com.reviewdesk.domain.AiReviewSuggestion;
import com.reviewdesk.repository.AccountRepository;
import com.reviewdesk.repository.AiReviewRunRepository;
import com.reviewdesk.repository.AiReviewSuggestionRepository;
import com.reviewdesk.repository.ReviewIssueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证 React 前端的会话、角色路由和 JSON 数据契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SpaApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AiReviewRunRepository aiReviewRunRepository;

    @Autowired
    private AiReviewSuggestionRepository aiReviewSuggestionRepository;

    @Autowired
    private ReviewIssueRepository reviewIssueRepository;

    @Test
    void reactEntryAndTeacherSessionAreAvailable() throws Exception {
        mockMvc.perform(get("/app/teacher").with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/app/index.html"));

        mockMvc.perform(get("/app/account").with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/app/index.html"));

        mockMvc.perform(get("/api/app/session").with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("chen"))
                .andExpect(jsonPath("$.user.role").value("TEACHER"))
                .andExpect(jsonPath("$.csrfToken").isNotEmpty())
                .andExpect(jsonPath("$.homePath").value("/app/teacher"));
    }

    @Test
    void teacherApiProvidesDashboardReviewAndLessonData() throws Exception {
        mockMvc.perform(get("/api/app/teacher/dashboard")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments.length()").value(1))
                .andExpect(jsonPath("$.submissions.length()").value(1))
                .andExpect(jsonPath("$.classrooms[0].joinCode").isNotEmpty());

        mockMvc.perform(get("/api/app/teacher/reviews/1")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submission.id").value(1))
                .andExpect(jsonPath("$.review.revision").value(0))
                .andExpect(jsonPath("$.scores.length()").value(4))
                .andExpect(jsonPath("$.reviewChecklist.length()").isNotEmpty())
                .andExpect(jsonPath("$.aiConfigured").value(false))
                .andExpect(jsonPath("$.aiReviews.length()").value(0));

        mockMvc.perform(get("/api/app/teacher/assignments/1/lesson-plan")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment.id").value(1))
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    @Test
    void studentApiProvidesAssignmentAndRespectsRoleBoundaries() throws Exception {
        mockMvc.perform(get("/api/app/student/dashboard")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.assignments.length()").value(1));

        mockMvc.perform(get("/api/app/student/assignments/1")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment.id").value(1))
                .andExpect(jsonPath("$.rubrics.length()").value(4))
                .andExpect(jsonPath("$.latestSubmission.content").isNotEmpty());

        mockMvc.perform(get("/api/app/teacher/dashboard")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/app/student/dashboard")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactClientCanCreateClassroomThroughJsonApi() throws Exception {
        mockMvc.perform(post("/api/app/teacher/classrooms")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "新媒体设计课 · 2026",
                                  "courseName": "新媒体设计"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("新媒体设计课 · 2026"))
                .andExpect(jsonPath("$.joinCode").isNotEmpty());
    }

    @Test
    void authenticatedUserCanChangePasswordAfterVerifyingOldPassword() throws Exception {
        mockMvc.perform(put("/api/app/account/password")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Teach1234",
                                  "newPassword": "ChangedPass-2026!"
                                }
                                """))
                .andExpect(status().isNoContent());

        var account = accountRepository.findByUsername("chen").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(
                        passwordEncoder.matches("ChangedPass-2026!", account.getPasswordHash()))
                .isTrue();

        mockMvc.perform(put("/api/app/account/password")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Teach1234",
                                  "newPassword": "AnotherPass-2026!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("当前密码不正确"));
    }

    @Test
    void teacherCanReviewAiTraceAndExplicitlyAdoptSuggestion() throws Exception {
        AiReviewRun run = aiReviewRunRepository.save(new AiReviewRun(
                1L, "deepseek-chat", "模型认为引用需要进一步核对，最终由教师判断。"));
        AiReviewSuggestion suggestion = aiReviewSuggestionRepository.save(
                new AiReviewSuggestion(
                        run.getId(),
                        1L,
                        "证据与引用",
                        "已有研究表明，短视频使用会影响注意力恢复。",
                        "该结论缺少可追溯来源，请教师核对后决定是否要求补充引用。",
                        20));

        mockMvc.perform(get("/api/app/teacher/reviews/1")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiReviews[0].model").value("deepseek-chat"))
                .andExpect(jsonPath("$.aiReviews[0].suggestions[0].status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.aiReviews[0].suggestions[0].suggestedPoints")
                        .value(20));

        mockMvc.perform(post("/api/app/teacher/ai-review/suggestions/{id}/adopt",
                                suggestion.getId())
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADOPTED"));
        org.assertj.core.api.Assertions.assertThat(
                        reviewIssueRepository.countBySubmissionIdAndRubricName(
                                1L, "证据与引用"))
                .isEqualTo(2);
    }

    @Test
    void aiGenerationReturnsClearConfigurationErrorWhenKeyIsMissing() throws Exception {
        mockMvc.perform(post("/api/app/teacher/reviews/1/ai-review")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "AI 模型尚未配置，请在服务器设置 AI_API_KEY 后重启服务"));
    }
}
