package com.reviewdesk.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;

/**
 * 使用本地模拟 OpenAI 兼容接口验证结构化 AI 预审结果解析。
 */
class AiSuggestionClientTest {

    @Test
    void parsesStructuredReviewSuggestionsWithoutChangingTeacherDecision() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            String content = """
                    {
                      "summary": "发现一处引用证据不足，请教师核对。",
                      "suggestions": [
                        {
                          "rubricName": "证据与引用",
                          "evidenceQuote": "已有研究表明",
                          "suggestion": "请核对是否提供了可追溯文献。",
                          "suggestedPoints": 20
                        }
                      ]
                    }
                    """;
            var root = objectMapper.createObjectNode();
            root.putArray("choices")
                    .addObject()
                    .putObject("message")
                    .put("content", content);
            byte[] body = objectMapper.writeValueAsBytes(root);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            AiSuggestionClient client = new AiSuggestionClient(
                    objectMapper,
                    "test-key",
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-model",
                    5);
            var result = client.reviewSubmission(
                            "课程作业",
                            "DOCUMENT",
                            "已有研究表明，该结论成立。",
                            "NONE",
                            null,
                            null,
                            "课程材料",
                            "无",
                            "- 证据与引用（满分 25）：来源是否可追溯")
                    .orElseThrow();

            assertThat(result.summary()).contains("教师核对");
            assertThat(result.suggestions()).singleElement().satisfies(suggestion -> {
                assertThat(suggestion.rubricName()).isEqualTo("证据与引用");
                assertThat(suggestion.suggestedPoints()).isEqualTo(20);
            });
            assertThat(client.testConnection(client.defaultProvider())).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void missingApiKeyNeverReturnsFakeAiOutput() {
        AiSuggestionClient client = new AiSuggestionClient(
                new ObjectMapper(), "", "http://127.0.0.1:1", "test", 5);

        assertThat(client.isConfigured()).isFalse();
        assertThat(client.reviewSubmission(
                "课程作业", "DOCUMENT", "正文", "NONE",
                null, null, null, null, "量规"))
                .isEmpty();
    }
}
