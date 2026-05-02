package com.gamyeon.answer.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamyeon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "answers")
@Getter
public class Answer extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long intvId;

  @Column(nullable = false)
  private Long questionSetId;

  @Column(nullable = false)
  private String originalFileName;

  @Column(nullable = false)
  private String fileKey;

  @Column(nullable = false)
  private String fileUrl;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private Long fileSizeBytes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AnswerStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private JsonNode content;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  protected Answer() {}

  private Answer(
      Long intvId,
      Long questionSetId,
      String originalFileName,
      String fileKey,
      String fileUrl,
      String contentType,
      Long fileSizeBytes) {
    this.intvId = intvId;
    this.questionSetId = questionSetId;
    this.originalFileName = originalFileName;
    this.fileKey = fileKey;
    this.fileUrl = fileUrl;
    this.contentType = contentType;
    this.fileSizeBytes = fileSizeBytes;
    this.status = AnswerStatus.UPLOADED;
  }

  public static Answer create(
      Long intvId,
      Long questionSetId,
      String originalFileName,
      String fileKey,
      String fileUrl,
      String contentType,
      Long fileSizeBytes) {
    return new Answer(
        intvId, questionSetId, originalFileName, fileKey, fileUrl, contentType, fileSizeBytes);
  }

  public void markSttPending() {
    this.status = AnswerStatus.STT_PENDING;
    this.errorMessage = null;
  }

  public void markSttProcessing() {
    this.status = AnswerStatus.STT_PROCESSING;
    this.errorMessage = null;
  }

  public void completeStt(JsonNode content) {
    this.status = AnswerStatus.STT_COMPLETED;
    this.content = content;
    this.errorMessage = null;
  }

  public void failStt(String errorMessage, JsonNode content) {
    this.status = AnswerStatus.STT_FAILED;
    this.content = content;
    this.errorMessage = errorMessage;
  }
}
