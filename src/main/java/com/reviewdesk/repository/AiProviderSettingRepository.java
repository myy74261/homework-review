package com.reviewdesk.repository;

import com.reviewdesk.domain.AiProviderSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 教师个人 AI 服务配置的数据访问接口。
 */
public interface AiProviderSettingRepository
        extends JpaRepository<AiProviderSetting, Long> {

    Optional<AiProviderSetting> findByTeacherId(Long teacherId);
}
