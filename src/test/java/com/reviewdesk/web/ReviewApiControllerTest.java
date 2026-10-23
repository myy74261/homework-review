package com.reviewdesk.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审核意见保存、反馈版本和恢复的接口测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewApiControllerTest {

    private static final String REVIEW_URL = "/api/submissions/1/review";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seededReviewIsAvailable() throws Exception {
        mockMvc.perform(get(REVIEW_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", containsString("审核意见")))
                .andExpect(jsonPath("$.content", containsString("总体判断")))
                .andExpect(jsonPath("$.revision").isNumber())
                .andExpect(jsonPath("$.versionCount").value(1));
    }

    @Test
    void saveIncrementsRevision() throws Exception {
        long revision = currentRevision();

        mockMvc.perform(put(REVIEW_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest("补充后的审核意见", "新增审核反馈", revision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value((int) revision + 1))
                .andExpect(jsonPath("$.title").value("补充后的审核意见"))
                .andExpect(jsonPath("$.content").value("新增审核反馈"));
    }

    @Test
    void staleRevisionIsRejected() throws Exception {
        long revision = currentRevision();

        mockMvc.perform(put(REVIEW_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest("审核意见", "反馈", revision + 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_REVISION"))
                .andExpect(jsonPath("$.currentRevision").value((int) revision));
    }

    @Test
    void versionCanBeCreatedListedAndDiffed() throws Exception {
        long versionId = createVersion();
        long revision = currentRevision();

        mockMvc.perform(put(REVIEW_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest("修订后的审核意见", "第一段。\n\n新增反馈。", revision)))
                .andExpect(status().isOk());

        mockMvc.perform(get(REVIEW_URL + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(REVIEW_URL + "/versions/" + versionId + "/diff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added", greaterThan(0)));
    }

    @Test
    void restoreBringsBackVersionContent() throws Exception {
        String original = readReview().get("content").asText();
        long versionId = createVersion();
        long revision = currentRevision();

        mockMvc.perform(put(REVIEW_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest("临时审核意见", "临时反馈", revision)))
                .andExpect(status().isOk());

        mockMvc.perform(post(REVIEW_URL + "/versions/" + versionId + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(original));

        mockMvc.perform(get(REVIEW_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(original));
    }

    @Test
    void completingReviewPublishesFeedbackToStudent() throws Exception {
        mockMvc.perform(post(REVIEW_URL + "/complete"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已审核")));

        mockMvc.perform(get("/student/assignments/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("教师最终反馈")))
                .andExpect(content().string(containsString("总体判断")));
    }

    private String saveRequest(String title, String content, long baseRevision)
            throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "content", content,
                "baseRevision", baseRevision));
    }

    private JsonNode readReview() throws Exception {
        String body = mockMvc.perform(get(REVIEW_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    private long currentRevision() throws Exception {
        return readReview().get("revision").asLong();
    }

    private long createVersion() throws Exception {
        MvcResult result = mockMvc.perform(post(REVIEW_URL + "/versions"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(
                        result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("id")
                .asLong();
    }
}
