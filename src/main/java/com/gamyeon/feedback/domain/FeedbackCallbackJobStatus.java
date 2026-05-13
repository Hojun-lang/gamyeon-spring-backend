package com.gamyeon.feedback.domain;

public enum FeedbackCallbackJobStatus {
  RECEIVED,
  PROCESSING,
  PROCESSED,
  RETRY_WAITING,
  FAILED
}
