package com.gamyeon.answer.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamyeon.answer.application.port.in.HandleAnswerSttCallbackCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AnswerSttCallbackRequest(
    String requestId,
    @NotNull Long intvId,
    @NotNull Long questionSetId,
    Boolean degraded,
    @Valid AnswerText answerText,
    String errorMessage) {

  public HandleAnswerSttCallbackCommand toCommand(JsonNode callbackPayload) {
    return new HandleAnswerSttCallbackCommand(
        requestId, intvId, questionSetId, callbackPayload, errorMessage);
  }

  public record AnswerText(
      String rawTranscript,
      String phoneticTranscript,
      String correctedTranscript,
      List<Correction> corrections,
      List<WordTimestamp> wordTimestamps) {}

  public record Correction(
      String original, String corrected, Integer position, Double confidence) {}

  public record WordTimestamp(String word, Double start, Double end, Double probability) {}
}
