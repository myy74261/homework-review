package com.reviewdesk.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证 P3 可解释备课建议的生成、编辑和重新生成。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LessonPlanControllerTest {

    private static final String PLAN_URL = "/api/assignments/1/lesson-plan";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void lessonPlanPageRendersWithEvidence() throws Exception {
        mockMvc.perform(get("/teacher/assignments/1/lesson-plan"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("可解释备课建议")))
                .andExpect(content().string(containsString("证据链")))
                .andExpect(content().string(containsString("典型错例与证据")))
                .andExpect(content().string(containsString("请为“已有研究表明”补充具体文献来源")));
    }

    @Test
    void generatedPlanContainsEvidenceAndIssueCount() throws Exception {
        mockMvc.perform(get(PLAN_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceCount").value(2))
                .andExpect(jsonPath("$.content", containsString("共性问题")))
                .andExpect(jsonPath("$.content", containsString("教师判断")))
                .andExpect(jsonPath("$.content",
                        containsString("不评价学生是否独立完成作业")));
    }

    @Test
    void teacherCanEditAndSavePlan() throws Exception {
        JsonNode original = readPlan();
        long revision = original.get("revision").asLong();

        mockMvc.perform(put(PLAN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "教师修改后的课堂讲解",
                                "content", "一、课堂目标\n\n用典型错例讲解证据链。",
                                "baseRevision", revision))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value((int) revision + 1))
                .andExpect(jsonPath("$.title").value("教师修改后的课堂讲解"));

        mockMvc.perform(get(PLAN_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("典型错例")));
    }

    @Test
    void regenerateUsesLatestTeacherConfirmedIssues() throws Exception {
        readPlan();
        mockMvc.perform(post(PLAN_URL + "/regenerate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueGroups[0].rubricName").value("证据与引用"))
                .andExpect(jsonPath("$.content", containsString("证据与引用")))
                .andExpect(jsonPath("$.content", containsString("方法与数据")));
    }

    @Test
    void teacherCanExportWordCompatibleMaterial() throws Exception {
        mockMvc.perform(get("/teacher/assignments/1/lesson-plan/export.doc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/msword"))
                .andExpect(content().string(containsString("课堂讲解建议")))
                .andExpect(content().string(containsString("附：问题证据")));
    }

    private JsonNode readPlan() throws Exception {
        String body = mockMvc.perform(get(PLAN_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }
}
