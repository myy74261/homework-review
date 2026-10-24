package com.reviewdesk.web.spa;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 将 React 路由刷新请求交回 SPA 入口，由前端 Router 处理。
 */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/app",
            "/app/teacher",
            "/app/teacher/**",
            "/app/reviews/**",
            "/app/student",
            "/app/student/**"
    })
    public String app() {
        return "forward:/app/index.html";
    }
}
