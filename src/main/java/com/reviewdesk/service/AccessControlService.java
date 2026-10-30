package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

/**
 * 负责跨账号的数据边界检查。
 */
@Service
public class AccessControlService {

    private final AccountService accountService;
    private final ClassroomService classroomService;
    private final AssignmentService assignmentService;
    private final SubmissionRepository submissionRepository;

    public AccessControlService(AccountService accountService,
                                ClassroomService classroomService,
                                AssignmentService assignmentService,
                                SubmissionRepository submissionRepository) {
        this.accountService = accountService;
        this.classroomService = classroomService;
        this.assignmentService = assignmentService;
        this.submissionRepository = submissionRepository;
    }

    public Account requireTeacher(String username) {
        Account account = accountService.requireByUsername(username);
        if (!account.isTeacher()) {
            throw new SecurityException("需要教师权限");
        }
        return account;
    }

    public void requireTeacherClassroom(String username, long classroomId) {
        if (!classroomService.isTeacherManager(requireTeacher(username), classroomId)) {
            throw new SecurityException("无权访问该课堂");
        }
    }

    public void requireTeacherAssignment(String username, long assignmentId) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        requireTeacherClassroom(username, assignment.getClassroomId());
    }

    public void requireTeacherOwnerAssignment(String username, long assignmentId) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        if (!classroomService.isTeacherOwner(requireTeacher(username),
                assignment.getClassroomId())) {
            throw new SecurityException("只有课堂所有者可以执行此操作");
        }
    }

    public Submission requireTeacherSubmission(String username, long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        requireTeacherAssignment(username, submission.getAssignmentId());
        return submission;
    }

    public Submission requireTeacherOwnerSubmission(String username, long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        requireTeacherOwnerAssignment(username, submission.getAssignmentId());
        return submission;
    }
}
