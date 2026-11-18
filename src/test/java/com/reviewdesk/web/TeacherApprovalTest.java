package com.reviewdesk.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewdesk.domain.Account;
import com.reviewdesk.repository.AccountRepository;
import com.reviewdesk.security.ReviewDeskUserDetailsService;
import com.reviewdesk.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证教师注册必须经过平台管理员审核。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeacherApprovalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ReviewDeskUserDetailsService userDetailsService;

    @Test
    void newTeacherCannotLoginUntilAdministratorApproves() throws Exception {
        Account teacher = accountService.register(
                "pending_teacher",
                "Teacher2026!",
                "待审核教师",
                Account.Role.TEACHER,
                null);
        assertThat(teacher.getApprovalStatus()).isEqualTo("PENDING");
        assertThat(userDetailsService.loadUserByUsername("pending_teacher").isEnabled())
                .isFalse();

        mockMvc.perform(post("/login")
                        .param("username", "pending_teacher")
                        .param("password", "Teacher2026!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?pending"));

        mockMvc.perform(get("/api/app/admin/teachers")
                        .param("status", "PENDING")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").isNumber());

        mockMvc.perform(get("/api/app/admin/teachers")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/app/admin/teachers/{id}/review", teacher.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED",
                                  "note": "教师身份材料完整"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        Account approved = accountRepository.findByUsername("pending_teacher").orElseThrow();
        assertThat(approved.isApproved()).isTrue();
        assertThat(userDetailsService.loadUserByUsername("pending_teacher").isEnabled())
                .isTrue();
    }

    @Test
    void rejectedTeacherReceivesReasonAndRemainsDisabled() throws Exception {
        Account teacher = accountService.register(
                "rejected_teacher",
                "Teacher2026!",
                "未通过教师",
                Account.Role.TEACHER,
                null);
        mockMvc.perform(post("/api/app/admin/teachers/{id}/review", teacher.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REJECTED",
                                  "note": "未提供教师身份证明材料"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.reviewNote").value("未提供教师身份证明材料"));
        assertThat(userDetailsService.loadUserByUsername("rejected_teacher").isEnabled())
                .isFalse();
    }
}
