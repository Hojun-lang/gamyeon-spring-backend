package com.gamyeon.answer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.answer.application.port.in.HandleAnswerSttCallbackCommand;
import com.gamyeon.answer.application.port.in.RequestAnswerAnalysisCommand;
import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerAnalysisJob;
import com.gamyeon.answer.domain.AnswerAnalysisJobRepository;
import com.gamyeon.answer.domain.AnswerAnalysisJobStatus;
import com.gamyeon.answer.domain.AnswerErrorCode;
import com.gamyeon.answer.domain.AnswerException;
import com.gamyeon.answer.domain.AnswerRepository;
import com.gamyeon.answer.domain.AnswerStatus;
import com.gamyeon.common.storage.application.StorageFileKeyGenerator;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("답변 서비스 - STT 요청 및 callback 처리")
@ExtendWith(MockitoExtension.class)
class AnswerApplicationServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private AnswerRepository answerRepository;
  @Mock private AnswerAnalysisJobRepository answerAnalysisJobRepository;
  @Mock private StorageFileKeyGenerator storageFileKeyGenerator;
  @Mock private StoragePresignedUrlPort storagePresignedUrlPort;
  @Mock private LoadQuestionSetPort loadQuestionSetPort;

  private AnswerApplicationService answerApplicationService;

  @BeforeEach
  void setUp() {
    AnswerAnalysisProperties answerAnalysisProperties = new AnswerAnalysisProperties();
    answerAnalysisProperties.setMaxRetryCount(5);

    answerApplicationService =
        new AnswerApplicationService(
            answerRepository,
            answerAnalysisJobRepository,
            storageFileKeyGenerator,
            storagePresignedUrlPort,
            answerAnalysisProperties,
            loadQuestionSetPort);
  }

  @Test
  @DisplayName("AAS-001 - 분석 요청 시 answer를 pending으로 바꾸고 analysis job을 생성해야 한다")
  void shouldCreateAnalysisJobAndMarkAnswerPendingWhenAnalysisRequested() {
    Answer answer = answer(1L, 10L, 100L);
    given(answerRepository.findById(1L)).willReturn(Optional.of(answer));
    given(answerAnalysisJobRepository.existsActiveJobByAnswerId(1L)).willReturn(false);
    given(loadQuestionSetPort.getQuestionContent(100L)).willReturn("질문");
    given(answerRepository.save(any(Answer.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(answerAnalysisJobRepository.save(any(AnswerAnalysisJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    answerApplicationService.requestAnalysis(new RequestAnswerAnalysisCommand(99L, 1L));

    assertEquals(AnswerStatus.STT_PENDING, answer.getStatus());

    ArgumentCaptor<AnswerAnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnswerAnalysisJob.class);
    verify(answerAnalysisJobRepository).save(jobCaptor.capture());

    AnswerAnalysisJob savedJob = jobCaptor.getValue();
    assertEquals(1L, savedJob.getAnswerId());
    assertEquals(AnswerAnalysisJobStatus.QUEUED, savedJob.getStatus());
    assertEquals(5, savedJob.getMaxRetryCount());
    assertNotNull(savedJob.getRequestId());
  }

  @Test
  @DisplayName("AAS-002 - 활성 analysis job이 있으면 중복 분석 요청을 거절해야 한다")
  void shouldRejectDuplicateAnalysisRequestWhenActiveJobExists() {
    Answer answer = answer(1L, 10L, 100L);
    given(answerRepository.findById(1L)).willReturn(Optional.of(answer));
    given(answerAnalysisJobRepository.existsActiveJobByAnswerId(1L)).willReturn(true);

    AnswerException exception =
        assertThrows(
            AnswerException.class,
            () ->
                answerApplicationService.requestAnalysis(
                    new RequestAnswerAnalysisCommand(99L, 1L)));

    assertEquals(AnswerErrorCode.ANALYSIS_ALREADY_IN_PROGRESS, exception.getErrorCode());
    verify(answerAnalysisJobRepository, never()).save(any(AnswerAnalysisJob.class));
  }

  @Test
  @DisplayName("AAS-003 - requestId로 callback을 찾으면 answer와 job을 완료 처리해야 한다")
  void shouldCompleteAnswerAndJobWhenCallbackMatchesRequestId() {
    Answer answer = answer(1L, 10L, 100L);
    answer.markSttProcessing();
    AnswerAnalysisJob job = answerAnalysisJob(11L, 1L, "req-1");
    job.markSent(java.time.LocalDateTime.now());
    JsonNode payload = objectMapper.createObjectNode().put("result", "done");

    given(answerAnalysisJobRepository.findByRequestId("req-1")).willReturn(Optional.of(job));
    given(answerRepository.findById(1L)).willReturn(Optional.of(answer));
    given(answerRepository.save(any(Answer.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(answerAnalysisJobRepository.save(any(AnswerAnalysisJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    answerApplicationService.handle(
        new HandleAnswerSttCallbackCommand("req-1", 10L, 100L, payload, null));

    assertEquals(AnswerStatus.STT_COMPLETED, answer.getStatus());
    assertEquals(payload, answer.getContent());
    assertEquals(AnswerAnalysisJobStatus.COMPLETED, job.getStatus());
  }

  @Test
  @DisplayName("AAS-004 - 이미 종료된 job에 대한 중복 callback은 무시해야 한다")
  void shouldIgnoreDuplicateCallbackWhenJobIsAlreadyTerminal() {
    AnswerAnalysisJob job = answerAnalysisJob(11L, 1L, "req-1");
    job.complete();

    given(answerAnalysisJobRepository.findByRequestId("req-1")).willReturn(Optional.of(job));

    answerApplicationService.handle(
        new HandleAnswerSttCallbackCommand(
            "req-1", 10L, 100L, objectMapper.createObjectNode(), null));

    verify(answerRepository, never()).save(any(Answer.class));
    verify(answerAnalysisJobRepository, never()).save(any(AnswerAnalysisJob.class));
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
}
