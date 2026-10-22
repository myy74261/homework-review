package com.reviewdesk.service;

/**
 * 指定提交还没有审核意见。
 */
public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(long submissionId) {
        super("找不到审核意见：" + submissionId);
    }
}

