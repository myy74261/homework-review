package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 教师个人配置的 OpenAI 兼容模型服务。
 *
 * <p>API Key 只保存加密后的密文，不向浏览器返回明文。</p>
 */
@Entity
@Table(name = "ai_provider_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_provider_settings_teacher",
                columnNames = "teacher_id"))
public class AiProviderSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 300)
    private String baseUrl;

    @Column(nullable = false, length = 100)
    private String model;

    @Lob
    @Column(nullable = false)
    private String encryptedApiKey;

    @Column(nullable = false, length = 12)
    private String keySuffix;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AiProviderSetting() {
        // JPA 需要的无参构造方法
    }

    public AiProviderSetting(Long teacherId, String provider, String baseUrl,
                             String model, String encryptedApiKey,
                             String keySuffix, boolean active) {
        this.teacherId = teacherId;
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.model = model;
        this.encryptedApiKey = encryptedApiKey;
        this.keySuffix = keySuffix;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public void update(String provider, String baseUrl, String model,
                       String encryptedApiKey, String keySuffix, boolean active) {
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.model = model;
        this.encryptedApiKey = encryptedApiKey;
        this.keySuffix = keySuffix;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public String getProvider() {
        return provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }

    public String getKeySuffix() {
        return keySuffix;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
