package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.AiProviderSetting;
import com.reviewdesk.repository.AiProviderSettingRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师个人 AI 服务配置、脱敏展示和连接检测。
 */
@Service
@Transactional(readOnly = true)
public class AiSettingsService {

    private static final Map<String, ProviderDefinition> PROVIDERS = providers();

    private final AccountService accountService;
    private final AiProviderSettingRepository settingRepository;
    private final AiSecretCipher secretCipher;
    private final AiSuggestionClient aiSuggestionClient;

    public AiSettingsService(
            AccountService accountService,
            AiProviderSettingRepository settingRepository,
            AiSecretCipher secretCipher,
            AiSuggestionClient aiSuggestionClient) {
        this.accountService = accountService;
        this.settingRepository = settingRepository;
        this.secretCipher = secretCipher;
        this.aiSuggestionClient = aiSuggestionClient;
    }

    public SettingsView view(String username) {
        Account teacher = requireTeacher(username);
        AiProviderSetting setting = settingRepository
                .findByTeacherId(teacher.getId())
                .orElse(null);
        if (setting != null) {
            return new SettingsView(
                    setting.isActive(),
                    "TEACHER",
                    setting.getProvider(),
                    providerName(setting.getProvider()),
                    setting.getBaseUrl(),
                    setting.getModel(),
                    "••••" + setting.getKeySuffix(),
                    setting.getUpdatedAt(),
                    providerViews());
        }
        AiSuggestionClient.ProviderConfig environment =
                aiSuggestionClient.defaultProvider();
        return new SettingsView(
                environment.configured(),
                "SERVER",
                "ENVIRONMENT",
                "服务器环境变量",
                environment.baseUrl(),
                environment.model(),
                environment.configured() ? mask(environment.apiKey()) : "",
                null,
                providerViews());
    }

    @Transactional
    public SettingsView save(String username, String providerCode,
                             String model, String apiKey) {
        Account teacher = requireTeacher(username);
        ProviderDefinition provider = requireProvider(providerCode);
        String cleanModel = cleanModel(model, provider.defaultModel());
        String cleanKey = apiKey == null ? "" : apiKey.strip();
        AiProviderSetting existing = settingRepository
                .findByTeacherId(teacher.getId())
                .orElse(null);
        String encrypted;
        String suffix;
        if (!cleanKey.isBlank()) {
            encrypted = secretCipher.encrypt(cleanKey);
            suffix = suffix(cleanKey);
        } else if (existing != null) {
            encrypted = existing.getEncryptedApiKey();
            suffix = existing.getKeySuffix();
        } else {
            throw new IllegalArgumentException("首次接入模型时需要填写 API Key");
        }

        AiProviderSetting setting;
        if (existing == null) {
            setting = new AiProviderSetting(
                    teacher.getId(),
                    provider.code(),
                    provider.baseUrl(),
                    cleanModel,
                    encrypted,
                    suffix,
                    true);
        } else {
            existing.update(
                    provider.code(),
                    provider.baseUrl(),
                    cleanModel,
                    encrypted,
                    suffix,
                    true);
            setting = existing;
        }
        settingRepository.save(setting);
        return view(username);
    }

    public AiSuggestionClient.ProviderConfig resolve(String username) {
        Account teacher = requireTeacher(username);
        return settingRepository.findByTeacherId(teacher.getId())
                .filter(AiProviderSetting::isActive)
                .map(setting -> new AiSuggestionClient.ProviderConfig(
                        secretCipher.decrypt(setting.getEncryptedApiKey()),
                        setting.getBaseUrl(),
                        setting.getModel(),
                        "TEACHER",
                        setting.getProvider()))
                .orElseGet(aiSuggestionClient::defaultProvider);
    }

    public boolean isConfigured(String username) {
        return resolve(username).configured();
    }

    public ConnectionResult test(String username, String providerCode,
                                 String model, String apiKey) {
        String cleanProvider = providerCode == null || providerCode.isBlank()
                ? null : providerCode;
        AiSuggestionClient.ProviderConfig saved = resolve(username);
        ProviderDefinition provider = cleanProvider == null
                ? requireProvider(saved.provider()) : requireProvider(cleanProvider);
        String cleanKey = apiKey == null || apiKey.isBlank()
                ? saved.apiKey() : apiKey.strip();
        String cleanModel = cleanModel(
                model == null || model.isBlank() ? saved.model() : model,
                provider.defaultModel());
        AiSuggestionClient.ProviderConfig config =
                new AiSuggestionClient.ProviderConfig(
                        cleanKey, provider.baseUrl(), cleanModel, "TEST", provider.code());
        if (!config.configured()) {
            return new ConnectionResult(false, "请填写 API Key");
        }
        return aiSuggestionClient.testConnection(config)
                ? new ConnectionResult(true, "连接成功，模型已返回响应")
                : new ConnectionResult(false, "连接失败，请检查密钥、模型名称或账户额度");
    }

    private Account requireTeacher(String username) {
        Account account = accountService.requireByUsername(username);
        if (!account.isTeacher()) {
            throw new SecurityException("只有教师可以配置 AI 服务");
        }
        return account;
    }

    private ProviderDefinition requireProvider(String code) {
        ProviderDefinition provider = PROVIDERS.get(code);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的模型服务");
        }
        return provider;
    }

    private String cleanModel(String model, String fallback) {
        String value = model == null || model.isBlank() ? fallback : model.strip();
        if (!value.matches("[A-Za-z0-9._:/-]{1,100}")) {
            throw new IllegalArgumentException("模型名称格式不正确");
        }
        return value;
    }

    private String providerName(String code) {
        ProviderDefinition provider = PROVIDERS.get(code);
        return provider == null ? code : provider.name();
    }

    private List<ProviderView> providerViews() {
        return PROVIDERS.values().stream()
                .map(provider -> new ProviderView(
                        provider.code(),
                        provider.name(),
                        provider.baseUrl(),
                        provider.defaultModel()))
                .toList();
    }

    private String mask(String apiKey) {
        return apiKey == null || apiKey.length() < 4 ? "••••" : "••••" + suffix(apiKey);
    }

    private String suffix(String apiKey) {
        return apiKey.length() <= 4 ? apiKey : apiKey.substring(apiKey.length() - 4);
    }

    private static Map<String, ProviderDefinition> providers() {
        Map<String, ProviderDefinition> values = new LinkedHashMap<>();
        values.put("DEEPSEEK", new ProviderDefinition(
                "DEEPSEEK", "DeepSeek",
                "https://api.deepseek.com", "deepseek-chat"));
        values.put("OPENAI", new ProviderDefinition(
                "OPENAI", "OpenAI",
                "https://api.openai.com/v1", "gpt-4.1-mini"));
        values.put("QWEN", new ProviderDefinition(
                "QWEN", "通义千问",
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"));
        values.put("ZHIPU", new ProviderDefinition(
                "ZHIPU", "智谱 AI",
                "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash"));
        values.put("MOONSHOT", new ProviderDefinition(
                "MOONSHOT", "Moonshot",
                "https://api.moonshot.cn/v1", "moonshot-v1-8k"));
        return Collections.unmodifiableMap(values);
    }

    private record ProviderDefinition(
            String code, String name, String baseUrl, String defaultModel) {
    }

    public record ProviderView(
            String code, String name, String baseUrl, String defaultModel) {
    }

    public record SettingsView(
            boolean configured,
            String source,
            String provider,
            String providerName,
            String baseUrl,
            String model,
            String maskedKey,
            Instant updatedAt,
            List<ProviderView> providers
    ) {
    }

    public record ConnectionResult(boolean success, String message) {
    }
}
