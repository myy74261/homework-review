package com.reviewdesk.domain;

/**
 * 客户端保存的审核意见基于过期版本。
 */
public class StaleReviewException extends RuntimeException {

    private final long currentRevision;

    public StaleReviewException(long currentRevision) {
        super("审核意见已在其他页面更新");
        this.currentRevision = currentRevision;
    }

    public long getCurrentRevision() {
        return currentRevision;
    }
}

