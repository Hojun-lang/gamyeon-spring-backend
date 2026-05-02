package com.gamyeon.answer.domain;

import com.gamyeon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "answer_analysis_jobs")
@Getter
public class AnswerAnalysisJob extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long answerId;

  @Column(nullable = false, unique = true, length = 64)
  private String requestId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AnswerAnalysisJobStatus status;

  @Column(nullable = false)
  private int retryCount;

  @Column(nullable = false)
  private int maxRetryCount;

  private LocalDateTime nextRetryAt;

  @Column(columnDefinition = "TEXT")
  private String lastError;

  private LocalDateTime sentAt;

  private LocalDateTime lastAttemptAt;

  private LocalDateTime completedAt;

  protected AnswerAnalysisJob() {}

  private AnswerAnalysisJob(Long answerId, String requestId, int maxRetryCount) {
    this.answerId = answerId;
    this.requestId = requestId;
    this.maxRetryCount = maxRetryCount;
    this.status = AnswerAnalysisJobStatus.QUEUED;
    this.retryCount = 0;
  }

  public static AnswerAnalysisJob create(Long answerId, String requestId, int maxRetryCount) {
    return new AnswerAnalysisJob(answerId, requestId, maxRetryCount);
  }

  public void markSending(LocalDateTime attemptedAt) {
    this.status = AnswerAnalysisJobStatus.SENDING;
    this.lastAttemptAt = attemptedAt;
    this.lastError = null;
  }

  public void markSent(LocalDateTime sentAt) {
    this.status = AnswerAnalysisJobStatus.SENT;
    this.sentAt = sentAt;
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
    this.status = AnswerAnalysisJobStatus.RETRY_WAITING;
    this.lastError = errorMessage;
    this.nextRetryAt = nextRetryAt;
  }

  public void markFailed(String errorMessage) {
    this.status = AnswerAnalysisJobStatus.FAILED;
    this.lastError = errorMessage;
    this.nextRetryAt = null;
  }

  public void complete() {
    this.status = AnswerAnalysisJobStatus.COMPLETED;
    this.completedAt = LocalDateTime.now();
    this.nextRetryAt = null;
    this.lastError = null;
  }

  public boolean isTerminal() {
    return status == AnswerAnalysisJobStatus.COMPLETED || status == AnswerAnalysisJobStatus.FAILED;
  }
}
