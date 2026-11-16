package com.gamyeon.intv.presentation.dto.response;

import com.gamyeon.intv.application.dto.result.ResumeContextInfo;
import java.util.List;

public record ResumeContextResponse(
    Long intvId,
    String status,
    int totalQuestionCount,
    int answeredCount,
    boolean completed,
    Long nextQuestionSetId,
    Integer nextQuestionOrder,
    String nextQuestionContent,
    List<QuestionProgressResponse> questions) {

  public static ResumeContextResponse from(ResumeContextInfo info) {
    return new ResumeContextResponse(
        info.intvId(),
        info.status().name(),
        info.totalQuestionCount(),
        info.answeredCount(),
        info.completed(),
        info.nextQuestionSetId(),
        info.nextQuestionOrder(),
        info.nextQuestionContent(),
        info.questions().stream().map(QuestionProgressResponse::from).toList());
  }
}
