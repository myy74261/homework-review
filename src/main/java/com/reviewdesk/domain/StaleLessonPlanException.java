package com.reviewdesk.domain;

/**
 * 教师编辑的备课稿基于过期版本。
 */
public class StaleLessonPlanException extends RuntimeException {

    private final long currentRevision;

    public StaleLessonPlanException(long currentRevision) {
        super("备课稿已在其他页面更新");
        this.currentRevision = currentRevision;
    }

    public long getCurrentRevision() {
        return currentRevision;
    }
}

