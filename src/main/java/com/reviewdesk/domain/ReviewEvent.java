package com.reviewdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 教师审核过程中产生的关键事件。
 *
 * <p>只记录审核动作，不记录键盘级输入，避免将过程采集变成无意义的监控。</p>
 */
@Entity
@Table(name = "review_events", indexes = {
        @Index(name = "idx_review_events_submission",
                columnList = "submission_id, occurred_at")
})
public class ReviewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false, length = 240)
    private String detail;

    @Column(nullable = false)
    private Instant occurredAt;

    protected ReviewEvent() {
        // JPA 需要的无参构造方法
    }

    public ReviewEvent(Long submissionId, String eventType, String detail) {
        this.submissionId = submissionId;
        this.eventType = eventType;
        this.detail = detail;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
