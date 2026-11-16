package com.gamyeon.question.domain;

import com.gamyeon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
    name = "question_set",
    uniqueConstraints = @UniqueConstraint(columnNames = {"intv_id", "question_order"}))
@Getter
public class QuestionSet extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long intvId;

  private String content;

  @Column(nullable = false)
  private Integer questionOrder;

  protected QuestionSet() {}

  private QuestionSet(Long intvId, String content, Integer questionOrder) {
    this.intvId = intvId;
    this.content = content;
    this.questionOrder = questionOrder;
  }

  public static QuestionSet create(Long intvId, String content, Integer questionOrder) {
    return new QuestionSet(intvId, content, questionOrder);
  }
}
