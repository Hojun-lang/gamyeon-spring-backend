package com.gamyeon.feedback.infrastructure.persistence;

import com.gamyeon.feedback.domain.Feedback;
import com.gamyeon.feedback.domain.FeedbackStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "FEEDBACKS",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_feedbacks_question_set_id", columnNames = "question_set_id"))
public class FeedbackEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "intv_id", nullable = false)
  private Long intvId;

  @Column(name = "question_set_id", nullable = false)
  private Long questionSetId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private FeedbackStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "content", columnDefinition = "jsonb")
  private String content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // ── 도메인 → 엔티티 변환 ────────────────────────────────────────────
  public static FeedbackEntity fromDomain(Feedback domain) {
    FeedbackEntity e = new FeedbackEntity();
    e.id = domain.getId();
    e.intvId = domain.getIntvId();
    e.questionSetId = domain.getQuestionSetId();
    e.status = domain.getStatus();
    e.content = domain.getContent();
    e.createdAt = domain.getCreatedAt();
    e.updatedAt = domain.getUpdatedAt();
    return e;
  }

  // ── 엔티티 → 도메인 변환 ────────────────────────────────────────────
  public Feedback toDomain() {
    return Feedback.restore(id, intvId, questionSetId, status, content, createdAt, updatedAt);
  }

  // JPA 기본 생성자
  protected FeedbackEntity() {}

  // FeedbackEntity에 Getter 추가
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
}
