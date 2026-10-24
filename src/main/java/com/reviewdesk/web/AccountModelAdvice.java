package com.reviewdesk.web;

import com.reviewdesk.config.DemoAccountProperties;
import com.reviewdesk.domain.Account;
import com.reviewdesk.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 向所有页面提供当前登录账号，避免模板写死身份。
 */
@ControllerAdvice
public class AccountModelAdvice {

    private final AccountRepository accountRepository;
    private final DemoAccountProperties demoAccounts;
    private final boolean showDemoCredentials;

    public AccountModelAdvice(
            AccountRepository accountRepository,
            DemoAccountProperties demoAccounts,
            @Value("${review-desk.ui.show-demo-credentials:true}")
            boolean showDemoCredentials) {
        this.accountRepository = accountRepository;
        this.demoAccounts = demoAccounts;
        this.showDemoCredentials = showDemoCredentials;
    }

    @ModelAttribute("currentAccount")
    public Account currentAccount(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }
        return accountRepository.findByUsername(authentication.getName()).orElse(null);
    }

    @ModelAttribute("showDemoCredentials")
    public boolean showDemoCredentials() {
        return showDemoCredentials;
    }

    @ModelAttribute("demoTeacherUsername")
    public String demoTeacherUsername() {
        return showDemoCredentials ? demoAccounts.teacherUsername() : null;
    }

    @ModelAttribute("demoTeacherPassword")
    public String demoTeacherPassword() {
        return showDemoCredentials ? demoAccounts.teacherPassword() : null;
    }

    @ModelAttribute("demoStudentUsername")
    public String demoStudentUsername() {
        return showDemoCredentials ? demoAccounts.studentUsername() : null;
    }

    @ModelAttribute("demoStudentPassword")
    public String demoStudentPassword() {
        return showDemoCredentials ? demoAccounts.studentPassword() : null;
    }
}

