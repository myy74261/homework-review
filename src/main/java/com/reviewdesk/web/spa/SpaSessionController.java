package com.reviewdesk.web.spa;

import com.reviewdesk.domain.Account;
import com.reviewdesk.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * React 应用会话与 CSRF 初始化接口。
 */
@RestController
@RequestMapping("/api/app")
public class SpaSessionController {

    private final AccountService accountService;

    public SpaSessionController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/session")
    public SpaViewModels.SessionView session(Authentication authentication,
                                              CsrfToken csrfToken) {
        Account account = accountService.current(authentication);
        return new SpaViewModels.SessionView(
                toUser(account),
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                account.isTeacher() ? "/app/teacher" : "/app/student");
    }

    @PutMapping("/account/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/account/profile")
    public SpaViewModels.UserView updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        Account account = accountService.updateProfile(
                authentication.getName(),
                request.displayName(),
                request.email(),
                request.department(),
                request.professionalTitle());
        return toUser(account);
    }

    @PostMapping("/account/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/account/deactivate")
    public ResponseEntity<Void> deactivate(
            Authentication authentication,
            @Valid @RequestBody DeactivateAccountRequest request) {
        accountService.deactivate(
                authentication.getName(), request.currentPassword());
        return ResponseEntity.noContent().build();
    }

    private SpaViewModels.UserView toUser(Account account) {
        return new SpaViewModels.UserView(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.getStudentNumber(),
                account.getEmail(),
                account.getDepartment(),
                account.getProfessionalTitle());
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "请输入当前密码") String currentPassword,
            @NotBlank(message = "请输入新密码")
            @Size(min = 8, max = 72, message = "新密码需要 8–72 个字符")
            String newPassword) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "请填写姓名")
            @Size(max = 80, message = "姓名不能超过 80 个字符")
            String displayName,
            @Email(message = "邮箱格式不正确")
            @Size(max = 120, message = "邮箱不能超过 120 个字符")
            String email,
            @Size(max = 100, message = "院系不能超过 100 个字符")
            String department,
            @Size(max = 100, message = "职称/职务不能超过 100 个字符")
            String professionalTitle) {
    }

    public record DeactivateAccountRequest(
            @NotBlank(message = "请输入当前密码") String currentPassword) {
    }
}
