package com.reviewdesk.config;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 首次启动时创建的演示账号配置。
 *
 * <p>生产环境应通过环境变量覆盖默认值，不应继续使用公开的演示密码。</p>
 */
@ConfigurationProperties(prefix = "review-desk.demo")
public record DemoAccountProperties(
        @DefaultValue("chen") String teacherUsername,
        @DefaultValue("Teach1234") String teacherPassword,
        @DefaultValue("陈老师") String teacherName,
        @DefaultValue("lin") String studentUsername,
        @DefaultValue("Student1234") String studentPassword,
        @DefaultValue("林清和") String studentName,
        @DefaultValue("20230116") String studentNumber
) {
}

