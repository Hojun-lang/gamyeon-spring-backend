package com.gamyeon.answer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.gamyeon.answer.application.port.out.AnswerAnalysisTarget;
import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.application.port.out.RequestAnswerSttAnalysisPort;
import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerAnalysisJob;
import com.gamyeon.answer.domain.AnswerAnalysisJobRepository;
import com.gamyeon.answer.domain.AnswerAnalysisJobStatus;
import com.gamyeon.answer.domain.AnswerRepository;
import com.gamyeon.answer.domain.AnswerStatus;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("답변 분석 전송 서비스 - dispatch 및 재시도")
@ExtendWith(MockitoExtension.class)
class AnswerAnalysisDispatchServiceTest {

  @Mock private AnswerRepository answerRepository;
  @Mock private AnswerAnalysisJobRepository answerAnalysisJobRepository;
  @Mock private LoadQuestionSetPort loadQuestionSetPort;
  @Mock private RequestAnswerSttAnalysisPort requestAnswerSttAnalysisPort;

  private AnswerAnalysisDispatchService answerAnalysisDispatchService;

  @BeforeEach
  void setUp() {
    AnswerAnalysisDispatchTransactionService transactionService =
        new AnswerAnalysisDispatchTransactionService(
            answerRepository, answerAnalysisJobRepository, loadQuestionSetPort);
    answerAnalysisDispatchService =
        new AnswerAnalysisDispatchService(transactionService, requestAnswerSttAnalysisPort);
  }

  @Test
  @DisplayName("ADS-001 - 대기 중 job 전송이 성공하면 sent와 processing 상태로 바꿔야 한다")
  void shouldMarkJobSentAndAnswerProcessingWhenDispatchSucceeds() {
    Answer answer = answer(1L, 10L, 100L);
    AnswerAnalysisJob job = answerAnalysisJob(11L, 1L, "req-1");

    given(answerAnalysisJobRepository.findNextDispatchCandidate(any(LocalDateTime.class)))
        .willReturn(Optional.of(job))
        .willReturn(Optional.empty());
    given(answerAnalysisJobRepository.findById(11L)).willReturn(Optional.of(job));
    given(answerRepository.findById(1L)).willReturn(Optional.of(answer));
    given(loadQuestionSetPort.getQuestionContent(100L)).willReturn("질문");
    given(answerAnalysisJobRepository.save(any(AnswerAnalysisJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(answerRepository.save(any(Answer.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    int dispatchedCount = answerAnalysisDispatchService.dispatchPendingJobs(1);

    assertEquals(1, dispatchedCount);
    assertEquals(AnswerAnalysisJobStatus.SENT, job.getStatus());
    assertEquals(AnswerStatus.STT_PROCESSING, answer.getStatus());

    ArgumentCaptor<AnswerAnalysisTarget> targetCaptor =
        ArgumentCaptor.forClass(AnswerAnalysisTarget.class);
    verify(requestAnswerSttAnalysisPort).request(targetCaptor.capture());

    AnswerAnalysisTarget target = targetCaptor.getValue();
    assertEquals("req-1", target.requestId());
    assertEquals(1L, target.answerId());
    assertEquals(100L, target.questionSetId());
  }

  @Test
  @DisplayName("ADS-002 - 재시도 가능한 예외가 발생하면 retry waiting으로 전환해야 한다")
  void shouldScheduleRetryWhenRetryableExceptionOccurs() {
    Answer answer = answer(1L, 10L, 100L);
    AnswerAnalysisJob job = answerAnalysisJob(11L, 1L, "req-1");

    given(answerAnalysisJobRepository.findNextDispatchCandidate(any(LocalDateTime.class)))
        .willReturn(Optional.of(job))
        .willReturn(Optional.empty());
    given(answerAnalysisJobRepository.findById(11L)).willReturn(Optional.of(job));
    given(answerRepository.findById(1L)).willReturn(Optional.of(answer));
    given(loadQuestionSetPort.getQuestionContent(100L)).willReturn("질문");
    given(answerAnalysisJobRepository.save(any(AnswerAnalysisJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(answerRepository.save(any(Answer.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    willThrow(retryableException())
        .given(requestAnswerSttAnalysisPort)
        .request(any(AnswerAnalysisTarget.class));

    int dispatchedCount = answerAnalysisDispatchService.dispatchPendingJobs(1);

    assertEquals(1, dispatchedCount);
    assertEquals(AnswerAnalysisJobStatus.RETRY_WAITING, job.getStatus());
    assertEquals(1, job.getRetryCount());
    assertNotNull(job.getNextRetryAt());
    assertEquals(AnswerStatus.STT_PENDING, answer.getStatus());
  }

  @Test
  @DisplayName("ADS-003 - 재시도 불가능한 예외가 발생하면 최종 실패 처리해야 한다")
  void shouldMarkJobFailedWhenNonRetryableExceptionOccurs() {
    Answer answer = answer(1L, 10L, 100L);
    AnswerAnalysisJob job = answerAnalysisJob(11L, 1L, "req-1");

    given(answerAnalysisJobRepository.findNextDispatchCandidate(any(LocalDateTime.class)))
        .willReturn(Optional.of(job))
        .willReturn(Optional.empty());
    given(answerAnalysisJobRepository.findById(11L)).willReturn(Optional.of(job));
    given(answerRepository.findById(1L)).willReturn(Optional.of(answer));
    given(loadQuestionSetPort.getQuestionContent(100L)).willReturn("질문");
    given(answerAnalysisJobRepository.save(any(AnswerAnalysisJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(answerRepository.save(any(Answer.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    willThrow(new IllegalStateException("bad request"))
        .given(requestAnswerSttAnalysisPort)
        .request(any(AnswerAnalysisTarget.class));

    int dispatchedCount = answerAnalysisDispatchService.dispatchPendingJobs(1);

    assertEquals(1, dispatchedCount);
    assertEquals(AnswerAnalysisJobStatus.FAILED, job.getStatus());
    assertEquals(AnswerStatus.STT_FAILED, answer.getStatus());
    assertNotNull(answer.getErrorMessage());
  }

  @Test
  @DisplayName("ADS-004 - 오래된 SENDING job은 retry waiting으로 복구해야 한다")
  void shouldRecoverStaleSendingJobAsRetryWaiting() {
    Answer answer = answer(1L, 10L, 100L);
    AnswerAnalysisJob job = answerAnalysisJob(11L, 1L, "req-1");
    job.markSending(LocalDateTime.now().minusMinutes(10));

    given(answerAnalysisJobRepository.findNextStaleSendingJob(any(LocalDateTime.class)))
        .willReturn(Optional.of(job))
        .willReturn(Optional.empty());
    given(answerRepository.findById(1L)).willReturn(Optional.of(answer));
    given(answerAnalysisJobRepository.save(any(AnswerAnalysisJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(answerRepository.save(any(Answer.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    int recoveredCount = answerAnalysisDispatchService.recoverStaleSendingJobs(1, 300_000L);

    assertEquals(1, recoveredCount);
    assertEquals(AnswerAnalysisJobStatus.RETRY_WAITING, job.getStatus());
    assertEquals(1, job.getRetryCount());
    assertNotNull(job.getNextRetryAt());
    assertEquals(AnswerStatus.STT_PENDING, answer.getStatus());
  }

  private Answer answer(Long answerId, Long intvId, Long questionSetId) {
    Answer answer =
        Answer.create(
            intvId,
            questionSetId,
            "answer.mp4",
            "answers/video.mp4",
            "https://cdn.example.com/answers/video.mp4",
            "video/mp4",
            1024L);
    ReflectionTestUtils.setField(answer, "id", answerId);
    return answer;
  }

  private AnswerAnalysisJob answerAnalysisJob(Long jobId, Long answerId, String requestId) {
    AnswerAnalysisJob job = AnswerAnalysisJob.create(answerId, requestId, 5);
    ReflectionTestUtils.setField(job, "id", jobId);
    return job;
  }

  private RetryableException retryableException() {
    Request request =
        Request.create(
            HttpMethod.POST,
            "http://localhost/internal/v1/answers/analyze",
            Map.of(),
            null,
            StandardCharsets.UTF_8);
    return new RetryableException(503, "temporary failure", HttpMethod.POST, 1000L, request);
  }
}
