package com.reviewdesk.service;

import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.ReviewVersion;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.ReviewDraftRepository;
import com.reviewdesk.repository.ReviewVersionRepository;
import com.reviewdesk.repository.SubmissionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师审核意见保存、版本快照与恢复的业务入口。
 */
@Service
@Transactional(readOnly = true)
public class ReviewService {

    public static final long DEMO_SUBMISSION_ID = 1L;

    private final SubmissionRepository submissionRepository;
    private final ReviewDraftRepository draftRepository;
    private final ReviewVersionRepository versionRepository;
    private final ReviewActivityService activityService;

    public ReviewService(SubmissionRepository submissionRepository,
                         ReviewDraftRepository draftRepository,
                         ReviewVersionRepository versionRepository,
                         ReviewActivityService activityService) {
        this.submissionRepository = submissionRepository;
        this.draftRepository = draftRepository;
        this.versionRepository = versionRepository;
        this.activityService = activityService;
    }

    public Submission requireSubmission(long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
    }

    public ReviewDraft requireDraft(long submissionId) {
        requireSubmission(submissionId);
        return draftRepository.findById(submissionId)
                .orElseThrow(() -> new ReviewNotFoundException(submissionId));
    }

    public ReviewVersion requireVersion(long submissionId, long versionId) {
        return versionRepository.findByIdAndSubmissionId(versionId, submissionId)
                .orElseThrow(() -> new ReviewVersionNotFoundException(submissionId, versionId));
    }

    /**
     * 保存教师反馈，并检查客户端读取的版本仍然是最新版本。
     */
    @Transactional
    public ReviewDraft save(long submissionId, String title, String content,
                            long baseRevision) {
        ReviewDraft draft = requireDraft(submissionId);
        draft.applyUpdate(title.strip(), content, baseRevision);
        ReviewDraft saved = draftRepository.save(draft);
        activityService.record(submissionId, "FEEDBACK_SAVED", "自动保存教师反馈");
        return saved;
    }

    /**
     * 为当前审核意见创建反馈快照。
     *
     * @param triggerType INITIAL、MANUAL 或 RESTORE
     */
    @Transactional
    public ReviewVersion createVersion(long submissionId, String triggerType) {
        ReviewDraft draft = requireDraft(submissionId);
        ReviewVersion version = appendVersion(draft, triggerType);
        activityService.record(
                submissionId, "VERSION_CREATED", "反馈版本 " + version.getVersionNumber());
        return version;
    }

    public List<ReviewVersion> listVersions(long submissionId) {
        requireSubmission(submissionId);
        return versionRepository.findBySubmissionIdOrderByVersionNumberDesc(submissionId);
    }

    public long countVersions(long submissionId) {
        requireSubmission(submissionId);
        return versionRepository.countBySubmissionId(submissionId);
    }

    /**
     * 恢复历史反馈，同时生成恢复后快照，确保审核轨迹完整。
     */
    @Transactional
    public ReviewDraft restore(long submissionId, long versionId) {
        ReviewDraft draft = requireDraft(submissionId);
        ReviewVersion version = requireVersion(submissionId, versionId);
        draft.applyRestore(version.getTitle(), version.getContent());
        draftRepository.save(draft);
        ReviewVersion restored = appendVersion(draft, "RESTORE");
        activityService.record(
                submissionId, "VERSION_RESTORED",
                "恢复反馈版本 " + version.getVersionNumber()
                        + "，生成快照 " + restored.getVersionNumber());
        return draft;
    }

    /**
     * 标记教师已完成审核，并记录审核事件。
     */
    @Transactional
    public Submission completeReview(long submissionId) {
        Submission submission = requireSubmission(submissionId);
        submission.markReviewed();
        submissionRepository.save(submission);
        activityService.record(submissionId, "REVIEW_COMPLETED", "教师完成本轮审核");
        return submission;
    }

    private ReviewVersion appendVersion(ReviewDraft draft, String triggerType) {
        long nextVersion = versionRepository
                .findFirstBySubmissionIdOrderByVersionNumberDesc(draft.getSubmissionId())
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1L);
        ReviewVersion version = new ReviewVersion(
                draft.getSubmissionId(),
                nextVersion,
                draft.getTitle(),
                draft.getContent(),
                countCharacters(draft.getContent()),
                triggerType);
        return versionRepository.save(version);
    }

    /**
     * 统计不含空白字符的字数，与服务端展示口径一致。
     */
    public static int countCharacters(String text) {
        return (int) text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count();
    }
}

