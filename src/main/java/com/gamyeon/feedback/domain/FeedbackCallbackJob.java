package com.gamyeon.feedback.domain;

import com.gamyeon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(
    name = "feedback_callback_jobs",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_feedback_callback_jobs_request_id", columnNames = "request_id"),
      @UniqueConstraint(
          name = "uk_feedback_callback_jobs_question_set_id",
          columnNames = "question_set_id")
    })
@Getter
public class FeedbackCallbackJob extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "question_set_id", nullable = false)
  private Long questionSetId;

  @Column(name = "intv_id")
  private Long intvId;

  @Column(name = "request_id", length = 64)
  private String requestId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private FeedbackCallbackJobStatus status;

  @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
  private String rawPayload;

  @Column(nullable = false)
  private int retryCount;

  @Column(nullable = false)
  private int maxRetryCount;

  private LocalDateTime nextRetryAt;

  @Column(columnDefinition = "TEXT")
  private String lastError;

  private LocalDateTime receivedAt;

  private LocalDateTime lastAttemptAt;

  private LocalDateTime processedAt;

  protected FeedbackCallbackJob() {}

  private FeedbackCallbackJob(
      Long questionSetId, String requestId, String rawPayload, int maxRetryCount) {
    this.questionSetId = questionSetId;
    this.requestId = requestId;
    this.rawPayload = rawPayload;
    this.maxRetryCount = maxRetryCount;
    this.status = FeedbackCallbackJobStatus.RECEIVED;
    this.retryCount = 0;
    this.receivedAt = LocalDateTime.now();
  }

  public static FeedbackCallbackJob create(
      Long questionSetId, String requestId, String rawPayload, int maxRetryCount) {
    return new FeedbackCallbackJob(questionSetId, requestId, rawPayload, maxRetryCount);
  }

  public void markProcessing(LocalDateTime attemptedAt) {
    this.status = FeedbackCallbackJobStatus.PROCESSING;
    this.lastAttemptAt = attemptedAt;
    this.lastError = null;
  }

  public void markProcessed(Long intvId) {
    this.intvId = intvId;
    this.status = FeedbackCallbackJobStatus.PROCESSED;
    this.processedAt = LocalDateTime.now();
    this.nextRetryAt = null;
    this.lastError = null;
  }

  public int incrementRetryCount() {
    this.retryCount += 1;
    return this.retryCount;
  }

  public boolean canRetry() {
    return retryCount <= maxRetryCount;
  }

  public void scheduleRetry(String errorMessage, LocalDateTime nextRetryAt) {
    this.status = FeedbackCallbackJobStatus.RETRY_WAITING;
    this.lastError = errorMessage;
    this.nextRetryAt = nextRetryAt;
  }

  public void markFailed(String errorMessage) {
    this.status = FeedbackCallbackJobStatus.FAILED;
    this.lastError = errorMessage;
    this.nextRetryAt = null;
  }

  public void reopen(String requestId, String rawPayload, int maxRetryCount) {
    if (status != FeedbackCallbackJobStatus.FAILED) {
      throw new IllegalStateException("FAILED 상태의 callback job만 재접수할 수 있습니다.");
    }

    if (requestId != null && !requestId.isBlank()) {
      this.requestId = requestId;
    }
    this.rawPayload = rawPayload;
    this.maxRetryCount = maxRetryCount;
    this.retryCount = 0;
    this.status = FeedbackCallbackJobStatus.RECEIVED;
    this.nextRetryAt = null;
    this.lastError = null;
    this.receivedAt = LocalDateTime.now();
    this.lastAttemptAt = null;
    this.processedAt = null;
  }

  public boolean isTerminal() {
    return status == FeedbackCallbackJobStatus.PROCESSED
        || status == FeedbackCallbackJobStatus.FAILED;
  }
}
