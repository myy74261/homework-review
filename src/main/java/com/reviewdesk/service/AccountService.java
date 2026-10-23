package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.repository.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师与学生账号的创建和当前用户解析。
 */
@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Account requireByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
    }

    public Account current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("请先登录");
        }
        return requireByUsername(authentication.getName());
    }

    @Transactional
    public Account register(String username, String password, String displayName,
                            Account.Role role, String studentNumber) {
        String cleanUsername = username == null ? "" : username.strip().toLowerCase();
        if (!cleanUsername.matches("[a-z0-9_]{3,30}")) {
            throw new IllegalArgumentException("账号需为 3–30 位小写字母、数字或下划线");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("密码至少需要 8 个字符");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("请填写姓名");
        }
        if (accountRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("该账号已经被使用");
        }
        if (role == Account.Role.STUDENT
                && (studentNumber == null || studentNumber.isBlank())) {
            throw new IllegalArgumentException("学生账号需要填写学号");
        }
        return accountRepository.save(new Account(
                cleanUsername,
                passwordEncoder.encode(password),
                displayName.strip(),
                role,
                role == Account.Role.STUDENT ? studentNumber.strip() : null));
    }
}

