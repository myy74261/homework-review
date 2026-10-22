package com.reviewdesk.service;

/**
 * 指定的审核反馈版本不存在。
 */
public class ReviewVersionNotFoundException extends RuntimeException {

    public ReviewVersionNotFoundException(long submissionId, long versionId) {
        super("找不到反馈版本：" + versionId + "（提交 " + submissionId + "）");
    }
}

