package com.gamyeon.answer.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.answer.application.port.in.HandleAnswerSttCallbackUseCase;
import com.gamyeon.answer.domain.AnswerSuccessCode;
import com.gamyeon.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AnswerCallbackController {

  private final HandleAnswerSttCallbackUseCase handleAnswerSttCallbackUseCase;
  private final ObjectMapper objectMapper;

  @PostMapping("/internal/v1/answers/stt/callback")
  public ResponseEntity<ApiResponse<Void>> handleSttCallback(
      @Valid @RequestBody AnswerSttCallbackRequest request) {
    log.info(
        "Received STT callback request. requestId={}, intvId={}, questionSetId={}, hasError={}",
        request.requestId(),
        request.intvId(),
        request.questionSetId(),
        request.errorMessage() != null && !request.errorMessage().isBlank());
    handleAnswerSttCallbackUseCase.handle(request.toCommand(objectMapper.valueToTree(request)));
    return ApiResponse.success(AnswerSuccessCode.ANSWER_STT_CALLBACK_PROCESSED);
  }
}
