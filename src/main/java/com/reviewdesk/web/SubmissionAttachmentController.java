package com.reviewdesk.web;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.domain.SubmissionAttachment;
import com.reviewdesk.service.AccessControlService;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.SubmissionAttachmentService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 在权限校验后提供学生提交附件下载。
 */
@Controller
public class SubmissionAttachmentController {

    private final AccountService accountService;
    private final ReviewService reviewService;
    private final AccessControlService accessControlService;
    private final SubmissionAttachmentService attachmentService;

    public SubmissionAttachmentController(
            AccountService accountService,
            ReviewService reviewService,
            AccessControlService accessControlService,
            SubmissionAttachmentService attachmentService) {
        this.accountService = accountService;
        this.reviewService = reviewService;
        this.accessControlService = accessControlService;
        this.attachmentService = attachmentService;
    }

    @GetMapping("/files/submissions/{submissionId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> download(
            @PathVariable long submissionId,
            @PathVariable long attachmentId,
            Authentication authentication) throws IOException {
        Account account = accountService.current(authentication);
        Submission submission = reviewService.requireSubmission(submissionId);
        if (account.isTeacher()) {
            accessControlService.requireTeacherSubmission(
                    account.getUsername(), submissionId);
        } else if (!account.getId().equals(submission.getStudentAccountId())) {
            throw new SecurityException("无权访问该附件");
        }

        SubmissionAttachmentService.StoredAttachment stored =
                attachmentService.load(submissionId, attachmentId);
        SubmissionAttachment attachment = stored.attachment();
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(attachment.getContentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(attachment.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(attachment.getOriginalName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(stored.resource());
    }
}
