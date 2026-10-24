package com.reviewdesk.web;

import com.reviewdesk.service.IntegrityService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 教师记录和确认学术诚信问题。
 */
@Controller
public class IntegrityFlagController {

    private final IntegrityService integrityService;

    public IntegrityFlagController(IntegrityService integrityService) {
        this.integrityService = integrityService;
    }

    @PostMapping("/teacher/submissions/{submissionId}/integrity-flags")
    public String create(@PathVariable long submissionId,
                         @RequestParam String category,
                         @RequestParam String severity,
                         @RequestParam String evidence,
                         @RequestParam String reason,
                         Authentication authentication) {
        integrityService.create(
                authentication.getName(), submissionId,
                category, severity, evidence, reason);
        return "redirect:/reviews/" + submissionId + "#integrity";
    }

    @PostMapping("/teacher/integrity-flags/{flagId}/resolve")
    public String resolve(@PathVariable long flagId,
                          @RequestParam String status,
                          @RequestParam long submissionId,
                          Authentication authentication) {
        integrityService.resolve(authentication.getName(), flagId, status);
        return "redirect:/reviews/" + submissionId + "#integrity";
    }
}
