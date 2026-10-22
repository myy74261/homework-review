package com.reviewdesk.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验证 P0 教师审核工作台可以正常渲染。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReviewWorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void workspaceRendersReviewContext() throws Exception {
        mockMvc.perform(get("/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("教师作业审核平台")))
                .andExpect(content().string(containsString("评分量规")))
                .andExpect(content().string(containsString("教师审核意见")));
    }
}

