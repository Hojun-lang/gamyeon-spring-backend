package com.gamyeon.feedback.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.feedback.application.port.in.FeedbackWebhookCommand;
import com.gamyeon.feedback.application.port.out.LoadQuestionSetPort;
import com.gamyeon.feedback.application.port.out.SaveFeedbackPort;
import com.gamyeon.feedback.domain.Feedback;
import com.gamyeon.feedback.domain.FeedbackCallbackJob;
import com.gamyeon.feedback.domain.FeedbackCallbackJobRepository;
import com.gamyeon.feedback.domain.FeedbackCallbackJobStatus;
import com.gamyeon.feedback.domain.FeedbackStatus;
import com.gamyeon.feedback.domain.event.FeedbackSavedEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("피드백 callback processing transaction 서비스")
@ExtendWith(MockitoExtension.class)
class FeedbackCallbackProcessingTransactionServiceTest {

  @Mock private FeedbackCallbackJobRepository feedbackCallbackJobRepository;
  @Mock private SaveFeedbackPort saveFeedbackPort;
  @Mock private LoadQuestionSetPort loadQuestionSetPort;
  @Mock private ApplicationEventPublisher eventPublisher;

  private ObjectMapper objectMapper;
  private FeedbackCallbackProcessingTransactionService service;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service =
        new FeedbackCallbackProcessingTransactionService(
            feedbackCallbackJobRepository,
            saveFeedbackPort,
            loadQuestionSetPort,
            objectMapper,
            eventPublisher);
  }

  @Test
  @DisplayName("FCP-001 - 처리 가능한 job은 feedback 저장 후 PROCESSED와 이벤트 발행으로 마무리해야 한다")
  void shouldProcessReceivedJobAndPublishEvent() throws Exception {
    FeedbackCallbackJob job = callbackJob(1L, 100L, "req-1", "SUCCEED");

    given(feedbackCallbackJobRepository.findNextProcessingCandidate(any(LocalDateTime.class)))
        .willReturn(Optional.of(job));
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(loadQuestionSetPort.findIntvIdById(100L)).willReturn(Optional.of(10L));
    given(saveFeedbackPort.existsByQuestionSetId(100L)).willReturn(false);
    given(saveFeedbackPort.saveIfAbsent(any(Feedback.class))).willReturn(true);

    boolean processed = service.processNextJob();

    assertEquals(true, processed);
    assertEquals(FeedbackCallbackJobStatus.PROCESSED, job.getStatus());
    assertEquals(10L, job.getIntvId());
    assertNotNull(job.getProcessedAt());
    verify(saveFeedbackPort).saveIfAbsent(any(Feedback.class));

    ArgumentCaptor<FeedbackSavedEvent> eventCaptor =
        ArgumentCaptor.forClass(FeedbackSavedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertEquals(10L, eventCaptor.getValue().intvId());
    assertEquals(100L, eventCaptor.getValue().questionSetId());
    assertEquals(FeedbackStatus.SUCCEED, eventCaptor.getValue().status());
  }

  @Test
  @DisplayName("FCP-002 - 이미 feedback이 있으면 job만 PROCESSED 처리하고 이벤트는 발행하지 않아야 한다")
  void shouldMarkJobProcessedWithoutEventWhenFeedbackAlreadyExists() throws Exception {
    FeedbackCallbackJob job = callbackJob(1L, 100L, "req-1", "SUCCEED");

    given(feedbackCallbackJobRepository.findNextProcessingCandidate(any(LocalDateTime.class)))
        .willReturn(Optional.of(job));
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(loadQuestionSetPort.findIntvIdById(100L)).willReturn(Optional.of(10L));
    given(saveFeedbackPort.existsByQuestionSetId(100L)).willReturn(true);

    boolean processed = service.processNextJob();

    assertEquals(true, processed);
    assertEquals(FeedbackCallbackJobStatus.PROCESSED, job.getStatus());
    verify(saveFeedbackPort, never()).saveIfAbsent(any(Feedback.class));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("FCP-003 - 존재하지 않는 questionSet이면 재시도하지 않고 FAILED 처리해야 한다")
  void shouldFailPermanentlyWhenQuestionSetDoesNotExist() throws Exception {
    FeedbackCallbackJob job = callbackJob(1L, 100L, "req-1", "SUCCEED");

    given(feedbackCallbackJobRepository.findNextProcessingCandidate(any(LocalDateTime.class)))
        .willReturn(Optional.of(job));
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(loadQuestionSetPort.findIntvIdById(100L)).willReturn(Optional.empty());

    boolean processed = service.processNextJob();

    assertEquals(true, processed);
    assertEquals(FeedbackCallbackJobStatus.FAILED, job.getStatus());
    assertEquals(1, job.getRetryCount());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("FCP-004 - 오래된 PROCESSING job은 RETRY_WAITING으로 복구해야 한다")
  void shouldRecoverStaleProcessingJobAsRetryWaiting() throws Exception {
    FeedbackCallbackJob job = callbackJob(1L, 100L, "req-1", "SUCCEED");
    job.markProcessing(LocalDateTime.now().minusMinutes(10));

    given(feedbackCallbackJobRepository.findNextStaleProcessingJob(any(LocalDateTime.class)))
        .willReturn(Optional.of(job));
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    boolean recovered = service.recoverNextStaleProcessingJob(LocalDateTime.now().minusMinutes(5));

    assertEquals(true, recovered);
    assertEquals(FeedbackCallbackJobStatus.RETRY_WAITING, job.getStatus());
    assertEquals(1, job.getRetryCount());
    assertNotNull(job.getNextRetryAt());
  }

  @Test
  @DisplayName("FCP-005 - 일반 RuntimeException은 재시도하지 않고 FAILED 처리해야 한다")
  void shouldFailPermanentlyWhenUnexpectedRuntimeExceptionOccurs() throws Exception {
    FeedbackCallbackJob job = callbackJob(1L, 100L, "req-1", "SUCCEED");

    given(feedbackCallbackJobRepository.findNextProcessingCandidate(any(LocalDateTime.class)))
        .willReturn(Optional.of(job));
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(loadQuestionSetPort.findIntvIdById(100L)).willReturn(Optional.of(10L));
    given(saveFeedbackPort.existsByQuestionSetId(100L))
        .willThrow(new RuntimeException("unexpected bug"));

    boolean processed = service.processNextJob();

    assertEquals(true, processed);
    assertEquals(FeedbackCallbackJobStatus.FAILED, job.getStatus());
    assertEquals(1, job.getRetryCount());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("FCP-006 - 락 획득 실패는 RETRY_WAITING으로 재시도 예약해야 한다")
  void shouldScheduleRetryWhenLockFailureOccurs() throws Exception {
    FeedbackCallbackJob job = callbackJob(1L, 100L, "req-1", "SUCCEED");

    given(feedbackCallbackJobRepository.findNextProcessingCandidate(any(LocalDateTime.class)))
        .willReturn(Optional.of(job));
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(loadQuestionSetPort.findIntvIdById(100L)).willReturn(Optional.of(10L));
    given(saveFeedbackPort.existsByQuestionSetId(100L))
        .willThrow(new CannotAcquireLockException("temporary lock failure"));

    boolean processed = service.processNextJob();

    assertEquals(true, processed);
    assertEquals(FeedbackCallbackJobStatus.RETRY_WAITING, job.getStatus());
    assertEquals(1, job.getRetryCount());
    assertNotNull(job.getNextRetryAt());
    verify(eventPublisher, never()).publishEvent(any());
  }

  private FeedbackCallbackJob callbackJob(
      Long jobId, Long questionSetId, String requestId, String status)
      throws JsonProcessingException {
    FeedbackCallbackJob job =
        FeedbackCallbackJob.create(
            questionSetId,
            requestId,
            objectMapper.writeValueAsString(request(requestId, questionSetId, status)),
            5);
    ReflectionTestUtils.setField(job, "id", jobId);
    return job;
  }

  private FeedbackWebhookCommand request(String requestId, Long questionSetId, String status) {
    return new FeedbackWebhookCommand(
        requestId,
        questionSetId,
        status,
        90,
        80,
        70,
        "calm",
        "summary",
        "strength",
        "improvement",
        List.of("clear"),
        85,
        75,
        1234,
        42);
  }
}
