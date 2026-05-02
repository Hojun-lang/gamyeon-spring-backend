package com.gamyeon.answer.application.port.out;

public record AnswerAnalysisTarget(
    String requestId,
    Long answerId,
    Long intvId,
    Long questionSetId,
    String questionContent,
    String mediaFileKey) {}
