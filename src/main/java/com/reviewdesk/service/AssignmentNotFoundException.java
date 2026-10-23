package com.reviewdesk.service;

/**
 * 指定的课程作业不存在。
 */
public class AssignmentNotFoundException extends RuntimeException {

    public AssignmentNotFoundException(long assignmentId) {
        super("找不到作业：" + assignmentId);
    }
}

