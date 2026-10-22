package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 教师当前正在编写的审核意见。
 *
 * <p>每次保存都会递增 {@code revision}。客户端保存时提交读取到的版本号，
 * 版本不一致则拒绝覆盖，避免多个审核页面互相静默覆盖。</p>
 */
@Entity
@Table(name = "review_drafts")
public class ReviewDraft {

    @Id
    private Long submissionId;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private long revision;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ReviewDraft() {
        // JPA 需要的无参构造方法
    }

    public ReviewDraft(Long submissionId, String title, String content) {
        this.submissionId = submissionId;
        this.title = title;
        this.content = content;
        this.revision = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 保存教师反馈。
     *
     * @param title 审核意见标题
     * @param content 审核意见正文
     * @param expectedRevision 客户端读取到的版本号
     */
    public void applyUpdate(String title, String content, long expectedRevision) {
        if (revision != expectedRevision) {
            throw new StaleReviewException(revision);
        }
        this.title = title;
        this.content = content;
        this.revision++;
        this.updatedAt = Instant.now();
    }

    /**
     * 恢复历史反馈。恢复本身也是一次新保存，因此版本号继续递增。
     *
     * @param title 历史审核意见标题
     * @param content 历史审核意见正文
     */
    public void applyRestore(String title, String content) {
        this.title = title;
        this.content = content;
        this.revision++;
        this.updatedAt = Instant.now();
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

