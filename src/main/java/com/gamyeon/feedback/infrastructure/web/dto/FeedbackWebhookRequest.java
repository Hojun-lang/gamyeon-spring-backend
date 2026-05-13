package com.gamyeon.feedback.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gamyeon.feedback.application.port.in.FeedbackWebhookCommand;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FeedbackWebhookRequest(
    @JsonProperty("request_id") String requestId,
    @NotNull @JsonProperty("intv_question_id") Long intvQuestionId, // = question_set_id
    @NotNull String status,

    // ── LLM 산출 필드 (FAILED 시 null) ─────────────────────────────────
    @JsonProperty("logic_score") Integer logicScore,
    @JsonProperty("answer_composition_score") Integer answerCompositionScore,
    Integer reliability,
    String characteristic,
    @JsonProperty("answer_summary") String answerSummary,
    String strength,
    String improvement,
    @JsonProperty("feedback_badges") List<String> feedbackBadges,

    // ── media 수치 필드 (항상 존재) ─────────────────────────────────────
    @JsonProperty("gaze_score") Integer gazeScore,
    @JsonProperty("time_score") Integer timeScore,
    @JsonProperty("answer_duration_ms") Integer answerDurationMs,
    @JsonProperty("keyword_count") Integer keywordCount) {

  public FeedbackWebhookCommand toCommand() {
    return new FeedbackWebhookCommand(
        requestId,
        intvQuestionId,
        status,
        logicScore,
        answerCompositionScore,
        reliability,
        characteristic,
        answerSummary,
        strength,
        improvement,
        feedbackBadges,
        gazeScore,
        timeScore,
        answerDurationMs,
        keywordCount);
  }
}
