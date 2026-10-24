package com.reviewdesk.web.spa;

import com.reviewdesk.domain.Account;
import com.reviewdesk.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
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

    private SpaViewModels.UserView toUser(Account account) {
        return new SpaViewModels.UserView(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.getStudentNumber());
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "请输入当前密码") String currentPassword,
            @NotBlank(message = "请输入新密码")
            @Size(min = 8, max = 72, message = "新密码需要 8–72 个字符")
            String newPassword) {
    }
}
