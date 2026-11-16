package com.gamyeon.intv.application.dto.result;

import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerStatus;
import com.gamyeon.question.domain.QuestionSet;

public record QuestionProgressInfo(
    Long questionSetId,
    Integer questionOrder,
    String content,
    boolean answered,
    Long answerId,
    AnswerStatus answerStatus) {

  public static QuestionProgressInfo of(QuestionSet questionSet, Answer answer) {
    return new QuestionProgressInfo(
        questionSet.getId(),
        questionSet.getQuestionOrder(),
        questionSet.getContent(),
        answer != null,
        answer == null ? null : answer.getId(),
        answer == null ? null : answer.getStatus());
  }
}
