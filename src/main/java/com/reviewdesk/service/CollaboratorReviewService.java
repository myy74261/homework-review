package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.CollaboratorReviewDraft;
import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.CollaboratorReviewDraftRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 区分课堂所有者与协作者的审核草稿，避免协作者互相覆盖。
 */
@Service
@Transactional(readOnly = true)
public class CollaboratorReviewService {

    private final AccountService accountService;
    private final ClassroomService classroomService;
    private final AssignmentService assignmentService;
    private final ReviewService reviewService;
    private final CollaboratorReviewDraftRepository draftRepository;
    private final ReviewActivityService activityService;

    public CollaboratorReviewService(
            AccountService accountService,
            ClassroomService classroomService,
            AssignmentService assignmentService,
            ReviewService reviewService,
            CollaboratorReviewDraftRepository draftRepository,
            ReviewActivityService activityService) {
        this.accountService = accountService;
        this.classroomService = classroomService;
        this.assignmentService = assignmentService;
        this.reviewService = reviewService;
        this.draftRepository = draftRepository;
        this.activityService = activityService;
    }

    public boolean isOwner(String username, long submissionId) {
        Account account = accountService.requireByUsername(username);
        return isOwner(account, submissionId);
    }

    public DraftData ownerDraft(long submissionId) {
        ReviewDraft draft = reviewService.requireDraft(submissionId);
        return new DraftData(
                draft.getTitle(),
                draft.getContent(),
                draft.getRevision(),
                draft.getUpdatedAt(),
                reviewService.countVersions(submissionId),
                true);
    }

    @Transactional
    public DraftData collaboratorDraft(String username, long submissionId) {
        Account reviewer = accountService.requireByUsername(username);
        requireCollaborator(reviewer, submissionId);
        CollaboratorReviewDraft draft = getOrCreate(reviewer, submissionId);
        return new DraftData(
                draft.getTitle(),
                draft.getContent(),
                draft.getRevision(),
                draft.getUpdatedAt(),
                0,
                false);
    }

    @Transactional
    public DraftData saveCollaboratorDraft(String username, long submissionId,
                                           String title, String content,
                                           long baseRevision) {
        Account reviewer = accountService.requireByUsername(username);
        requireCollaborator(reviewer, submissionId);
        CollaboratorReviewDraft draft = getOrCreate(reviewer, submissionId);
        draft.update(title.strip(), content, baseRevision);
        CollaboratorReviewDraft saved = draftRepository.save(draft);
        activityService.recordAs(
                submissionId,
                "FEEDBACK_SAVED",
                "协作者保存独立审核草稿",
                reviewer.getId(),
                reviewer.getDisplayName());
        return new DraftData(
                saved.getTitle(),
                saved.getContent(),
                saved.getRevision(),
                saved.getUpdatedAt(),
                0,
                false);
    }

    public List<OwnerDraftData> collaboratorDraftsForOwner(
            String username, long submissionId) {
        Account owner = accountService.requireByUsername(username);
        if (!isOwner(owner, submissionId)) {
            throw new SecurityException("只有课堂所有者可以查看协作者草稿");
        }
        return draftRepository.findBySubmissionIdOrderByUpdatedAtDesc(submissionId)
                .stream()
                .map(draft -> {
                    Account reviewer = accountService.requireById(
                            draft.getReviewerAccountId());
                    return new OwnerDraftData(
                            reviewer.getId(),
                            reviewer.getDisplayName(),
                            reviewer.getUsername(),
                            draft.getTitle(),
                            draft.getContent(),
                            draft.getUpdatedAt());
                })
                .toList();
    }

    private CollaboratorReviewDraft getOrCreate(Account reviewer, long submissionId) {
        return draftRepository.findBySubmissionIdAndReviewerAccountId(
                        submissionId, reviewer.getId())
                .orElseGet(() -> {
                    Submission submission = reviewService.requireSubmission(submissionId);
                    Assignment assignment = assignmentService.requireAssignment(
                            submission.getAssignmentId());
                    return draftRepository.save(new CollaboratorReviewDraft(
                            submissionId,
                            reviewer.getId(),
                            assignment.getTitle() + " · 协作审核草稿",
                            ""));
                });
    }

    private void requireCollaborator(Account reviewer, long submissionId) {
        if (isOwner(reviewer, submissionId)
                || !classroomService.isTeacherManager(
                        reviewer, assignmentClassroomId(submissionId))) {
            throw new SecurityException("需要课堂协作者权限");
        }
    }

    private boolean isOwner(Account account, long submissionId) {
        return classroomService.isTeacherOwner(
                account, assignmentClassroomId(submissionId));
    }

    private long assignmentClassroomId(long submissionId) {
        Submission submission = reviewService.requireSubmission(submissionId);
        return assignmentService.requireAssignment(
                submission.getAssignmentId()).getClassroomId();
    }

    public record DraftData(String title, String content, long revision,
                            Instant updatedAt, long versionCount, boolean owner) {
    }

    public record OwnerDraftData(long reviewerId, String reviewerName,
                                 String reviewerUsername, String title,
                                 String content, Instant updatedAt) {
    }
}
