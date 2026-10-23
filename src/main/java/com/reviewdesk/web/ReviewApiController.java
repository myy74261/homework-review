package com.reviewdesk.web;

import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.ReviewVersion;
import com.reviewdesk.service.ReviewDiffService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.AccessControlService;
import com.reviewdesk.web.api.DiffResponse;
import com.reviewdesk.web.api.ReviewResponse;
import com.reviewdesk.web.api.SaveReviewRequest;
import com.reviewdesk.web.api.VersionDetailResponse;
import com.reviewdesk.web.api.VersionSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.Authentication;

/**
 * 教师审核意见保存与反馈版本管理的 REST 接口。
 */
@RestController
@RequestMapping("/api/submissions/{submissionId}/review")
public class ReviewApiController {

    private final ReviewService reviewService;
    private final ReviewDiffService diffService;
    private final AccessControlService accessControlService;

    public ReviewApiController(ReviewService reviewService,
                               ReviewDiffService diffService,
                               AccessControlService accessControlService) {
        this.reviewService = reviewService;
        this.diffService = diffService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public ReviewResponse getReview(@PathVariable long submissionId,
                                    Authentication authentication) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        ReviewDraft draft = reviewService.requireDraft(submissionId);
        return ReviewResponse.from(draft, reviewService.countVersions(submissionId));
    }

    @PutMapping
    public ReviewResponse saveReview(@PathVariable long submissionId,
                                     Authentication authentication,
                                     @Valid @RequestBody SaveReviewRequest request) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        ReviewDraft draft = reviewService.save(
                submissionId, request.title(), request.content(), request.baseRevision());
        return ReviewResponse.from(draft, reviewService.countVersions(submissionId));
    }

    @PostMapping("/versions")
    public VersionSummaryResponse createVersion(@PathVariable long submissionId,
                                                Authentication authentication) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        ReviewVersion version = reviewService.createVersion(submissionId, "MANUAL");
        return VersionSummaryResponse.from(version);
    }

    @GetMapping("/versions")
    public List<VersionSummaryResponse> listVersions(@PathVariable long submissionId,
                                                     Authentication authentication) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        return reviewService.listVersions(submissionId).stream()
                .map(VersionSummaryResponse::from)
                .toList();
    }

    @GetMapping("/versions/{versionId}")
    public VersionDetailResponse getVersion(@PathVariable long submissionId,
                                            @PathVariable long versionId,
                                            Authentication authentication) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        return VersionDetailResponse.from(
                reviewService.requireVersion(submissionId, versionId));
    }

    /**
     * 比较历史反馈与当前反馈，ADD 表示当前反馈新增的行。
     */
    @GetMapping("/versions/{versionId}/diff")
    public DiffResponse diffVersion(@PathVariable long submissionId,
                                    @PathVariable long versionId,
                                    Authentication authentication) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        ReviewDraft draft = reviewService.requireDraft(submissionId);
        ReviewVersion version = reviewService.requireVersion(submissionId, versionId);
        ReviewDiffService.DiffResult result =
                diffService.diff(version.getContent(), draft.getContent());
        return DiffResponse.from(version, result);
    }

    @PostMapping("/versions/{versionId}/restore")
    public ReviewResponse restoreVersion(@PathVariable long submissionId,
                                         @PathVariable long versionId,
                                         Authentication authentication) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        ReviewDraft draft = reviewService.restore(submissionId, versionId);
        return ReviewResponse.from(draft, reviewService.countVersions(submissionId));
    }

    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeReview(@PathVariable long submissionId,
                               Authentication authentication) {
        accessControlService.requireTeacherSubmission(authentication.getName(), submissionId);
        reviewService.completeReview(submissionId);
    }
}
