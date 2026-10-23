package com.reviewdesk.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewdesk.repository.ReviewEventRepository;
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

    @Test
    void teacherAndStudentPortalsRender() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("我的课堂")))
                .andExpect(content().string(containsString("发布新作业")))
                .andExpect(content().string(containsString("诚信与可追溯")));

        mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("我的作业")))
                .andExpect(content().string(containsString("评分量规")));
    }

    @Test
    void teacherCanPublishAssignmentWithRubric() throws Exception {
        mockMvc.perform(get("/teacher/assignments/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("评分量规")))
                .andExpect(content().string(containsString("发布作业并开放提交")));

        mockMvc.perform(post("/teacher/assignments")
                        .param("title", "媒介使用与学习策略调查")
                        .param("courseName", "传播学方法课")
                        .param("instructions", "完成一份调查说明，并展示数据来源与核心发现。")
                        .param("dueAt", "2027-01-10T23:59")
                        .param("criterion1Name", "研究问题")
                        .param("criterion1Points", "25")
                        .param("criterion1Description", "研究目标是否清楚")
                        .param("criterion2Name", "证据")
                        .param("criterion2Points", "25")
                        .param("criterion2Description", "引用是否可追溯")
                        .param("criterion3Name", "方法")
                        .param("criterion3Points", "30")
                        .param("criterion3Description", "方法说明是否完整")
                        .param("criterion4Name", "表达")
                        .param("criterion4Points", "20")
                        .param("criterion4Description", "结构是否清晰"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher?published=2"));
    }

    @Test
    void studentSubmissionEntersTeacherQueue() throws Exception {
        String content = "摘要\n\n本研究围绕课程作业问题展开分析，并说明数据来源、研究方法与主要发现。";

        mockMvc.perform(post("/student/assignments/1/submissions")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/assignments/1"));

        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("第 3 次")))
                .andExpect(content().string(containsString("开始审核")));
    }

    @Test
    void confirmedIssueAppearsInTeacherSummary() throws Exception {
        mockMvc.perform(post("/api/submissions/1/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rubricName": "证据与引用",
                                  "evidenceQuote": "已有研究表明，短视频使用会影响注意力恢复。",
                                  "teacherNote": "请补充具体文献来源。"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 处")));

        org.assertj.core.api.Assertions.assertThat(
                eventRepository.countBySubmissionIdAndEventType(
                        1L, "SUGGESTION_ADOPTED"))
                .isEqualTo(1L);
    }
}
