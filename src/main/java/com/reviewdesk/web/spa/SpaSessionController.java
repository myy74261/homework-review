package com.reviewdesk.web.spa;

import com.reviewdesk.domain.Account;
import com.reviewdesk.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
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

    private SpaViewModels.UserView toUser(Account account) {
        return new SpaViewModels.UserView(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.getStudentNumber());
    }
}
