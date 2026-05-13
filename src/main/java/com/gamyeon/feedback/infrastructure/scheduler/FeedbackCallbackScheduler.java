package com.gamyeon.feedback.infrastructure.scheduler;

import com.gamyeon.feedback.application.FeedbackCallbackProperties;
import com.gamyeon.feedback.application.service.FeedbackCallbackProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackCallbackScheduler {

  private final FeedbackCallbackProcessingService feedbackCallbackProcessingService;
  private final FeedbackCallbackProperties feedbackCallbackProperties;

  @Scheduled(fixedDelayString = "${feedback.callback.scheduler-fixed-delay-ms:60000}")
  public void processPendingJobs() {
    int recoveredCount =
        feedbackCallbackProcessingService.recoverStaleProcessingJobs(
            feedbackCallbackProperties.getProcessingBatchSize(),
            feedbackCallbackProperties.getProcessingRecoveryTimeoutMs());
    if (recoveredCount > 0) {
      log.warn("[Feedback] recovered {} stale PROCESSING callback jobs.", recoveredCount);
    }

    int processedCount =
        feedbackCallbackProcessingService.processPendingJobs(
            feedbackCallbackProperties.getProcessingBatchSize());

    if (processedCount > 0) {
      log.info("[Feedback] processed {} pending callback jobs.", processedCount);
    }
  }
}
