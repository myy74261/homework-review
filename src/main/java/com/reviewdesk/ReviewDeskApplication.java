package com.reviewdesk;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;

/**
 * 教师作业审核平台的应用入口。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ReviewDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewDeskApplication.class, args);
    }
}

