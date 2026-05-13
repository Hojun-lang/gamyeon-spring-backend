package com.gamyeon.feedback.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.feedback.application.FeedbackCallbackProperties;
import com.gamyeon.feedback.application.exception.FeedbackSerializationException;
import com.gamyeon.feedback.application.port.in.FeedbackWebhookCommand;
import com.gamyeon.feedback.application.port.in.FeedbackWebhookUseCase;
import com.gamyeon.feedback.domain.FeedbackCallbackJob;
import com.gamyeon.feedback.domain.FeedbackCallbackJobRepository;
import com.gamyeon.feedback.domain.FeedbackCallbackJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackWebhookService implements FeedbackWebhookUseCase {

  private final FeedbackCallbackJobRepository feedbackCallbackJobRepository;
  private final FeedbackCallbackProperties feedbackCallbackProperties;
  private final ObjectMapper objectMapper;

  @Override
  public void handleWebhook(FeedbackWebhookCommand command) {
    Long questionSetId = command.intvQuestionId();
    String requestId = normalize(command.requestId());
    String rawPayload = serialize(command);

    if (requestId != null
        && feedbackCallbackJobRepository
            .findByRequestId(requestId)
            .map(job -> handleExistingJob(job, requestId, rawPayload))
            .orElse(false)) {
      return;
    }

    if (feedbackCallbackJobRepository
        .findByQuestionSetId(questionSetId)
        .map(job -> handleExistingJob(job, requestId, rawPayload))
        .orElse(false)) {
      return;
    }

    FeedbackCallbackJob job =
        FeedbackCallbackJob.create(
            questionSetId, requestId, rawPayload, feedbackCallbackProperties.getMaxRetryCount());

    try {
      feedbackCallbackJobRepository.save(job);
      log.info(
          "[Feedback] callback job 접수 완료 | jobId={}, requestId={}, questionSetId={}",
          job.getId(),
          requestId,
          questionSetId);
    } catch (DataIntegrityViolationException e) {
      log.info(
          "[Feedback] 동시 중복 callback job 접수 무시 | requestId={}, questionSetId={}",
          requestId,
          questionSetId);
    }
  }

  private String serialize(FeedbackWebhookCommand request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      throw new FeedbackSerializationException("Webhook body 직렬화 실패", e);
    }
  }

  private String normalize(String requestId) {
    if (requestId == null || requestId.isBlank()) {
      return null;
    }
    return requestId;
  }

  private boolean handleExistingJob(FeedbackCallbackJob job, String requestId, String rawPayload) {
    if (job.getStatus() == FeedbackCallbackJobStatus.FAILED) {
      job.reopen(requestId, rawPayload, feedbackCallbackProperties.getMaxRetryCount());
      feedbackCallbackJobRepository.save(job);
      log.info(
          "[Feedback] FAILED callback job 재접수 | jobId={}, requestId={}, questionSetId={}",
          job.getId(),
          job.getRequestId(),
          job.getQuestionSetId());
      return true;
    }

    log.info(
        "[Feedback] 중복 callback job 접수 무시 | jobId={}, requestId={}, questionSetId={}, status={}",
        job.getId(),
        requestId,
        job.getQuestionSetId(),
        job.getStatus());
    return true;
  }
}
