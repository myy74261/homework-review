package com.reviewdesk.web.spa;

import com.reviewdesk.config.DemoAccountProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端使用帮助与演示账号信息。
 */
@RestController
@RequestMapping("/api/app/teacher")
public class TeacherHelpController {

    private final DemoAccountProperties demoAccounts;
    private final boolean showDemoCredentials;

    public TeacherHelpController(
            DemoAccountProperties demoAccounts,
            @Value("${review-desk.ui.show-demo-credentials:true}")
            boolean showDemoCredentials) {
        this.demoAccounts = demoAccounts;
        this.showDemoCredentials = showDemoCredentials;
    }

    @GetMapping("/help")
    public HelpView help() {
        return new HelpView(
                showDemoCredentials,
                demoAccounts.teacherUsername(),
                demoAccounts.teacherPassword(),
                demoAccounts.studentUsername(),
                demoAccounts.studentPassword(),
                demoAccounts.adminUsername(),
                showDemoCredentials ? demoAccounts.adminPassword() : null,
                showDemoCredentials ? disciplineAccounts() : List.of());
    }

    private List<DemoCredential> disciplineAccounts() {
        return List.of(
                new DemoCredential("理科教师", "zhou", "Teach2026!", "普通物理实验课堂"),
                new DemoCredential("理科学生", "science_01", "Student2026!", "普通物理实验学生"),
                new DemoCredential("工科教师", "wujian", "Teach2026!", "软件工程实践课堂"),
                new DemoCredential("工科学生", "engineering_01", "Student2026!", "软件工程实践学生"),
                new DemoCredential("文科学生", "humanities_01", "Student2026!", "传播学方法课学生"));
    }

    public record HelpView(
            boolean showDemoCredentials,
            String teacherUsername,
            String teacherPassword,
            String studentUsername,
            String studentPassword,
            String adminUsername,
            String adminPassword,
            List<DemoCredential> disciplineAccounts) {
    }

    public record DemoCredential(
            String role,
            String username,
            String password,
            String description) {
    }
}
