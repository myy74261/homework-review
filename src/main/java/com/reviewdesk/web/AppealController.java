package com.reviewdesk.web;

import com.reviewdesk.domain.Account;
import com.reviewdesk.service.AppealService;
import com.reviewdesk.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 学生申诉提交和教师复核接口。
 */
@Controller
public class AppealController {

    private final AccountService accountService;
    private final AppealService appealService;

    public AppealController(AccountService accountService,
                            AppealService appealService) {
        this.accountService = accountService;
        this.appealService = appealService;
    }

    @PostMapping("/student/submissions/{submissionId}/appeals")
    public String submit(@PathVariable long submissionId,
                         @RequestParam String reason,
                         @RequestParam(required = false) String requestedResolution,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        Account student = accountService.current(authentication);
        try {
            appealService.submit(
                    submissionId, student.getId(), reason, requestedResolution);
            redirectAttributes.addFlashAttribute("successMessage", "申诉已提交，等待教师复核。");
        } catch (IllegalArgumentException | SecurityException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/assignments/"
                + appealService.assignmentIdFor(submissionId) + "#appeals";
    }

    @PostMapping("/teacher/appeals/{appealId}/respond")
    public String respond(@PathVariable long appealId,
                          @RequestParam String status,
                          @RequestParam String teacherResponse,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        var appeal = appealService.respond(
                authentication.getName(), appealId, status, teacherResponse);
        redirectAttributes.addFlashAttribute("appealMessage", "申诉复核结果已保存。");
        return "redirect:/teacher#appeal-" + appeal.getId();
    }
}
