package com.gamyeon.feedback.application.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackCallbackProcessingService {

  private final FeedbackCallbackProcessingTransactionService transactionService;

  public int processPendingJobs(int batchSize) {
    int processedCount = 0;

    for (int index = 0; index < batchSize; index++) {
      boolean processed = transactionService.processNextJob();
      if (!processed) {
        break;
      }
      processedCount++;
    }

    return processedCount;
  }

  public int recoverStaleProcessingJobs(int batchSize, long staleTimeoutMs) {
    int recoveredCount = 0;
    LocalDateTime staleBefore = LocalDateTime.now().minusNanos(staleTimeoutMs * 1_000_000L);

    for (int index = 0; index < batchSize; index++) {
      boolean recovered = transactionService.recoverNextStaleProcessingJob(staleBefore);
      if (!recovered) {
        break;
      }
      recoveredCount++;
    }

    return recoveredCount;
  }
}
