package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.repository.AccountRepository;
import com.reviewdesk.repository.ClassroomMembershipRepository;
import com.reviewdesk.repository.ClassroomCollaboratorRepository;
import com.reviewdesk.repository.ClassroomRepository;
import com.reviewdesk.repository.ClassroomRosterRepository;
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
    private final ClassroomRepository classroomRepository;
    private final ClassroomMembershipRepository membershipRepository;
    private final ClassroomRosterRepository rosterRepository;
    private final ClassroomCollaboratorRepository collaboratorRepository;

    public AccountService(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          ClassroomRepository classroomRepository,
                          ClassroomMembershipRepository membershipRepository,
                          ClassroomRosterRepository rosterRepository,
                          ClassroomCollaboratorRepository collaboratorRepository) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.classroomRepository = classroomRepository;
        this.membershipRepository = membershipRepository;
        this.rosterRepository = rosterRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    public Account requireByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
    }

    public Account requireById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
    }

    public Account current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("请先登录");
        }
        Account account = requireByUsername(authentication.getName());
        if (!account.isActive()) {
            throw new IllegalArgumentException("账号已注销");
        }
        return account;
    }

    @Transactional
    public void changePassword(String username, String currentPassword,
                               String newPassword) {
        Account account = requireByUsername(username);
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("请输入当前密码");
        }
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("新密码至少需要 8 个字符");
        }
        if (newPassword.length() > 72) {
            throw new IllegalArgumentException("新密码不能超过 72 个字符");
        }
        if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        account.changePasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public Account updateProfile(String username, String displayName,
                                 String email, String department,
                                 String professionalTitle) {
        Account account = requireByUsername(username);
        String cleanDisplayName = displayName == null ? "" : displayName.strip();
        if (cleanDisplayName.isEmpty() || cleanDisplayName.length() > 80) {
            throw new IllegalArgumentException("姓名需要为 1–80 个字符");
        }
        String cleanEmail = clean(email);
        if (cleanEmail != null
                && !cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        String cleanDepartment = account.isTeacher() ? clean(department) : null;
        String cleanTitle = account.isTeacher() ? clean(professionalTitle) : null;
        if ((cleanDepartment != null && cleanDepartment.length() > 100)
                || (cleanTitle != null && cleanTitle.length() > 100)) {
            throw new IllegalArgumentException("院系和职称信息不能超过 100 个字符");
        }
        account.updateProfile(
                cleanDisplayName,
                cleanEmail,
                cleanDepartment,
                cleanTitle);
        return account;
    }

    @Transactional
    public void deactivate(String username, String currentPassword) {
        Account account = requireByUsername(username);
        if (!account.isActive()) {
            throw new IllegalArgumentException("账号已经注销");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("请输入当前密码");
        }
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        if (account.isTeacher()
                && classroomRepository.existsByTeacherIdAndDissolvedAtIsNull(
                        account.getId())) {
            throw new IllegalStateException("请先处理并解散所有现有课堂，再注销教师账号");
        }
        if (account.isTeacher()) {
            collaboratorRepository.deleteByTeacherId(account.getId());
        }
        if (!account.isTeacher()) {
            rosterRepository.findByStudentAccountId(account.getId()).forEach(entry -> {
                entry.unlinkAccount();
                rosterRepository.save(entry);
            });
            membershipRepository.deleteByStudentId(account.getId());
        }
        account.deactivate();
        accountRepository.save(account);
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

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}

