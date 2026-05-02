package com.gamyeon.answer.domain;

public enum AnswerAnalysisJobStatus {
  QUEUED,
  SENDING,
  SENT,
  RETRY_WAITING,
  COMPLETED,
  FAILED
}
