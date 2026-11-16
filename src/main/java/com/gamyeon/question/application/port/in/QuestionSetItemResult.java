package com.gamyeon.question.application.port.in;

import com.gamyeon.question.domain.QuestionSet;

public record QuestionSetItemResult(Long id, String content, Integer questionOrder) {
  public static QuestionSetItemResult from(QuestionSet questionSet) {
    return new QuestionSetItemResult(
        questionSet.getId(), questionSet.getContent(), questionSet.getQuestionOrder());
  }
}
