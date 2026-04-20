package com.gamyeon.answer.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamyeon.answer.application.port.in.SendAnswerGazeSegmentUseCase;
import com.gamyeon.answer.domain.AnswerSuccessCode;
import com.gamyeon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AnswerGazeController {

  private final SendAnswerGazeSegmentUseCase sendAnswerGazeSegmentUseCase;

  @PostMapping("/api/v1/intvs/{questionSetId}/gaze")
  public ResponseEntity<ApiResponse<Void>> sendGazeSegment(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long questionSetId,
      @RequestBody JsonNode requestBody) {
    log.info("body: {}", requestBody.toString());
    sendAnswerGazeSegmentUseCase.send(
        new com.gamyeon.answer.application.port.in.SendAnswerGazeSegmentCommand(
            userId, questionSetId, requestBody));
    return ApiResponse.success(AnswerSuccessCode.ANSWER_GAZE_SEGMENT_RECEIVED);
  }
}
