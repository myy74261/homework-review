package com.reviewdesk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OpenAI 兼容的模型 API 适配层，默认可用于 DeepSeek 等服务。
 *
 * <p>密钥只保存在服务端环境变量中。模型失败时调用方必须回退到确定性规则。</p>
 */
@Service
public class AiSuggestionClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AiSuggestionClient.class);
    private static final int MAX_SOURCE_LENGTH = 18_000;

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;

    public AiSuggestionClient(
            ObjectMapper objectMapper,
            @Value("${review-desk.ai.api-key:}") String apiKey,
            @Value("${review-desk.ai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${review-desk.ai.model:deepseek-chat}") String model,
            @Value("${review-desk.ai.timeout-seconds:30}") long timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.baseUrl = baseUrl == null
                ? "https://api.deepseek.com" : baseUrl.replaceAll("/+$", "");
        this.model = model == null || model.isBlank() ? "deepseek-chat" : model.strip();
        this.timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * 让模型只基于教师确认的证据润色备课稿。任何失败都返回空结果。
     */
    public Optional<String> enhanceLessonPlan(String assignmentTitle,
                                              String submissionFormat,
                                              String deterministicDraft,
                                              String evidenceSummary) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", model);
            payload.put("temperature", 0.25);
            ArrayNode messages = payload.putArray("messages");
            messages.addObject()
                    .put("role", "system")
                    .put("content", """
                            你是教师的备课整理助手。只能依据用户提供的“教师确认证据”整理课堂建议，
                            不得新增学生事实、不得推断学生是否独立完成作业、不得直接判定学术不端。
                            输出应保留原有标题层级，包含课堂目标、共性问题、讲解步骤、追问和练习。
                            使用简洁、可编辑的中文，不要输出 Markdown 代码围栏。
                            """);
            String source = """
                    作业：%s
                    提交形式：%s

                    当前规则稿：
                    %s

                    教师确认证据：
                    %s
                    """.formatted(
                    assignmentTitle,
                    submissionFormat,
                    limit(deterministicDraft),
                    limit(evidenceSummary));
            messages.addObject()
                    .put("role", "user")
                    .put("content", source);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("AI 备课增强调用失败，HTTP {}", response.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0)
                    .path("message").path("content").asText("").strip();
            return content.isBlank() ? Optional.empty() : Optional.of(content);
        } catch (Exception exception) {
            LOGGER.warn("AI 备课增强调用异常，已回退到规则稿", exception);
            return Optional.empty();
        }
    }

    private String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_SOURCE_LENGTH
                ? value : value.substring(0, MAX_SOURCE_LENGTH);
    }
}
