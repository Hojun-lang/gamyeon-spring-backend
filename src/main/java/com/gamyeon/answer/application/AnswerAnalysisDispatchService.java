package com.gamyeon.answer.application;

import com.gamyeon.answer.application.port.out.RequestAnswerSttAnalysisPort;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerAnalysisDispatchService {

  private final AnswerAnalysisDispatchTransactionService transactionService;
  private final RequestAnswerSttAnalysisPort requestAnswerSttAnalysisPort;

  public int dispatchPendingJobs(int batchSize) {
    int dispatchedCount = 0;

    for (int index = 0; index < batchSize; index++) {
      Optional<AnswerAnalysisDispatchCandidate> candidate =
          transactionService.claimNextDispatchCandidate();
      if (candidate.isEmpty()) {
        break;
      }

      dispatch(candidate.get());
      dispatchedCount++;
    }

    return dispatchedCount;
  }

  public int recoverStaleSendingJobs(int batchSize, long staleTimeoutMs) {
    int recoveredCount = 0;
    LocalDateTime staleBefore = LocalDateTime.now().minusNanos(staleTimeoutMs * 1_000_000L);

    for (int index = 0; index < batchSize; index++) {
      boolean recovered = transactionService.recoverNextStaleSendingJob(staleBefore);
      if (!recovered) {
        break;
      }
      recoveredCount++;
    }

    return recoveredCount;
  }

  public void dispatch(AnswerAnalysisDispatchCandidate candidate) {
    try {
      requestAnswerSttAnalysisPort.request(candidate.target());
      transactionService.markDispatchSuccess(candidate.jobId());
      log.info(
          "Dispatched STT analysis job. jobId={}, answerId={}, requestId={}",
          candidate.jobId(),
          candidate.answerId(),
          candidate.requestId());
    } catch (Exception e) {
      transactionService.handleDispatchFailure(candidate.jobId(), e);
    }
  }
}
