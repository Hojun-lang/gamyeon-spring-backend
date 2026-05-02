package com.gamyeon.answer.application;

import com.gamyeon.answer.application.port.out.AnswerAnalysisTarget;

public record AnswerAnalysisDispatchCandidate(
    Long jobId, Long answerId, String requestId, AnswerAnalysisTarget target) {}
