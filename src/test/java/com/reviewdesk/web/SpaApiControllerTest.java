package com.reviewdesk.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    @Test
    void reactEntryAndTeacherSessionAreAvailable() throws Exception {
        mockMvc.perform(get("/app/teacher").with(user("chen").roles("TEACHER")))
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
                .andExpect(jsonPath("$.reviewChecklist.length()").isNotEmpty());

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
}
