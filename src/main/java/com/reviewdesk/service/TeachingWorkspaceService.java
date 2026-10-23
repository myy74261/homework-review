package com.reviewdesk.service;

import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.AssignmentRepository;
import com.reviewdesk.repository.SubmissionRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 为教师首页汇总待审提交和共性问题。
 */
@Service
@Transactional(readOnly = true)
public class TeachingWorkspaceService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("M 月 d 日 HH:mm", Locale.CHINA);

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ReviewIssueService issueService;

    public TeachingWorkspaceService(AssignmentRepository assignmentRepository,
                                    SubmissionRepository submissionRepository,
                                    ReviewIssueService issueService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.issueService = issueService;
    }

    public List<AssignmentOverview> listAssignments(List<Long> classroomIds) {
        if (classroomIds.isEmpty()) {
            return List.of();
        }
        return assignmentRepository
                .findByClassroomIdInOrderByPublishedAtDesc(classroomIds).stream()
                .map(assignment -> new AssignmentOverview(
                        assignment,
                        submissionRepository.countByAssignmentId(assignment.getId()),
                        DATE_TIME.format(assignment.getDueAt().atZone(DISPLAY_ZONE))))
                .toList();
    }

    public List<SubmissionOverview> listSubmissions(List<Long> classroomIds) {
        List<Long> assignmentIds = listAssignments(classroomIds).stream()
                .map(overview -> overview.assignment().getId())
                .toList();
        if (assignmentIds.isEmpty()) {
            return List.of();
        }
        return submissionRepository.findByAssignmentIdInOrderBySubmittedAtDesc(assignmentIds)
                .stream()
                .map(submission -> new SubmissionOverview(
                        submission,
                        DATE_TIME.format(submission.getSubmittedAt().atZone(DISPLAY_ZONE)),
                        submission.getReviewStatus().equals("PENDING") ? "待审核" : "已审核"))
                .toList();
    }

    public List<ReviewIssueService.IssueSummary> listCommonIssues(
            List<Long> classroomIds) {
        List<Long> submissionIds = listSubmissions(classroomIds).stream()
                .map(overview -> overview.submission().getId())
                .toList();
        return issueService.summarizeForSubmissions(submissionIds);
    }

    public record AssignmentOverview(Assignment assignment, long submissionCount,
                                     String dueAtText) {
    }

    public record SubmissionOverview(Submission submission, String submittedAtText,
                                     String statusText) {
    }
}

