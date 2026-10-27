package com.reviewdesk.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewdesk.domain.Account;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.repository.ClassroomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证登录、角色隔离、账号注册与课堂数据边界。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountAndClassroomSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Test
    void demoTeacherCanLogIn() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("登录账号")));

        mockMvc.perform(formLogin("/login")
                        .user("chen")
                        .password("Teach1234"))
                .andExpect(authenticated().withUsername("chen"))
                .andExpect(redirectedUrl("/app/teacher"));
    }

    @Test
    void roleRoutesAreSeparated() throws Exception {
        mockMvc.perform(get("/teacher")
                        .with(user("lin").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/student")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedTeacherPageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void anotherTeacherCannotReadExistingSubmission() throws Exception {
        accountService.register(
                "teacher2", "Other1234", "周老师", Account.Role.TEACHER, null);

        mockMvc.perform(get("/reviews/1")
                        .with(user("teacher2").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void newStudentCanRegisterAndJoinClassroomByCode() throws Exception {
        String joinCode = classroomRepository.findAll().get(0).getJoinCode();

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "student2")
                        .param("password", "Student5678")
                        .param("displayName", "赵同学")
                        .param("role", "STUDENT")
                        .param("studentNumber", "20230117"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        mockMvc.perform(post("/student/join")
                        .with(user("student2").roles("STUDENT"))
                        .with(csrf())
                        .param("joinCode", joinCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"));

        mockMvc.perform(get("/student")
                        .with(user("student2").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("传播学方法课")));
    }
}
