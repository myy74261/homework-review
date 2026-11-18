package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.repository.AccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台管理员审核教师注册申请。
 */
@Service
@Transactional(readOnly = true)
public class TeacherApprovalService {

    private static final List<String> VALID_STATUSES =
            List.of("PENDING", "APPROVED", "REJECTED");

    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public TeacherApprovalService(
            AccountRepository accountRepository,
            AccountService accountService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    public ApprovalOverview overview(String adminUsername, String status) {
        requireAdmin(adminUsername);
        String normalized = status == null || status.isBlank()
                ? "PENDING" : status.strip().toUpperCase(Locale.ROOT);
        List<Account> teachers = "ALL".equals(normalized)
                ? accountRepository.findByRoleOrderByCreatedAtDesc("TEACHER")
                : accountRepository.findByRoleAndApprovalStatusOrderByCreatedAtDesc(
                        "TEACHER", requireStatus(normalized));
        List<Account> allTeachers =
                accountRepository.findByRoleOrderByCreatedAtDesc("TEACHER");
        return new ApprovalOverview(
                count(allTeachers, "PENDING"),
                count(allTeachers, "APPROVED"),
                count(allTeachers, "REJECTED"),
                teachers.stream().map(this::toView).toList());
    }

    @Transactional
    public TeacherApplicationView review(
            String adminUsername, long teacherId, String status, String note) {
        Account admin = requireAdmin(adminUsername);
        String normalized = requireStatus(
                status == null ? "" : status.strip().toUpperCase(Locale.ROOT));
        if ("PENDING".equals(normalized)) {
            throw new IllegalArgumentException("审核结果只能是通过或拒绝");
        }
        Account teacher = accountRepository.findById(teacherId)
                .filter(Account::isTeacher)
                .orElseThrow(() -> new IllegalArgumentException("教师申请不存在"));
        String cleanNote = note == null ? "" : note.strip();
        if ("REJECTED".equals(normalized) && cleanNote.isBlank()) {
            throw new IllegalArgumentException("拒绝申请时请填写原因");
        }
        if ("APPROVED".equals(normalized)) {
            teacher.approve(admin.getId(), cleanNote.isBlank() ? "审核通过" : cleanNote);
        } else {
            teacher.reject(admin.getId(), cleanNote);
        }
        return toView(accountRepository.save(teacher));
    }

    private Account requireAdmin(String username) {
        Account account = accountService.requireByUsername(username);
        if (!account.isAdmin() || !account.isActive()) {
            throw new SecurityException("需要平台管理员权限");
        }
        return account;
    }

    private String requireStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("不支持的审核状态");
        }
        return status;
    }

    private long count(List<Account> accounts, String status) {
        return accounts.stream()
                .filter(account -> status.equals(account.getApprovalStatus()))
                .count();
    }

    private TeacherApplicationView toView(Account teacher) {
        return new TeacherApplicationView(
                teacher.getId(),
                teacher.getUsername(),
                teacher.getDisplayName(),
                teacher.getEmail(),
                teacher.getDepartment(),
                teacher.getProfessionalTitle(),
                teacher.getApprovalStatus(),
                teacher.getReviewNote(),
                teacher.getCreatedAt(),
                teacher.getReviewedAt());
    }

    public record ApprovalOverview(
            long pendingCount,
            long approvedCount,
            long rejectedCount,
            List<TeacherApplicationView> teachers) {
    }

    public record TeacherApplicationView(
            long id,
            String username,
            String displayName,
            String email,
            String department,
            String professionalTitle,
            String approvalStatus,
            String reviewNote,
            Instant createdAt,
            Instant reviewedAt) {
    }
}
