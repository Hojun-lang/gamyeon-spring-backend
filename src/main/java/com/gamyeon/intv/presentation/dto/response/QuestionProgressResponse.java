package com.gamyeon.intv.presentation.dto.response;

import com.gamyeon.intv.application.dto.result.QuestionProgressInfo;

public record QuestionProgressResponse(
    Long questionSetId,
    Integer questionOrder,
    String content,
    boolean answered,
    Long answerId,
    String answerStatus) {

  public static QuestionProgressResponse from(QuestionProgressInfo info) {
    return new QuestionProgressResponse(
        info.questionSetId(),
        info.questionOrder(),
        info.content(),
        info.answered(),
        info.answerId(),
        info.answerStatus() == null ? null : info.answerStatus().name());
  }
}
