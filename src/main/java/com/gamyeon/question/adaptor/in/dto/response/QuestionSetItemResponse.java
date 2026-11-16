package com.gamyeon.question.adaptor.in.dto.response;

import com.gamyeon.question.application.port.in.QuestionSetItemResult;

public record QuestionSetItemResponse(Long questionSetId, String content, Integer questionOrder) {
  public static QuestionSetItemResponse from(QuestionSetItemResult result) {
    return new QuestionSetItemResponse(result.id(), result.content(), result.questionOrder());
  }
}
