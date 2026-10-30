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
import com.reviewdesk.repository.AiProviderSettingRepository;
import com.reviewdesk.repository.ClassroomRepository;
import com.reviewdesk.repository.ClassroomMembershipRepository;
import com.reviewdesk.repository.ReviewIssueRepository;
import com.reviewdesk.service.AccountService;
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
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AiReviewRunRepository aiReviewRunRepository;

    @Autowired
    private AiReviewSuggestionRepository aiReviewSuggestionRepository;

    @Autowired
    private ReviewIssueRepository reviewIssueRepository;

    @Autowired
    private AiProviderSettingRepository aiProviderSettingRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private ClassroomMembershipRepository membershipRepository;

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
                .andExpect(jsonPath("$.classroomAnalytics.length()").value(1))
                .andExpect(jsonPath("$.classroomAnalytics[0].classroomId").value(1))
                .andExpect(jsonPath("$.classroomAnalytics[0].enrolledStudentCount").value(1))
                .andExpect(jsonPath("$.classroomAnalytics[0].submittedStudentCount").value(1))
                .andExpect(jsonPath("$.classroomAnalytics[0].commonIssues.length()").value(2))
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
    void teacherCanWithdrawEditAndRepublishAnAssignment() throws Exception {
        mockMvc.perform(post("/api/app/teacher/assignments/1/withdraw")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.redirectPath")
                        .value("/app/teacher/assignments/1/edit"));

        mockMvc.perform(get("/api/app/student/dashboard")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments.length()").value(0));

        mockMvc.perform(get("/api/app/student/assignments/1")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/app/teacher/assignments/1/edit")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.assignment.title")
                        .value("短视频使用与大学生注意力恢复的关系研究"))
                .andExpect(jsonPath("$.submissionCount").value(1))
                .andExpect(jsonPath("$.rubrics.length()").value(4));

        mockMvc.perform(put("/api/app/teacher/assignments/1")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "短视频使用与注意力恢复（修订版）",
                                  "courseName": "传播学方法课",
                                  "classroomId": 1,
                                  "instructions": "修订后的作业要求。",
                                  "dueAt": "2099-06-30T23:59",
                                  "submissionFormat": "MIXED",
                                  "aiPolicy": "ASSISTED_ALLOWED",
                                  "aiDeclarationRequired": true,
                                  "sourceDeclarationRequired": true,
                                  "appealAllowed": true,
                                  "rubrics": [
                                    {
                                      "name": "研究问题与目标",
                                      "points": 25,
                                      "description": "问题、目标与研究边界是否清楚"
                                    },
                                    {
                                      "name": "证据与引用",
                                      "points": 25,
                                      "description": "结论是否有可追溯的数据或文献支持"
                                    },
                                    {
                                      "name": "方法与数据",
                                      "points": 30,
                                      "description": "样本、抽样、回收率与统计过程是否透明"
                                    },
                                    {
                                      "name": "结构与表达",
                                      "points": 20,
                                      "description": "论证顺序、段落衔接与学术表达是否清晰"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.redirectPath").value("/app/teacher"));

        mockMvc.perform(get("/api/app/student/dashboard")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments.length()").value(1))
                .andExpect(jsonPath("$.assignments[0].assignment.title")
                        .value("短视频使用与注意力恢复（修订版）"));
    }

    @Test
    void teacherCanChangeDeadlineWithoutWithdrawingTheAssignment() throws Exception {
        mockMvc.perform(put("/api/app/teacher/assignments/1/due-date")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dueAt": "2099-06-30T23:59"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.dueAt").value("2099-06-30T15:59:00Z"));

        mockMvc.perform(get("/api/app/student/dashboard")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments.length()").value(1))
                .andExpect(jsonPath("$.assignments[0].assignment.status").value("PUBLISHED"));
    }

    @Test
    void importedRosterLinksExistingAndFutureStudentAccounts() throws Exception {
        mockMvc.perform(post("/api/app/teacher/classrooms/1/roster")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "students": [
                                    {"studentNumber": "20230116", "studentName": "林清和"},
                                    {"studentNumber": "20230199", "studentName": "待注册学生"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rosterCount").value(2))
                .andExpect(jsonPath("$.linkedCount").value(1));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "student99")
                        .param("password", "Student9912")
                        .param("displayName", "待注册学生")
                        .param("role", "STUDENT")
                        .param("studentNumber", "20230199"))
                .andExpect(status().is3xxRedirection());

        org.assertj.core.api.Assertions.assertThat(
                        membershipRepository.countByClassroomId(1L))
                .isEqualTo(2);
    }

    @Test
    void rosterRestrictsClassroomCodeJoiningToListedStudents() throws Exception {
        mockMvc.perform(post("/api/app/teacher/classrooms/1/roster")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "students": [
                                    {"studentNumber": "20230116", "studentName": "林清和"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        accountService.register(
                "student_unlisted",
                "Student5678",
                "名单外学生",
                com.reviewdesk.domain.Account.Role.STUDENT,
                "20239999");

        String joinCode = classroomRepository.findById(1L).orElseThrow().getJoinCode();
        mockMvc.perform(post("/api/app/student/classrooms/join")
                        .with(user("student_unlisted").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinCode\":\"" + joinCode + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "你的学号不在该课堂名单中，请联系教师导入名单"));
    }

    @Test
    void classroomCollaboratorUsesIndependentAccountWithLimitedPermissions()
            throws Exception {
        var collaborator = accountService.register(
                "teacher_collab",
                "Teacher5678",
                "协作教师",
                com.reviewdesk.domain.Account.Role.TEACHER,
                null);

        mockMvc.perform(put("/api/app/teacher/classrooms/1/collaboration")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collaborationEnabled").value(true));

        mockMvc.perform(post("/api/app/teacher/classrooms/1/collaborators")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher_collab"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(collaborator.getId()))
                .andExpect(jsonPath("$.username").value("teacher_collab"));

        mockMvc.perform(get("/api/app/teacher/dashboard")
                        .with(user("teacher_collab").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classrooms.length()").value(1))
                .andExpect(jsonPath("$.classrooms[0].owner").value(false))
                .andExpect(jsonPath("$.classrooms[0].collaboratorCount").value(1));

        mockMvc.perform(get("/api/app/teacher/reviews/1")
                        .with(user("teacher_collab").roles("TEACHER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/app/teacher/classrooms/1/collaborators")
                        .with(user("teacher_collab").roles("TEACHER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/app/teacher/classrooms/1/dissolve")
                        .with(user("teacher_collab").roles("TEACHER"))
                        .with(csrf()))
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
                                  "courseName": "新媒体设计",
                                  "startAt": "2026-01-01T00:00:00Z",
                                  "endAt": "2099-06-30T15:59:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("新媒体设计课 · 2026"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.joinCode").isNotEmpty());
    }

    @Test
    void teacherCanRescheduleAndDissolveAClassroomOnlyAfterItEnds() throws Exception {
        mockMvc.perform(post("/api/app/teacher/classrooms")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "课程时间测试课堂",
                                  "courseName": "课程时间测试",
                                  "startAt": "2026-01-01T00:00:00Z",
                                  "endAt": "2099-06-30T15:59:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        var classroom = classroomRepository.findAll().stream()
                .filter(item -> "课程时间测试课堂".equals(item.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/api/app/teacher/classrooms/{id}/dissolve", classroom.getId())
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("课程尚未结束，不能解散课堂"));

        mockMvc.perform(put("/api/app/teacher/classrooms/{id}/schedule", classroom.getId())
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2025-01-01T00:00:00Z",
                                  "endAt": "2025-06-30T15:59:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));

        mockMvc.perform(post("/api/app/teacher/classrooms/{id}/dissolve", classroom.getId())
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISSOLVED"));

        org.assertj.core.api.Assertions.assertThat(
                        classroomRepository.findById(classroom.getId()).orElseThrow()
                                .getDissolvedAt())
                .isNotNull();
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
    void teacherCanUpdateProfileDetailsWithoutChangingUsernameOrRole() throws Exception {
        mockMvc.perform(put("/api/app/account/profile")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "陈老师（教研）",
                                  "email": "chen@example.edu.cn",
                                  "department": "新闻传播学院",
                                  "professionalTitle": "副教授"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("chen"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.displayName").value("陈老师（教研）"))
                .andExpect(jsonPath("$.email").value("chen@example.edu.cn"))
                .andExpect(jsonPath("$.department").value("新闻传播学院"))
                .andExpect(jsonPath("$.professionalTitle").value("副教授"));

        var account = accountRepository.findByUsername("chen").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(account.getDisplayName())
                .isEqualTo("陈老师（教研）");
    }

    @Test
    void accountLogoutAndDeactivationRequireTheCurrentSession() throws Exception {
        mockMvc.perform(post("/api/app/account/logout")
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/app/account/deactivate")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Teach1234"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "请先处理并解散所有现有课堂，再注销教师账号"));

        mockMvc.perform(post("/api/app/account/deactivate")
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Student1234"
                                }
                                """))
                .andExpect(status().isNoContent());

        var student = accountRepository.findByUsername("lin").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(student.isActive()).isFalse();
        org.assertj.core.api.Assertions.assertThat(student.getDeactivatedAt()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(
                        membershipRepository.countByClassroomId(1L))
                .isZero();
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
                        "AI 模型尚未配置，请在教师端“个人中心 → AI 接口设置”中接入"));
    }

    @Test
    void teacherCanStoreEncryptedAiProviderConfiguration() throws Exception {
        mockMvc.perform(get("/api/app/teacher/ai-settings")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.providers.length()").value(5));

        mockMvc.perform(put("/api/app/teacher/ai-settings")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "DEEPSEEK",
                                  "model": "deepseek-chat",
                                  "apiKey": "test-secret-api-key-1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.source").value("TEACHER"))
                .andExpect(jsonPath("$.providerName").value("DeepSeek"))
                .andExpect(jsonPath("$.maskedKey").value("••••1234"));

        var teacher = accountRepository.findByUsername("chen").orElseThrow();
        var stored = aiProviderSettingRepository
                .findByTeacherId(teacher.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stored.getEncryptedApiKey())
                .doesNotContain("test-secret-api-key-1234");
    }
}
