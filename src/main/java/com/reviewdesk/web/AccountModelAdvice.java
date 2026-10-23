package com.reviewdesk.web;

import com.reviewdesk.domain.Account;
import com.reviewdesk.repository.AccountRepository;
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

    public AccountModelAdvice(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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
}

