package com.reviewdesk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OpenAI 兼容的模型 API 适配层，默认可用于 DeepSeek 等服务。
 *
 * <p>密钥来自服务端环境变量或教师加密配置，不返回浏览器。模型输出只作为建议，最终判断由教师完成。</p>
 */
@Service
public class AiSuggestionClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AiSuggestionClient.class);
    private static final int MAX_SOURCE_LENGTH = 24_000;

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
            @Value("${review-desk.ai.timeout-seconds:60}") long timeoutSeconds) {
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

    public String getModel() {
        return model;
    }

    public ProviderConfig defaultProvider() {
        return new ProviderConfig(apiKey, baseUrl, model, "SERVER", "ENVIRONMENT");
    }

    /**
     * 让模型只基于教师确认的证据润色备课稿。任何失败都返回空结果。
     */
    public Optional<String> enhanceLessonPlan(String assignmentTitle,
                                              String submissionFormat,
                                              String deterministicDraft,
                                              String evidenceSummary) {
        return enhanceLessonPlan(
                defaultProvider(),
                assignmentTitle,
                submissionFormat,
                deterministicDraft,
                evidenceSummary);
    }

    public Optional<String> enhanceLessonPlan(
            ProviderConfig config,
            String assignmentTitle,
            String submissionFormat,
            String deterministicDraft,
            String evidenceSummary) {
        if (!config.configured()) {
            return Optional.empty();
        }
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
        return chat(config, """
                你是教师的备课整理助手。只能依据用户提供的“教师确认证据”整理课堂建议，
                不得新增学生事实、不得推断学生是否独立完成作业、不得直接判定学术不端。
                输出应保留原有标题层级，包含课堂目标、共性问题、讲解步骤、追问和练习。
                使用简洁、可编辑的中文，不要输出 Markdown 代码围栏。
                """, source).filter(value -> !value.isBlank());
    }

    /**
     * 生成结构化的 AI 预审建议。返回内容不会直接修改教师反馈或最终评分。
     */
    public Optional<ReviewResult> reviewSubmission(
            String assignmentTitle,
            String submissionFormat,
            String submissionContent,
            String aiUsage,
            String aiTools,
            String aiScope,
            String sourceDeclaration,
            String collaborationDeclaration,
            String rubricSummary) {
        return reviewSubmission(
                defaultProvider(),
                assignmentTitle,
                submissionFormat,
                submissionContent,
                aiUsage,
                aiTools,
                aiScope,
                sourceDeclaration,
                collaborationDeclaration,
                rubricSummary);
    }

    public Optional<ReviewResult> reviewSubmission(
            ProviderConfig config,
            String assignmentTitle,
            String submissionFormat,
            String submissionContent,
            String aiUsage,
            String aiTools,
            String aiScope,
            String sourceDeclaration,
            String collaborationDeclaration,
            String rubricSummary) {
        if (!config.configured()) {
            return Optional.empty();
        }
        String source = """
                作业名称：%s
                提交形式：%s
                学生 AI 使用声明：%s
                AI 工具：%s
                AI 使用范围：%s
                来源声明：%s
                协作声明：%s

                评分量规：
                %s

                学生提交正文：
                %s
                """.formatted(
                assignmentTitle,
                submissionFormat,
                aiUsage,
                valueOrNone(aiTools),
                valueOrNone(aiScope),
                valueOrNone(sourceDeclaration),
                valueOrNone(collaborationDeclaration),
                limit(rubricSummary),
                limit(submissionContent));
        String raw = chatRequired(config, """
                你是教师的作业预审助手。你的结论只是候选意见，不能代替教师，也不能自动扣分。
                只分析用户提供的作业正文，不得捏造引用、数据或学生信息。
                只能依据给定评分量规，指出可核验的薄弱点，并引用学生原文作为证据。
                不要把“像 AI 写的”作为诚信结论；如 AI 声明不完整，只能提示教师核对。
                必须只返回合法 JSON，不要使用 Markdown 代码块，结构如下：
                {
                  "summary": "总体预审摘要，明确这是待教师核实建议",
                  "suggestions": [
                    {
                      "rubricName": "必须与量规名称完全一致",
                      "evidenceQuote": "学生原文中的简短证据",
                      "suggestion": "给教师核实的建议和修改方向",
                      "suggestedPoints": 0
                    }
                  ]
                }
                最多返回 6 条建议。suggestedPoints 必须位于该量规 0 到满分之间。
                """, source);
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            String summary = root.path("summary").asText("").strip();
            List<ReviewSuggestion> suggestions = new ArrayList<>();
            for (JsonNode item : root.path("suggestions")) {
                String rubricName = item.path("rubricName").asText("").strip();
                String evidence = item.path("evidenceQuote").asText("").strip();
                String suggestion = item.path("suggestion").asText("").strip();
                if (rubricName.isBlank() || evidence.isBlank() || suggestion.isBlank()) {
                    continue;
                }
                Integer points = item.has("suggestedPoints")
                        && item.path("suggestedPoints").canConvertToInt()
                        ? item.path("suggestedPoints").asInt() : null;
                suggestions.add(new ReviewSuggestion(
                        rubricName, evidence, suggestion, points));
            }
            if (summary.isBlank() || suggestions.isEmpty()) {
                throw new IllegalStateException(
                        "AI 模型没有返回可用建议，请重试或调整评分维度名称");
            }
            return Optional.of(new ReviewResult(summary, suggestions));
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            LOGGER.warn("AI 预审返回内容无法解析", exception);
            throw new IllegalStateException("AI 模型返回格式无法识别，请稍后重试");
        }
    }

    public boolean testConnection(ProviderConfig config) {
        if (!config.configured()) {
            return false;
        }
        return chat(config, "你是连接测试助手，只回复 OK。", "回复 OK")
                .map(content -> !content.isBlank())
                .orElse(false);
    }

    private Optional<String> chat(ProviderConfig config,
                                  String systemPrompt,
                                  String userContent) {
        try {
            return Optional.of(chatRequired(config, systemPrompt, userContent));
        } catch (RuntimeException exception) {
            LOGGER.warn("{}（{} / {}）",
                    exception.getMessage(), config.provider(), config.model());
            return Optional.empty();
        }
    }

    private String chatRequired(ProviderConfig config,
                                String systemPrompt,
                                String userContent) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", config.model());
            payload.put("temperature", 0.2);
            ArrayNode messages = payload.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userContent);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/chat/completions"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError(response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0)
                    .path("message").path("content").asText("").strip();
            if (content.isBlank()) {
                throw new IllegalStateException("AI 模型返回内容为空，请稍后重试");
            }
            return content;
        } catch (HttpTimeoutException exception) {
            throw new IllegalStateException("AI 模型响应超时，请检查网络或稍后重试");
        } catch (ConnectException exception) {
            throw new IllegalStateException(
                    "服务器无法连接模型服务，请检查接口地址和服务器网络");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 模型请求被中断，请稍后重试");
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("AI 接口地址格式不正确，请检查模型设置");
        } catch (Exception exception) {
            LOGGER.warn("AI 模型调用异常", exception);
            throw new IllegalStateException("AI 模型请求失败，请稍后重试");
        }
    }

    private IllegalStateException providerError(int statusCode) {
        String message = switch (statusCode) {
            case 401, 403 -> "模型鉴权失败：API Key 无效、已过期或没有接口权限";
            case 402 -> "模型账户余额不足，请充值或更换 API Key";
            case 429 -> "模型请求频率或额度已用尽，请稍后重试";
            case 400, 404, 422 -> "模型名称或接口地址不正确，请检查 AI 设置";
            default -> statusCode >= 500
                    ? "模型服务暂时不可用，请稍后重试"
                    : "模型请求失败，HTTP " + statusCode;
        };
        LOGGER.warn("AI 模型调用失败，HTTP {}：{}", statusCode, message);
        return new IllegalStateException(message);
    }

    private String extractJson(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        return start >= 0 && end > start ? value.substring(start, end + 1) : value;
    }

    private String valueOrNone(String value) {
        return value == null || value.isBlank() ? "未填写" : value;
    }

    private String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_SOURCE_LENGTH
                ? value : value.substring(0, MAX_SOURCE_LENGTH);
    }

    public record ReviewResult(String summary, List<ReviewSuggestion> suggestions) {
    }

    public record ReviewSuggestion(String rubricName, String evidenceQuote,
                                   String suggestion, Integer suggestedPoints) {
    }

    public record ProviderConfig(
            String apiKey,
            String baseUrl,
            String model,
            String source,
            String provider
    ) {
        public boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
