package com.gamyeon.question.adaptor.in.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gamyeon.question.application.port.in.QuestionSetItemResult;
import org.junit.jupiter.api.Test;

class QuestionSetItemResponseTest {

  @Test
  void includesQuestionOrder() {
    QuestionSetItemResult result = new QuestionSetItemResult(10L, "자기소개를 해주세요.", 3);

    QuestionSetItemResponse response = QuestionSetItemResponse.from(result);

    assertEquals(10L, response.questionSetId());
    assertEquals("자기소개를 해주세요.", response.content());
    assertEquals(3, response.questionOrder());
  }
}
