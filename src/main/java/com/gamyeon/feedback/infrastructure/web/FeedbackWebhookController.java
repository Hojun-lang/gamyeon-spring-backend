package com.gamyeon.feedback.infrastructure.web;

import com.gamyeon.feedback.application.port.in.FeedbackWebhookUseCase;
import com.gamyeon.feedback.common.ApiResponse;
import com.gamyeon.feedback.infrastructure.web.dto.FeedbackWebhookRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/internal/v1/feedbacks")
@RequiredArgsConstructor
public class FeedbackWebhookController {

  private final FeedbackWebhookUseCase feedbackWebhookUseCase;

  /** Python AI 서버로부터 문항별 평가 결과 수신 POST /internal/v1/feedbacks/callback */
  @PostMapping("/callback")
  public ResponseEntity<ApiResponse<Void>> receiveWebhook(
      @Valid @RequestBody FeedbackWebhookRequest request) {
    log.info(
        "[Webhook 수신] requestId={}, questionSetId={}, status={}",
        request.requestId(),
        request.intvQuestionId(),
        request.status());

    feedbackWebhookUseCase.handleWebhook(request.toCommand());

    return ResponseEntity.ok(ApiResponse.success("FDBK-S000", "success"));
  }
}
