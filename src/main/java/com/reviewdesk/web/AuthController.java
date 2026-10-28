package com.reviewdesk.web;

import com.reviewdesk.domain.Account;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.ClassroomService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 登录与账号注册页面。
 */
@Controller
public class AuthController {

    private final AccountService accountService;
    private final ClassroomService classroomService;

    public AuthController(AccountService accountService,
                          ClassroomService classroomService) {
        this.accountService = accountService;
        this.classroomService = classroomService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Account account = accountService.requireByUsername(authentication.getName());
            return "redirect:" + (account.isTeacher()
                    ? "/app/teacher" : "/app/student");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String displayName,
                           @RequestParam Account.Role role,
                           @RequestParam(required = false) String studentNumber,
                           Model model) {
        try {
            Account account = accountService.register(
                    username, password, displayName, role, studentNumber);
            classroomService.linkStudentToRosters(account);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("displayName", displayName);
            model.addAttribute("studentNumber", studentNumber);
            model.addAttribute("selectedRole", role);
            return "register";
        }
    }
}
