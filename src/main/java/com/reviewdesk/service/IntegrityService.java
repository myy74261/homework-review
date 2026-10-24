package com.reviewdesk.service;

import com.reviewdesk.domain.IntegrityFlag;
import com.reviewdesk.repository.IntegrityFlagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师可疑问题标注、确认与撤销。
 */
@Service
@Transactional(readOnly = true)
public class IntegrityService {

    private final IntegrityFlagRepository flagRepository;
    private final AccessControlService accessControlService;
    private final ReviewActivityService activityService;

    public IntegrityService(IntegrityFlagRepository flagRepository,
                            AccessControlService accessControlService,
                            ReviewActivityService activityService) {
        this.flagRepository = flagRepository;
        this.accessControlService = accessControlService;
        this.activityService = activityService;
    }

    @Transactional
    public IntegrityFlag create(String teacherUsername, long submissionId,
                                String category, String severity,
                                String evidence, String reason) {
        accessControlService.requireTeacherSubmission(teacherUsername, submissionId);
        if (category == null || category.isBlank()
                || evidence == null || evidence.isBlank()
                || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("请填写可疑问题证据和教师判断");
        }
        IntegrityFlag flag = flagRepository.save(new IntegrityFlag(
                submissionId,
                category.strip(),
                severity == null || severity.isBlank() ? "MEDIUM" : severity.strip(),
                evidence.strip(),
                reason.strip()));
        activityService.record(submissionId, "INTEGRITY_FLAGGED", category.strip());
        return flag;
    }

    public List<IntegrityFlag> list(long submissionId) {
        return flagRepository.findBySubmissionIdOrderByCreatedAtDesc(submissionId);
    }

    public List<IntegrityFlag> confirmedForStudent(long submissionId) {
        return list(submissionId).stream()
                .filter(flag -> "CONFIRMED".equals(flag.getStatus()))
                .toList();
    }

    @Transactional
    public IntegrityFlag resolve(String teacherUsername, long flagId, String status) {
        IntegrityFlag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("诚信标记不存在"));
        accessControlService.requireTeacherSubmission(
                teacherUsername, flag.getSubmissionId());
        if (!"CONFIRMED".equals(status) && !"CLEARED".equals(status)) {
            throw new IllegalArgumentException("不支持的诚信处理状态");
        }
        flag.resolve(status);
        return flagRepository.save(flag);
    }
}
