package com.gamyeon.feedback.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FeedbackWebhookCommand(
    @JsonProperty("request_id") String requestId,
    @JsonProperty("intv_question_id") Long intvQuestionId,
    String status,
    @JsonProperty("logic_score") Integer logicScore,
    @JsonProperty("answer_composition_score") Integer answerCompositionScore,
    Integer reliability,
    String characteristic,
    @JsonProperty("answer_summary") String answerSummary,
    String strength,
    String improvement,
    @JsonProperty("feedback_badges") List<String> feedbackBadges,
    @JsonProperty("gaze_score") Integer gazeScore,
    @JsonProperty("time_score") Integer timeScore,
    @JsonProperty("answer_duration_ms") Integer answerDurationMs,
    @JsonProperty("keyword_count") Integer keywordCount) {}
