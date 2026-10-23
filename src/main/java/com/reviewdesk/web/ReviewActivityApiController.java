package com.reviewdesk.web;

import com.reviewdesk.domain.ReviewIssue;
import com.reviewdesk.service.ReviewActivityService;
import com.reviewdesk.service.ReviewIssueService;
import com.reviewdesk.web.api.CreateIssueRequest;
import com.reviewdesk.web.api.IssueResponse;
import com.reviewdesk.web.api.ReviewEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采集教师确认的问题和审核动作。
 */
@RestController
@RequestMapping("/api/submissions/{submissionId}")
public class ReviewActivityApiController {

    private final ReviewIssueService issueService;
    private final ReviewActivityService activityService;

    public ReviewActivityApiController(ReviewIssueService issueService,
                                       ReviewActivityService activityService) {
        this.issueService = issueService;
        this.activityService = activityService;
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse createIssue(@PathVariable long submissionId,
                                     @Valid @RequestBody CreateIssueRequest request) {
        ReviewIssue issue = issueService.create(
                submissionId,
                request.rubricName(),
                request.evidenceQuote(),
                request.teacherNote());
        return IssueResponse.from(issue);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordEvent(@PathVariable long submissionId,
                            @Valid @RequestBody ReviewEventRequest request) {
        activityService.record(submissionId, request.eventType(), request.detail());
    }
}

