package com.gamyeon.answer.application;

import com.gamyeon.answer.application.port.out.AnswerAnalysisTarget;
import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerAnalysisJob;
import com.gamyeon.answer.domain.AnswerAnalysisJobRepository;
import com.gamyeon.answer.domain.AnswerErrorCode;
import com.gamyeon.answer.domain.AnswerException;
import com.gamyeon.answer.domain.AnswerRepository;
import feign.FeignException;
import feign.RetryableException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerAnalysisDispatchTransactionService {

  private static final long[] RETRY_DELAY_MINUTES = {1, 3, 10, 30, 60};

  private final AnswerRepository answerRepository;
  private final AnswerAnalysisJobRepository answerAnalysisJobRepository;
  private final LoadQuestionSetPort loadQuestionSetPort;

  @Transactional
  public Optional<AnswerAnalysisDispatchCandidate> claimNextDispatchCandidate() {
    LocalDateTime now = LocalDateTime.now();
    Optional<AnswerAnalysisJob> candidateJob =
        answerAnalysisJobRepository.findNextDispatchCandidate(now);

    if (candidateJob.isEmpty()) {
      return Optional.empty();
    }

    AnswerAnalysisJob job = candidateJob.get();
    Answer answer =
        answerRepository
            .findById(job.getAnswerId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    String questionContent = loadQuestionSetPort.getQuestionContent(answer.getQuestionSetId());
    job.markSending(now);
    answerAnalysisJobRepository.save(job);

    AnswerAnalysisTarget target =
        new AnswerAnalysisTarget(
            job.getRequestId(),
            answer.getId(),
            answer.getIntvId(),
            answer.getQuestionSetId(),
            questionContent,
            answer.getFileKey());

    return Optional.of(
        new AnswerAnalysisDispatchCandidate(
            job.getId(), answer.getId(), job.getRequestId(), target));
  }

  @Transactional
  public void markDispatchSuccess(Long jobId) {
    AnswerAnalysisJob job = findJob(jobId);
    Answer answer = findAnswer(job.getAnswerId());

    job.markSent(LocalDateTime.now());
    answer.markSttProcessing();

    answerAnalysisJobRepository.save(job);
    answerRepository.save(answer);
  }

  @Transactional
  public void handleDispatchFailure(Long jobId, Exception exception) {
    AnswerAnalysisJob job = findJob(jobId);
    Answer answer = findAnswer(job.getAnswerId());

    String errorMessage = summarizeException(exception);
    int retryCount = job.incrementRetryCount();

    if (isRetryable(exception) && job.canRetry()) {
      LocalDateTime nextRetryAt = calculateNextRetryAt(retryCount);
      job.scheduleRetry(errorMessage, nextRetryAt);
      answer.markSttPending();
      answerAnalysisJobRepository.save(job);
      answerRepository.save(answer);
      log.warn(
          "Retrying STT analysis job after dispatch failure. jobId={}, answerId={}, retryCount={}, nextRetryAt={}, error={}",
          job.getId(),
          answer.getId(),
          retryCount,
          nextRetryAt,
          errorMessage,
          exception);
      return;
    }

    job.markFailed(errorMessage);
    answer.failStt(errorMessage, null);
    answerAnalysisJobRepository.save(job);
    answerRepository.save(answer);
    log.error(
        "STT analysis job failed permanently. jobId={}, answerId={}, retryCount={}, error={}",
        job.getId(),
        answer.getId(),
        retryCount,
        errorMessage,
        exception);
  }

  @Transactional
  public boolean recoverNextStaleSendingJob(LocalDateTime staleBefore) {
    Optional<AnswerAnalysisJob> candidateJob =
        answerAnalysisJobRepository.findNextStaleSendingJob(staleBefore);

    if (candidateJob.isEmpty()) {
      return false;
    }

    AnswerAnalysisJob job = candidateJob.get();
    Answer answer = findAnswer(job.getAnswerId());
    int retryCount = job.incrementRetryCount();
    String errorMessage =
        "Recovered stale SENDING job. lastAttemptAt=%s".formatted(job.getLastAttemptAt());

    if (job.canRetry()) {
      LocalDateTime nextRetryAt = calculateNextRetryAt(retryCount);
      job.scheduleRetry(errorMessage, nextRetryAt);
      answer.markSttPending();
      answerAnalysisJobRepository.save(job);
      answerRepository.save(answer);
      log.warn(
          "Recovered stale SENDING STT analysis job for retry. jobId={}, answerId={}, retryCount={}, nextRetryAt={}",
          job.getId(),
          answer.getId(),
          retryCount,
          nextRetryAt);
      return true;
    }

    job.markFailed(errorMessage);
    answer.failStt(errorMessage, null);
    answerAnalysisJobRepository.save(job);
    answerRepository.save(answer);
    log.error(
        "Recovered stale SENDING STT analysis job as failed. jobId={}, answerId={}, retryCount={}",
        job.getId(),
        answer.getId(),
        retryCount);
    return true;
  }

  private AnswerAnalysisJob findJob(Long jobId) {
    return answerAnalysisJobRepository
        .findById(jobId)
        .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));
  }

  private Answer findAnswer(Long answerId) {
    return answerRepository
        .findById(answerId)
        .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));
  }

  private boolean isRetryable(Exception exception) {
    if (exception instanceof RetryableException) {
      return true;
    }

    if (exception instanceof FeignException feignException) {
      return feignException.status() == 429 || feignException.status() >= 500;
    }

    return false;
  }

  private LocalDateTime calculateNextRetryAt(int retryCount) {
    int index = Math.min(retryCount - 1, RETRY_DELAY_MINUTES.length - 1);
    long jitterSeconds = ThreadLocalRandom.current().nextLong(10, 31);
    return LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES[index]).plusSeconds(jitterSeconds);
  }

  private String summarizeException(Exception exception) {
    if (exception instanceof FeignException feignException) {
      return "Feign status=%s message=%s"
          .formatted(feignException.status(), feignException.getMessage());
    }

    return exception.getClass().getSimpleName() + ": " + exception.getMessage();
  }
}
