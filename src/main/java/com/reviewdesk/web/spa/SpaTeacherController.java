package com.reviewdesk.web.spa;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Appeal;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.AssignmentRubric;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.IntegrityFlag;
import com.reviewdesk.domain.ReviewDraft;
import com.reviewdesk.domain.ReviewIssue;
import com.reviewdesk.domain.ReviewScore;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.domain.SubmissionAttachment;
import com.reviewdesk.repository.ReviewIssueRepository;
import com.reviewdesk.repository.SubmissionRepository;
import com.reviewdesk.service.AccessControlService;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.AppealService;
import com.reviewdesk.service.AiReviewService;
import com.reviewdesk.service.AiSettingsService;
import com.reviewdesk.service.AiBatchReviewService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ClassroomService;
import com.reviewdesk.service.IntegrityService;
import com.reviewdesk.service.ReviewActivityService;
import com.reviewdesk.service.ReviewScoreService;
import com.reviewdesk.service.ReviewService;
import com.reviewdesk.service.SubmissionAttachmentService;
import com.reviewdesk.service.TeachingSuggestionService;
import com.reviewdesk.service.TeachingWorkspaceService;
import com.reviewdesk.web.PublishAssignmentForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * React 教师端页面所需的 JSON 接口。
 */
@RestController
@RequestMapping("/api/app/teacher")
public class SpaTeacherController {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");

    private final AccountService accountService;
    private final ClassroomService classroomService;
    private final TeachingWorkspaceService workspaceService;
    private final AssignmentService assignmentService;
    private final ReviewService reviewService;
    private final ReviewScoreService scoreService;
    private final IntegrityService integrityService;
    private final SubmissionAttachmentService attachmentService;
    private final AppealService appealService;
    private final AiReviewService aiReviewService;
    private final AiSettingsService aiSettingsService;
    private final AiBatchReviewService aiBatchReviewService;
    private final ReviewIssueRepository issueRepository;
    private final SubmissionRepository submissionRepository;
    private final ReviewActivityService activityService;
    private final AccessControlService accessControlService;
    private final TeachingSuggestionService suggestionService;

    public SpaTeacherController(
            AccountService accountService,
            ClassroomService classroomService,
            TeachingWorkspaceService workspaceService,
            AssignmentService assignmentService,
            ReviewService reviewService,
            ReviewScoreService scoreService,
            IntegrityService integrityService,
            SubmissionAttachmentService attachmentService,
            AppealService appealService,
            AiReviewService aiReviewService,
            AiSettingsService aiSettingsService,
            AiBatchReviewService aiBatchReviewService,
            ReviewIssueRepository issueRepository,
            SubmissionRepository submissionRepository,
            ReviewActivityService activityService,
            AccessControlService accessControlService,
            TeachingSuggestionService suggestionService) {
        this.accountService = accountService;
        this.classroomService = classroomService;
        this.workspaceService = workspaceService;
        this.assignmentService = assignmentService;
        this.reviewService = reviewService;
        this.scoreService = scoreService;
        this.integrityService = integrityService;
        this.attachmentService = attachmentService;
        this.appealService = appealService;
        this.aiReviewService = aiReviewService;
        this.aiSettingsService = aiSettingsService;
        this.aiBatchReviewService = aiBatchReviewService;
        this.issueRepository = issueRepository;
        this.submissionRepository = submissionRepository;
        this.activityService = activityService;
        this.accessControlService = accessControlService;
        this.suggestionService = suggestionService;
    }

    @GetMapping("/dashboard")
    public SpaViewModels.TeacherDashboard dashboard(Authentication authentication) {
        Account teacher = accountService.current(authentication);
        List<Classroom> classrooms = classroomService.teacherClassrooms(teacher);
        List<Long> classroomIds = classrooms.stream().map(Classroom::getId).toList();
        var submissions = workspaceService.listSubmissions(classroomIds);
        var assignmentOverviews = workspaceService.listAssignments(classroomIds);
        Map<Long, Submission> submissionById = submissions.stream()
                .collect(Collectors.toMap(
                        row -> row.submission().getId(),
                        TeachingWorkspaceService.SubmissionOverview::submission));

        List<SpaViewModels.AppealOverview> appeals =
                appealService.listForSubmissions(submissionById.keySet()).stream()
                        .map(appeal -> {
                            Submission submission = submissionById.get(appeal.getSubmissionId());
                            return new SpaViewModels.AppealOverview(
                                    toAppeal(appeal),
                                    submission == null ? "未知学生" : submission.getStudentName(),
                                    submission == null ? "未知作业" : submission.getAssignmentTitle());
                        })
                        .toList();

        List<Long> assignmentIds = assignmentOverviews.stream()
                .map(item -> item.assignment().getId())
                .toList();
        List<SpaViewModels.ClassroomAnalyticsView> classroomAnalytics =
                classrooms.stream().map(classroom -> {
                    List<Long> classAssignmentIds = assignmentOverviews.stream()
                            .filter(item -> item.assignment().getClassroomId()
                                    .equals(classroom.getId()))
                            .map(item -> item.assignment().getId())
                            .toList();
                    long submittedStudents = submissions.stream()
                            .filter(item -> classAssignmentIds.contains(
                                    item.submission().getAssignmentId()))
                            .map(item -> item.submission().getStudentAccountId())
                            .distinct()
                            .count();
                    return new SpaViewModels.ClassroomAnalyticsView(
                            classroom.getId(),
                            classroomService.memberCount(classroom.getId()),
                            submittedStudents,
                            workspaceService.listCommonIssues(
                                            List.of(classroom.getId())).stream()
                                    .map(issue -> new SpaViewModels.CommonIssueView(
                                            issue.rubricName(), issue.count()))
                                    .toList());
                }).toList();
        return new SpaViewModels.TeacherDashboard(
                toUser(teacher),
                classrooms.stream()
                        .map(classroom -> toClassroom(classroom,
                                classroomService.memberCount(classroom.getId())))
                        .toList(),
                assignmentOverviews.stream()
                        .map(item -> new SpaViewModels.AssignmentOverview(
                                toAssignment(item.assignment()), item.submissionCount()))
                        .toList(),
                submissions.stream()
                        .map(item -> new SpaViewModels.SubmissionOverview(
                                item.submission().getId(),
                                item.submission().getStudentName(),
                                item.submission().getStudentNumber(),
                                item.submission().getAssignmentTitle(),
                                item.submission().getAttemptNumber(),
                                item.submission().getSubmittedAt(),
                                item.submission().getReviewStatus(),
                                item.statusText()))
                        .toList(),
                appeals,
                submissions.stream().filter(item -> "已审核".equals(item.statusText())).count(),
                aiSettingsService.isConfigured(teacher.getUsername()),
                aiBatchReviewService.progress(assignmentIds).stream()
                        .map(item -> new SpaViewModels.AiBatchProgressView(
                                item.assignmentId(),
                                item.total(),
                                item.completed(),
                                item.running(),
                                item.pending(),
                                item.failed(),
                                item.skipped()))
                        .toList(),
                classroomAnalytics);
    }

    @PostMapping("/classrooms")
    public SpaViewModels.ClassroomView createClassroom(
            Authentication authentication,
            @Valid @RequestBody CreateClassroomRequest request) {
        Account teacher = accountService.current(authentication);
        Classroom classroom = classroomService.create(
                teacher,
                request.name(),
                request.courseName(),
                request.startAt(),
                request.endAt());
        return toClassroom(classroom, 0);
    }

    @PutMapping("/classrooms/{classroomId}/schedule")
    public SpaViewModels.ClassroomView updateClassroomSchedule(
            @PathVariable long classroomId,
            Authentication authentication,
            @Valid @RequestBody UpdateClassroomScheduleRequest request) {
        Account teacher = accountService.current(authentication);
        Classroom classroom = classroomService.updateSchedule(
                teacher, classroomId, request.startAt(), request.endAt());
        return toClassroom(classroom, classroomService.memberCount(classroomId));
    }

    @PostMapping("/classrooms/{classroomId}/dissolve")
    public SpaViewModels.ClassroomView dissolveClassroom(
            @PathVariable long classroomId,
            Authentication authentication) {
        Account teacher = accountService.current(authentication);
        Classroom classroom = classroomService.dissolve(teacher, classroomId);
        return toClassroom(classroom, classroomService.memberCount(classroomId));
    }

    @PostMapping("/classrooms/{classroomId}/roster")
    public ClassroomService.RosterImportResult importClassroomRoster(
            @PathVariable long classroomId,
            Authentication authentication,
            @Valid @RequestBody ImportClassroomRosterRequest request) {
        Account teacher = accountService.current(authentication);
        return classroomService.importRoster(
                teacher,
                classroomId,
                request.students().stream()
                        .map(student -> new ClassroomService.RosterEntry(
                                student.studentNumber(), student.studentName()))
                        .toList());
    }

    @GetMapping("/classrooms")
    public List<SpaViewModels.ClassroomView> classrooms(Authentication authentication) {
        Account teacher = accountService.current(authentication);
        return classroomService.teacherClassrooms(teacher).stream()
                .map(classroom -> toClassroom(classroom,
                        classroomService.memberCount(classroom.getId())))
                .toList();
    }

    @PostMapping("/assignments")
    public Map<String, Object> publishAssignment(
            Authentication authentication,
            @Valid @RequestBody PublishAssignmentForm form) {
        Account teacher = accountService.current(authentication);
        if (!classroomService.isTeacherOwner(teacher, form.getClassroomId())) {
            throw new SecurityException("无权向该课堂发布作业");
        }
        List<AssignmentService.RubricDefinition> rubrics = toRubricDefinitions(form);
        Assignment assignment = assignmentService.publish(
                form.getClassroomId(),
                form.getTitle(),
                form.getCourseName(),
                form.getInstructions(),
                form.getDueAt().atZone(DISPLAY_ZONE).toInstant(),
                form.getSubmissionFormat(),
                form.getAiPolicy(),
                form.isAiDeclarationRequired(),
                form.isSourceDeclarationRequired(),
                form.isAppealAllowed(),
                rubrics);
        return Map.of(
                "id", assignment.getId(),
                "status", assignment.getStatus(),
                "redirectPath", "/app/teacher");
    }

    @PostMapping("/assignments/{assignmentId}/withdraw")
    public Map<String, Object> withdrawAssignment(
            @PathVariable long assignmentId,
            Authentication authentication) {
        accessControlService.requireTeacherAssignment(
                authentication.getName(), assignmentId);
        Assignment assignment = assignmentService.withdraw(assignmentId);
        return Map.of(
                "id", assignment.getId(),
                "status", assignment.getStatus(),
                "redirectPath", "/app/teacher/assignments/" + assignmentId + "/edit");
    }

    @PutMapping("/assignments/{assignmentId}/due-date")
    public SpaViewModels.AssignmentView updateAssignmentDueDate(
            @PathVariable long assignmentId,
            Authentication authentication,
            @Valid @RequestBody UpdateAssignmentDueDateRequest request) {
        accessControlService.requireTeacherAssignment(
                authentication.getName(), assignmentId);
        Assignment assignment = assignmentService.updateDueAt(
                assignmentId,
                request.dueAt().atZone(DISPLAY_ZONE).toInstant());
        return toAssignment(assignment);
    }

    @GetMapping("/assignments/{assignmentId}/edit")
    public SpaViewModels.EditableAssignmentView editableAssignment(
            @PathVariable long assignmentId,
            Authentication authentication) {
        accessControlService.requireTeacherAssignment(
                authentication.getName(), assignmentId);
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        if (!"WITHDRAWN".equals(assignment.getStatus())) {
            throw new IllegalStateException("请先撤回作业，再修改内容");
        }
        return new SpaViewModels.EditableAssignmentView(
                toAssignment(assignment),
                assignmentService.listRubrics(assignmentId).stream()
                        .map(this::toRubric)
                        .toList(),
                submissionRepository.countByAssignmentId(assignmentId));
    }

    @PutMapping("/assignments/{assignmentId}")
    public Map<String, Object> updateAndRepublishAssignment(
            @PathVariable long assignmentId,
            Authentication authentication,
            @Valid @RequestBody PublishAssignmentForm form) {
        Account teacher = accountService.current(authentication);
        accessControlService.requireTeacherAssignment(
                authentication.getName(), assignmentId);
        if (!classroomService.isTeacherOwner(teacher, form.getClassroomId())) {
            throw new SecurityException("无权向该课堂发布作业");
        }
        Assignment assignment = assignmentService.updateAndRepublish(
                assignmentId,
                form.getClassroomId(),
                form.getTitle(),
                form.getCourseName(),
                form.getInstructions(),
                form.getDueAt().atZone(DISPLAY_ZONE).toInstant(),
                form.getSubmissionFormat(),
                form.getAiPolicy(),
                form.isAiDeclarationRequired(),
                form.isSourceDeclarationRequired(),
                form.isAppealAllowed(),
                toRubricDefinitions(form));
        return Map.of(
                "id", assignment.getId(),
                "status", assignment.getStatus(),
                "redirectPath", "/app/teacher");
    }

    @GetMapping("/reviews/{submissionId}")
    public SpaViewModels.TeacherReviewWorkspace reviewWorkspace(
            @PathVariable long submissionId,
            Authentication authentication) {
        accessControlService.requireTeacherSubmission(
                authentication.getName(), submissionId);
        Submission submission = reviewService.requireSubmission(submissionId);
        Assignment assignment = assignmentService.requireAssignment(
                submission.getAssignmentId());
        ReviewDraft draft = reviewService.requireDraft(submissionId);
        activityService.record(submissionId, "SUBMISSION_VIEWED", "教师打开 React 审核工作台");
        return new SpaViewModels.TeacherReviewWorkspace(
                toAssignment(assignment),
                toSubmission(submission),
                new SpaViewModels.ReviewDraftView(
                        draft.getTitle(),
                        draft.getContent(),
                        draft.getRevision(),
                        draft.getUpdatedAt(),
                        reviewService.countVersions(submissionId)),
                attachmentService.list(submissionId).stream()
                        .map(this::toAttachment)
                        .toList(),
                assignmentService.listRubrics(assignment.getId()).stream()
                        .map(this::toRubric)
                        .toList(),
                scoreService.getOrCreate(submissionId).stream()
                        .map(this::toScore)
                        .toList(),
                scoreService.finalTotal(submissionId),
                integrityService.list(submissionId).stream()
                        .map(this::toIntegrityFlag)
                        .toList(),
                issueRepository.findBySubmissionIdInOrderByCreatedAtDesc(
                                List.of(submissionId)).stream()
                        .map(this::toIssue)
                        .toList(),
                reviewChecklist(assignment.getSubmissionFormat()),
                aiReviewService.isConfigured(authentication.getName()),
                aiReviewService.list(submissionId).stream()
                        .map(this::toAiReview)
                        .toList());
    }

    @PostMapping("/reviews/{submissionId}/ai-review")
    public SpaViewModels.AiReviewRunView generateAiReview(
            @PathVariable long submissionId,
            Authentication authentication) {
        return toAiReview(aiReviewService.generate(
                authentication.getName(), submissionId));
    }

    @GetMapping("/ai-settings")
    public AiSettingsService.SettingsView aiSettings(Authentication authentication) {
        return aiSettingsService.view(authentication.getName());
    }

    @PutMapping("/ai-settings")
    public AiSettingsService.SettingsView saveAiSettings(
            Authentication authentication,
            @Valid @RequestBody AiSettingsRequest request) {
        return aiSettingsService.save(
                authentication.getName(),
                request.provider(),
                request.model(),
                request.apiKey());
    }

    @PostMapping("/ai-settings/test")
    public AiSettingsService.ConnectionResult testAiSettings(
            Authentication authentication,
            @RequestBody AiSettingsRequest request) {
        return aiSettingsService.test(
                authentication.getName(),
                request.provider(),
                request.model(),
                request.apiKey());
    }

    @PostMapping("/ai-review/suggestions/{suggestionId}/adopt")
    public SpaViewModels.AiReviewSuggestionView adoptAiSuggestion(
            @PathVariable long suggestionId,
            Authentication authentication,
            @RequestBody(required = false) AiSuggestionDecisionRequest request) {
        return toAiSuggestion(aiReviewService.adopt(
                authentication.getName(),
                suggestionId,
                request == null ? null : request.teacherNote()));
    }

    @PostMapping("/ai-review/suggestions/{suggestionId}/dismiss")
    public SpaViewModels.AiReviewSuggestionView dismissAiSuggestion(
            @PathVariable long suggestionId,
            Authentication authentication) {
        return toAiSuggestion(aiReviewService.dismiss(
                authentication.getName(), suggestionId));
    }

    @PutMapping("/submissions/{submissionId}/scores")
    public List<SpaViewModels.ScoreView> saveScores(
            @PathVariable long submissionId,
            Authentication authentication,
            @RequestBody FinalScoresRequest request) {
        accessControlService.requireTeacherSubmission(
                authentication.getName(), submissionId);
        return scoreService.saveFinalScores(
                        submissionId,
                        request.scores() == null ? Map.of() : request.scores())
                .stream()
                .map(this::toScore)
                .toList();
    }

    @PostMapping("/submissions/{submissionId}/integrity-flags")
    public SpaViewModels.IntegrityFlagView createIntegrityFlag(
            @PathVariable long submissionId,
            Authentication authentication,
            @Valid @RequestBody IntegrityRequest request) {
        IntegrityFlag flag = integrityService.create(
                authentication.getName(),
                submissionId,
                request.category(),
                request.severity(),
                request.evidence(),
                request.reason());
        return toIntegrityFlag(flag);
    }

    @PostMapping("/integrity-flags/{flagId}/resolve")
    public SpaViewModels.IntegrityFlagView resolveIntegrityFlag(
            @PathVariable long flagId,
            Authentication authentication,
            @Valid @RequestBody ResolveIntegrityRequest request) {
        return toIntegrityFlag(integrityService.resolve(
                authentication.getName(), flagId, request.status()));
    }

    @PostMapping("/appeals/{appealId}/respond")
    public SpaViewModels.AppealView respondToAppeal(
            @PathVariable long appealId,
            Authentication authentication,
            @Valid @RequestBody AppealResponseRequest request) {
        return toAppeal(appealService.respond(
                authentication.getName(),
                appealId,
                request.status(),
                request.teacherResponse()));
    }

    @GetMapping("/assignments/{assignmentId}/lesson-plan")
    public SpaViewModels.LessonWorkspace lessonPlan(
            @PathVariable long assignmentId,
            Authentication authentication) {
        accessControlService.requireTeacherAssignment(
                authentication.getName(), assignmentId);
        TeachingSuggestionService.LessonPlanWorkspace workspace =
                suggestionService.getOrCreateWorkspace(assignmentId);
        return new SpaViewModels.LessonWorkspace(
                toAssignment(workspace.assignment()),
                assignmentService.listRubrics(assignmentId).stream()
                        .map(this::toRubric)
                        .toList(),
                workspace.plan().getTitle(),
                workspace.plan().getContent(),
                workspace.plan().getRevision(),
                workspace.plan().getUpdatedAt(),
                workspace.evidence().size(),
                workspace.groups().stream()
                        .map(group -> new SpaViewModels.LessonIssueGroup(
                                group.rubricName(), group.count(), group.teacherNotes()))
                        .toList(),
                suggestionService.isAiConfigured(authentication.getName()));
    }

    private SpaViewModels.UserView toUser(Account account) {
        return new SpaViewModels.UserView(
                account.getId(), account.getUsername(), account.getDisplayName(),
                account.getRole(), account.getStudentNumber(), account.getEmail(),
                account.getDepartment(), account.getProfessionalTitle());
    }

    private List<AssignmentService.RubricDefinition> toRubricDefinitions(
            PublishAssignmentForm form) {
        return form.getRubrics().stream()
                .map(rubric -> new AssignmentService.RubricDefinition(
                        rubric.getName(), rubric.getPoints(), rubric.getDescription()))
                .toList();
    }

    private SpaViewModels.ClassroomView toClassroom(Classroom classroom, long members) {
        return new SpaViewModels.ClassroomView(
                classroom.getId(), classroom.getName(), classroom.getCourseName(),
                classroom.getJoinCode(), members, classroom.getStartAt(),
                classroom.getEndAt(), classroomService.lifecycleStatus(classroom),
                classroomService.rosterCount(classroom.getId()),
                classroomService.linkedRosterCount(classroom.getId()),
                classroomService.unlistedMemberCount(classroom.getId()));
    }

    private SpaViewModels.AssignmentView toAssignment(Assignment assignment) {
        return new SpaViewModels.AssignmentView(
                assignment.getId(), assignment.getClassroomId(), assignment.getTitle(),
                assignment.getCourseName(), assignment.getInstructions(),
                assignment.getDueAt(), assignment.getSubmissionFormat(),
                assignment.getAiPolicy(), assignment.isAiDeclarationRequired(),
                assignment.isSourceDeclarationRequired(), assignment.isAppealAllowed(),
                assignment.getStatus());
    }

    private SpaViewModels.SubmissionView toSubmission(Submission submission) {
        return new SpaViewModels.SubmissionView(
                submission.getId(), submission.getAssignmentId(),
                submission.getAssignmentTitle(), submission.getStudentName(),
                submission.getStudentNumber(), submission.getContent(),
                submission.getWordCount(), submission.getAttemptNumber(),
                submission.getDueAt(), submission.getSubmittedAt(),
                submission.getReviewedAt(), submission.getReviewStatus(),
                submission.getAiUsage(), submission.getAiTools(), submission.getAiScope(),
                submission.getSourceDeclaration(),
                submission.getCollaborationDeclaration(),
                submission.isIntegrityConfirmed());
    }

    private SpaViewModels.RubricView toRubric(AssignmentRubric rubric) {
        return new SpaViewModels.RubricView(
                rubric.getId(), rubric.getName(), rubric.getPoints(),
                rubric.getDescription());
    }

    private SpaViewModels.ScoreView toScore(ReviewScore score) {
        return new SpaViewModels.ScoreView(
                score.getId(), score.getRubricId(), score.getRubricName(),
                score.getMaxPoints(), score.getSuggestedPoints(),
                score.getFinalPoints(), score.getSuggestion());
    }

    private SpaViewModels.IntegrityFlagView toIntegrityFlag(IntegrityFlag flag) {
        return new SpaViewModels.IntegrityFlagView(
                flag.getId(), flag.getCategory(), flag.getSeverity(),
                flag.getEvidence(), flag.getReason(), flag.getStatus(),
                flag.getCreatedAt(), flag.getResolvedAt());
    }

    private SpaViewModels.AppealView toAppeal(Appeal appeal) {
        return new SpaViewModels.AppealView(
                appeal.getId(), appeal.getSubmissionId(), appeal.getReason(),
                appeal.getRequestedResolution(), appeal.getStatus(),
                appeal.getTeacherResponse(), appeal.getCreatedAt(),
                appeal.getRespondedAt());
    }

    private SpaViewModels.AttachmentView toAttachment(
            SubmissionAttachment attachment) {
        return new SpaViewModels.AttachmentView(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getAttachmentKind(),
                "/files/submissions/" + attachment.getSubmissionId()
                        + "/attachments/" + attachment.getId());
    }

    private SpaViewModels.IssueView toIssue(ReviewIssue issue) {
        return new SpaViewModels.IssueView(
                issue.getId(), issue.getRubricName(), issue.getEvidenceQuote(),
                issue.getTeacherNote(), issue.getCreatedAt());
    }

    private SpaViewModels.AiReviewRunView toAiReview(
            AiReviewService.ReviewRunView review) {
        return new SpaViewModels.AiReviewRunView(
                review.run().getId(),
                review.run().getModel(),
                review.run().getSummary(),
                review.run().getCreatedAt(),
                review.suggestions().stream()
                        .map(this::toAiSuggestion)
                        .toList());
    }

    private SpaViewModels.AiReviewSuggestionView toAiSuggestion(
            com.reviewdesk.domain.AiReviewSuggestion suggestion) {
        return new SpaViewModels.AiReviewSuggestionView(
                suggestion.getId(),
                suggestion.getRubricName(),
                suggestion.getEvidenceQuote(),
                suggestion.getSuggestion(),
                suggestion.getSuggestedPoints(),
                suggestion.getStatus(),
                suggestion.getCreatedAt(),
                suggestion.getDecidedAt());
    }

    private List<String> reviewChecklist(String submissionFormat) {
        return switch (submissionFormat) {
            case "CODE" -> List.of(
                    "先运行或阅读代码，确认功能、边界条件与错误处理。",
                    "对照量规检查结构、可读性和关键实现说明。",
                    "只在需要时补充诚信核查，避免重复填写文档类审核项。");
            case "SCREENSHOT" -> List.of(
                    "打开截图或设计稿，核对目标、交互状态与可读性。",
                    "将视觉证据与量规维度一一对应。",
                    "如无文字稿，可跳过文档结构类重复检查。");
            case "MIXED" -> List.of(
                    "按“附件证据 → 文字说明 → 量规评分”的顺序审核。",
                    "优先检查文档、代码和截图之间是否互相印证。",
                    "仅对可疑来源或 AI 使用扩项，不重复审核已验证内容。");
            default -> List.of(
                    "先核对作业要求与评分量规，再阅读提交全文。",
                    "定位具体证据后填写反馈，避免泛泛评价。",
                    "按需核查引用与 AI 使用声明。");
        };
    }

    public record CreateClassroomRequest(
            @NotBlank(message = "请填写课堂名称") String name,
            @NotBlank(message = "请填写课程名称") String courseName,
            @NotNull(message = "请选择课程起始时间") Instant startAt,
            @NotNull(message = "请选择课程结束时间") Instant endAt) {
    }

    public record UpdateClassroomScheduleRequest(
            @NotNull(message = "请选择课程起始时间") Instant startAt,
            @NotNull(message = "请选择课程结束时间") Instant endAt) {
    }

    public record FinalScoresRequest(Map<Long, Integer> scores) {
    }

    public record IntegrityRequest(
            @NotBlank String category,
            @NotBlank String severity,
            @NotBlank String evidence,
            @NotBlank String reason) {
    }

    public record ResolveIntegrityRequest(@NotBlank String status) {
    }

    public record AppealResponseRequest(
            @NotBlank String status,
            @NotBlank String teacherResponse) {
    }

    public record AiSuggestionDecisionRequest(String teacherNote) {
    }

    public record AiSettingsRequest(
            @NotBlank String provider,
            String model,
            String apiKey) {
    }

    public record UpdateAssignmentDueDateRequest(
            @NotNull(message = "请选择新的截止时间") LocalDateTime dueAt) {
    }

    public record ImportClassroomRosterRequest(
            @Size(min = 1, max = 500, message = "名单需要包含 1–500 名学生")
            List<@Valid RosterStudentRequest> students) {
    }

    public record RosterStudentRequest(
            @NotBlank(message = "学号不能为空") String studentNumber,
            @NotBlank(message = "姓名不能为空") String studentName) {
    }
}
