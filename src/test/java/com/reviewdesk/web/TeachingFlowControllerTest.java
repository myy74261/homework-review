package com.reviewdesk.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.reviewdesk.repository.ReviewEventRepository;
import com.reviewdesk.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证发布作业、学生提交、教师审核和问题汇总的 P2 主链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeachingFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewEventRepository eventRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Test
    void teacherAndStudentPortalsRender() throws Exception {
        mockMvc.perform(get("/teacher").with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("我的课堂")))
                .andExpect(content().string(containsString("发布新作业")))
                .andExpect(content().string(containsString("诚信与可追溯")));

        mockMvc.perform(get("/student").with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("我的作业")))
                .andExpect(content().string(containsString("评分量规")));
    }

    @Test
    void teacherCanPublishAssignmentWithRubric() throws Exception {
        mockMvc.perform(get("/teacher/assignments/new")
                        .with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("评分量规")))
                .andExpect(content().string(containsString("发布作业并开放提交")));

        mockMvc.perform(post("/teacher/assignments")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .param("title", "媒介使用与学习策略调查")
                        .param("courseName", "传播学方法课")
                        .param("classroomId", "1")
                        .param("instructions", "完成一份调查说明，并展示数据来源与核心发现。")
                        .param("dueAt", "2027-01-10T23:59")
                        .param("submissionFormat", "MIXED")
                        .param("aiPolicy", "ASSISTED_ALLOWED")
                        .param("aiDeclarationRequired", "true")
                        .param("sourceDeclarationRequired", "true")
                        .param("appealAllowed", "true")
                        .param("rubrics[0].name", "研究问题")
                        .param("rubrics[0].points", "25")
                        .param("rubrics[0].description", "研究目标是否清楚")
                        .param("rubrics[1].name", "证据")
                        .param("rubrics[1].points", "25")
                        .param("rubrics[1].description", "引用是否可追溯")
                        .param("rubrics[2].name", "方法")
                        .param("rubrics[2].points", "30")
                        .param("rubrics[2].description", "方法说明是否完整")
                        .param("rubrics[3].name", "表达")
                        .param("rubrics[3].points", "20")
                        .param("rubrics[3].description", "结构是否清晰"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/teacher?published=*"));
    }

    @Test
    void studentSubmissionEntersTeacherQueue() throws Exception {
        String content = "摘要\n\n本研究围绕课程作业问题展开分析，并说明数据来源、研究方法与主要发现。";

        mockMvc.perform(post("/student/assignments/1/submissions")
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("aiUsage", "NONE")
                        .param("sourceDeclaration", "课程材料与自建问卷")
                        .param("collaborationDeclaration", "无")
                        .param("integrityConfirmed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/assignments/1"));

        mockMvc.perform(get("/teacher").with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("第 3 次")))
                .andExpect(content().string(containsString("开始审核")));

        mockMvc.perform(get("/student/assignments/1").with(user("lin").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("我的提交内容")))
                .andExpect(content().string(containsString("本研究围绕课程作业问题展开分析")));
    }

    @Test
    void teacherCannotPublishEmptyFeedback() throws Exception {
        String content = "这是一份用于验证最终反馈发布条件的完整学生作业内容。";
        mockMvc.perform(post("/student/assignments/1/submissions")
                        .with(user("lin").roles("STUDENT"))
                        .with(csrf())
                        .param("content", content)
                        .param("aiUsage", "NONE")
                        .param("sourceDeclaration", "课程材料与自建问卷")
                        .param("collaborationDeclaration", "无")
                        .param("integrityConfirmed", "true"))
                .andExpect(status().is3xxRedirection());

        long submissionId = submissionRepository.findTopByOrderByIdDesc()
                .orElseThrow()
                .getId();
        mockMvc.perform(post("/api/submissions/" + submissionId + "/review/complete")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请先填写审核反馈再完成审核"));
    }

    @Test
    void confirmedIssueAppearsInTeacherSummary() throws Exception {
        mockMvc.perform(post("/api/submissions/1/issues")
                        .with(user("chen").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rubricName": "证据与引用",
                                  "evidenceQuote": "已有研究表明，短视频使用会影响注意力恢复。",
                                  "teacherNote": "请补充具体文献来源。"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/teacher").with(user("chen").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 处")));

        org.assertj.core.api.Assertions.assertThat(
                eventRepository.countBySubmissionIdAndEventType(
                        1L, "SUGGESTION_ADOPTED"))
                .isEqualTo(1L);
    }
}
