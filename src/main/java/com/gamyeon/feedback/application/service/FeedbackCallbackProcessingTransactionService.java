package com.gamyeon.feedback.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.feedback.application.exception.QuestionSetNotFoundException;
import com.gamyeon.feedback.application.port.in.FeedbackWebhookCommand;
import com.gamyeon.feedback.application.port.out.LoadQuestionSetPort;
import com.gamyeon.feedback.application.port.out.SaveFeedbackPort;
import com.gamyeon.feedback.domain.Feedback;
import com.gamyeon.feedback.domain.FeedbackCallbackJob;
import com.gamyeon.feedback.domain.FeedbackCallbackJobRepository;
import com.gamyeon.feedback.domain.FeedbackStatus;
import com.gamyeon.feedback.domain.event.FeedbackSavedEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackCallbackProcessingTransactionService {

  private static final long[] RETRY_DELAY_MINUTES = {1, 3, 10, 30, 60};

  private final FeedbackCallbackJobRepository feedbackCallbackJobRepository;
  private final SaveFeedbackPort saveFeedbackPort;
  private final LoadQuestionSetPort loadQuestionSetPort;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public boolean processNextJob() {
    Optional<FeedbackCallbackJob> candidate =
        feedbackCallbackJobRepository.findNextProcessingCandidate(LocalDateTime.now());

    if (candidate.isEmpty()) {
      return false;
    }

    FeedbackCallbackJob job = candidate.get();
    job.markProcessing(LocalDateTime.now());
    feedbackCallbackJobRepository.save(job);

    try {
      process(job);
    } catch (Exception e) {
      handleProcessingFailure(job, e);
    }

    return true;
  }

  @Transactional
  public boolean recoverNextStaleProcessingJob(LocalDateTime staleBefore) {
    Optional<FeedbackCallbackJob> candidate =
        feedbackCallbackJobRepository.findNextStaleProcessingJob(staleBefore);

    if (candidate.isEmpty()) {
      return false;
    }

    FeedbackCallbackJob job = candidate.get();
    int retryCount = job.incrementRetryCount();
    String errorMessage =
        "Recovered stale PROCESSING callback job. lastAttemptAt=%s"
            .formatted(job.getLastAttemptAt());

    if (job.canRetry()) {
      LocalDateTime nextRetryAt = calculateNextRetryAt(retryCount);
      job.scheduleRetry(errorMessage, nextRetryAt);
      feedbackCallbackJobRepository.save(job);
      log.warn(
          "[Feedback] stale PROCESSING callback job 재시도 예약 | jobId={}, retryCount={}, nextRetryAt={}",
          job.getId(),
          retryCount,
          nextRetryAt);
      return true;
    }

    job.markFailed(errorMessage);
    feedbackCallbackJobRepository.save(job);
    log.error(
        "[Feedback] stale PROCESSING callback job 최종 실패 | jobId={}, retryCount={}",
        job.getId(),
        retryCount);
    return true;
  }

  private void process(FeedbackCallbackJob job) {
    FeedbackWebhookCommand request = deserialize(job.getRawPayload());
    Long questionSetId = request.intvQuestionId();
    Long intvId =
        loadQuestionSetPort
            .findIntvIdById(questionSetId)
            .orElseThrow(() -> new QuestionSetNotFoundException(questionSetId));

    if (saveFeedbackPort.existsByQuestionSetId(questionSetId)) {
      job.markProcessed(intvId);
      feedbackCallbackJobRepository.save(job);
      log.info(
          "[Feedback] 중복 callback 처리 완료 처리 | jobId={}, questionSetId={}",
          job.getId(),
          questionSetId);
      return;
    }

    FeedbackStatus finalStatus = FeedbackStatus.fromWebhook(request.status());
    Feedback feedback =
        Feedback.createCompleted(intvId, questionSetId, finalStatus, job.getRawPayload());
    boolean saved = saveFeedbackPort.saveIfAbsent(feedback);

    job.markProcessed(intvId);
    feedbackCallbackJobRepository.save(job);

    if (saved) {
      eventPublisher.publishEvent(new FeedbackSavedEvent(intvId, questionSetId, finalStatus));
      log.info(
          "[Feedback] feedback 저장 및 이벤트 발행 완료 | jobId={}, feedbackId={}, intvId={}, questionSetId={}, status={}",
          job.getId(),
          feedback.getId(),
          intvId,
          questionSetId,
          finalStatus);
      return;
    }

    log.info(
        "[Feedback] 동시 중복 feedback 저장 감지, 이벤트 미발행 | jobId={}, questionSetId={}",
        job.getId(),
        questionSetId);
  }

  private void handleProcessingFailure(FeedbackCallbackJob job, Exception exception) {
    String errorMessage = summarizeException(exception);
    int retryCount = job.incrementRetryCount();

    if (isRetryable(exception) && job.canRetry()) {
      LocalDateTime nextRetryAt = calculateNextRetryAt(retryCount);
      job.scheduleRetry(errorMessage, nextRetryAt);
      feedbackCallbackJobRepository.save(job);
      log.warn(
          "[Feedback] callback job 재시도 예약 | jobId={}, retryCount={}, nextRetryAt={}, error={}",
          job.getId(),
          retryCount,
          nextRetryAt,
          errorMessage,
          exception);
      return;
    }

    job.markFailed(errorMessage);
    feedbackCallbackJobRepository.save(job);
    log.error(
        "[Feedback] callback job 최종 실패 | jobId={}, retryCount={}, error={}",
        job.getId(),
        retryCount,
        errorMessage,
        exception);
  }

  private FeedbackWebhookCommand deserialize(String rawPayload) {
    try {
      return objectMapper.readValue(rawPayload, FeedbackWebhookCommand.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Feedback callback payload 역직렬화 실패", e);
    }
  }

  private boolean isRetryable(Exception exception) {
    return exception instanceof CannotAcquireLockException
        || exception instanceof PessimisticLockingFailureException;
  }

  private LocalDateTime calculateNextRetryAt(int retryCount) {
    int index = Math.min(retryCount - 1, RETRY_DELAY_MINUTES.length - 1);
    long jitterSeconds = ThreadLocalRandom.current().nextLong(10, 31);
    return LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES[index]).plusSeconds(jitterSeconds);
  }

  private String summarizeException(Exception exception) {
    return exception.getClass().getSimpleName() + ": " + exception.getMessage();
  }
}
