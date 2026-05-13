package com.gamyeon.feedback.domain;

import java.time.LocalDateTime;

public class Feedback {

  private Long id;
  private Long intvId;
  private Long questionSetId;
  private FeedbackStatus status;
  private String content; // jsonb 원본 (직렬화된 JSON 문자열)
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // ── 팩토리: IN_PROGRESS 초기 생성 ──────────────────────────────────────
  public static Feedback createInProgress(Long intvId, Long questionSetId, String contentJson) {
    Feedback f = new Feedback();
    f.intvId = intvId;
    f.questionSetId = questionSetId;
    f.status = FeedbackStatus.IN_PROGRESS;
    f.content = contentJson;
    f.createdAt = LocalDateTime.now();
    f.updatedAt = f.createdAt;
    return f;
  }

  public static Feedback createCompleted(
      Long intvId, Long questionSetId, FeedbackStatus finalStatus, String contentJson) {
    validateFinalStatus(finalStatus);

    Feedback f = new Feedback();
    f.intvId = intvId;
    f.questionSetId = questionSetId;
    f.status = finalStatus;
    f.content = contentJson;
    f.createdAt = LocalDateTime.now();
    f.updatedAt = f.createdAt;
    return f;
  }

  public static Feedback restore(
      Long id,
      Long intvId,
      Long questionSetId,
      FeedbackStatus status,
      String content,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    Feedback f = new Feedback();
    f.id = id;
    f.intvId = intvId;
    f.questionSetId = questionSetId;
    f.status = status;
    f.content = content;
    f.createdAt = createdAt;
    f.updatedAt = updatedAt;
    return f;
  }

  // ── 도메인 행위: 평가 완료 처리 ────────────────────────────────────────
  public void complete(FeedbackStatus finalStatus, String updatedContentJson) {
    validateFinalStatus(finalStatus);
    if (this.status != FeedbackStatus.IN_PROGRESS) {
      throw new IllegalStateException("IN_PROGRESS 상태에서만 완료 처리가 가능합니다.");
    }
    this.status = finalStatus;
    this.content = updatedContentJson;
    this.updatedAt = LocalDateTime.now();
  }

  // ── Getters (Setter 없음 — 불변 의도) ──────────────────────────────────
  public Long getId() {
    return id;
  }

  public Long getIntvId() {
    return intvId;
  }

  public Long getQuestionSetId() {
    return questionSetId;
  }

  public FeedbackStatus getStatus() {
    return status;
  }

  public String getContent() {
    return content;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  // persistence adapter 에서만 id 주입 허용
  public void assignId(Long id) {
    if (this.id != null) throw new IllegalStateException("ID는 한 번만 할당될 수 있습니다.");
    this.id = id;
  }

  private static void validateFinalStatus(FeedbackStatus status) {
    if (status == FeedbackStatus.IN_PROGRESS) {
      throw new IllegalArgumentException("최종 Feedback 상태는 IN_PROGRESS일 수 없습니다.");
    }
  }
}
