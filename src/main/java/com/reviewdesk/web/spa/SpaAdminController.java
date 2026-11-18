package com.reviewdesk.web.spa;

import com.reviewdesk.service.TeacherApprovalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * React 平台管理员端接口。
 */
@RestController
@RequestMapping("/api/app/admin")
public class SpaAdminController {

    private final TeacherApprovalService approvalService;

    public SpaAdminController(TeacherApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/teachers")
    public TeacherApprovalService.ApprovalOverview teachers(
            Authentication authentication,
            @RequestParam(defaultValue = "PENDING") String status) {
        return approvalService.overview(authentication.getName(), status);
    }

    @PostMapping("/teachers/{teacherId}/review")
    public TeacherApprovalService.TeacherApplicationView review(
            @PathVariable long teacherId,
            Authentication authentication,
            @Valid @RequestBody ReviewTeacherRequest request) {
        return approvalService.review(
                authentication.getName(),
                teacherId,
                request.status(),
                request.note());
    }

    public record ReviewTeacherRequest(
            @NotBlank(message = "请选择审核结果") String status,
            @Size(max = 300, message = "审核说明不能超过 300 个字符") String note) {
    }
}
