package com.reviewdesk.service;

/**
 * 指定的学生提交不存在。
 */
public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(long submissionId) {
        super("找不到提交：" + submissionId);
    }
}

