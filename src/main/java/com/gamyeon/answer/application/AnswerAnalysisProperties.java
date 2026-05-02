package com.gamyeon.answer.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "answer.analysis")
public class AnswerAnalysisProperties {

  private int dispatchBatchSize = 10;
  private int maxRetryCount = 5;
  private long schedulerFixedDelayMs = 60_000L;
  private long sendingRecoveryTimeoutMs = 300_000L;
}
