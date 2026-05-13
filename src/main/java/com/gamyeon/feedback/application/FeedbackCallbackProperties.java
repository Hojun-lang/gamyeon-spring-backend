package com.gamyeon.feedback.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feedback.callback")
public class FeedbackCallbackProperties {

  private int processingBatchSize = 10;
  private int maxRetryCount = 5;
  private long processingRecoveryTimeoutMs = 300_000L;

  public int getProcessingBatchSize() {
    return processingBatchSize;
  }

  public void setProcessingBatchSize(int processingBatchSize) {
    this.processingBatchSize = processingBatchSize;
  }

  public int getMaxRetryCount() {
    return maxRetryCount;
  }

  public void setMaxRetryCount(int maxRetryCount) {
    this.maxRetryCount = maxRetryCount;
  }

  public long getProcessingRecoveryTimeoutMs() {
    return processingRecoveryTimeoutMs;
  }

  public void setProcessingRecoveryTimeoutMs(long processingRecoveryTimeoutMs) {
    this.processingRecoveryTimeoutMs = processingRecoveryTimeoutMs;
  }
}
