package com.gamyeon.feedback.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.feedback.application.FeedbackCallbackProperties;
import com.gamyeon.feedback.application.port.in.FeedbackWebhookCommand;
import com.gamyeon.feedback.domain.FeedbackCallbackJob;
import com.gamyeon.feedback.domain.FeedbackCallbackJobRepository;
import com.gamyeon.feedback.domain.FeedbackCallbackJobStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("피드백 callback 접수 서비스")
@ExtendWith(MockitoExtension.class)
class FeedbackWebhookServiceTest {

  @Mock private FeedbackCallbackJobRepository feedbackCallbackJobRepository;

  private FeedbackWebhookService feedbackWebhookService;

  @BeforeEach
  void setUp() {
    FeedbackCallbackProperties properties = new FeedbackCallbackProperties();
    properties.setMaxRetryCount(5);
    feedbackWebhookService =
        new FeedbackWebhookService(feedbackCallbackJobRepository, properties, new ObjectMapper());
  }

  @Test
  @DisplayName("FWH-001 - 신규 callback은 RECEIVED job으로 접수해야 한다")
  void shouldCreateReceivedJobWhenNewCallbackArrives() {
    FeedbackWebhookCommand request = request("req-1", 100L, "SUCCEED");

    given(feedbackCallbackJobRepository.findByRequestId("req-1")).willReturn(Optional.empty());
    given(feedbackCallbackJobRepository.findByQuestionSetId(100L)).willReturn(Optional.empty());
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    feedbackWebhookService.handleWebhook(request);

    ArgumentCaptor<FeedbackCallbackJob> jobCaptor =
        ArgumentCaptor.forClass(FeedbackCallbackJob.class);
    verify(feedbackCallbackJobRepository).save(jobCaptor.capture());

    FeedbackCallbackJob job = jobCaptor.getValue();
    assertEquals(100L, job.getQuestionSetId());
    assertEquals("req-1", job.getRequestId());
    assertEquals(FeedbackCallbackJobStatus.RECEIVED, job.getStatus());
    assertEquals(5, job.getMaxRetryCount());
  }

  @Test
  @DisplayName("FWH-002 - FAILED job에 새 callback이 오면 RECEIVED로 재접수해야 한다")
  void shouldReopenFailedJobWhenCallbackArrivesAgain() {
    FeedbackCallbackJob job = FeedbackCallbackJob.create(100L, "req-old", "{}", 5);
    ReflectionTestUtils.setField(job, "id", 1L);
    job.markFailed("bad payload");

    given(feedbackCallbackJobRepository.findByRequestId("req-new")).willReturn(Optional.empty());
    given(feedbackCallbackJobRepository.findByQuestionSetId(100L)).willReturn(Optional.of(job));
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    feedbackWebhookService.handleWebhook(request("req-new", 100L, "SUCCEED"));

    assertEquals(FeedbackCallbackJobStatus.RECEIVED, job.getStatus());
    assertEquals("req-new", job.getRequestId());
    assertEquals(0, job.getRetryCount());
    verify(feedbackCallbackJobRepository).save(job);
  }

  @Test
  @DisplayName("FWH-003 - 동시 중복 insert 충돌은 중복 callback으로 흡수해야 한다")
  void shouldIgnoreConcurrentDuplicateInsertConflict() {
    FeedbackWebhookCommand request = request("req-1", 100L, "SUCCEED");

    given(feedbackCallbackJobRepository.findByRequestId("req-1")).willReturn(Optional.empty());
    given(feedbackCallbackJobRepository.findByQuestionSetId(100L)).willReturn(Optional.empty());
    given(feedbackCallbackJobRepository.save(any(FeedbackCallbackJob.class)))
        .willThrow(new DataIntegrityViolationException("duplicate"));

    feedbackWebhookService.handleWebhook(request);

    verify(feedbackCallbackJobRepository).save(any(FeedbackCallbackJob.class));
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
