package com.gamyeon.feedback.application.port.in;

public interface FeedbackWebhookUseCase {
  void handleWebhook(FeedbackWebhookCommand command);
}
