package com.reviewdesk.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 将容器错误转换为面向教师和学生的中文页面，避免暴露 Whitelabel 技术信息。
 */
@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        Object statusValue = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusValue instanceof Integer value ? value : 500;
        model.addAttribute("statusCode", status);
        model.addAttribute("errorTitle", switch (status) {
            case 403 -> "没有访问权限";
            case 404 -> "页面不存在";
            case 405 -> "操作方式不正确";
            default -> "页面暂时无法打开";
        });
        model.addAttribute("errorMessage", switch (status) {
            case 403 -> "当前账号不能访问这个页面，请返回自己的工作台。";
            case 404 -> "这个地址可能已经失效，请返回课堂重新进入。";
            case 405 -> "提交入口不能直接打开。请返回作业页重新填写并提交。";
            default -> "系统处理时遇到问题，请稍后重试或返回课堂。";
        });
        return "error";
    }
}
