package com.gamyeon.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.report.application.exception.ReportGenerationFailedException;
import com.gamyeon.report.application.port.out.LoadFeedbackPort;
import com.gamyeon.report.application.port.out.LoadQuestionSetPort;
import com.gamyeon.report.application.port.out.SaveReportPort;
import com.gamyeon.report.domain.Report;
import com.gamyeon.report.domain.ReportStatus;
import com.gamyeon.report.infrastructure.external.AiReportClient;
import com.gamyeon.report.infrastructure.external.dto.AiReportFeedbackItem;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiReportRequestorTest {

  // ── @CircuitBreaker는 AOP 기반이므로 순수 Mockito 환경에서는 동작하지 않음
  // handleAiFailure()를 직접 호출하여 Fallback 분기 로직만 독립 검증
  @InjectMocks private AiReportRequestor aiReportRequestor;

  @Mock private LoadFeedbackPort loadFeedbackPort;
  @Mock private LoadQuestionSetPort loadQuestionSetPort;
  @Mock private SaveReportPort saveReportPort;
  @Mock private AiReportClient aiReportClient;

  private Report report;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(aiReportRequestor, "callbackBaseUrl", "http://test-server.com");
    report = Report.createInProgress(1L, 7L);
  }

  // ── requestToAi() 정상 흐름 ─────────────────────────────────────────────

  @Nested
  @DisplayName("requestToAi() — 정상 흐름")
  class RequestToAiSuccess {

    @Test
    @DisplayName("질문 ID 오름차순으로 정렬되어 올바른 index와 questionContent가 AI 요청에 포함된다")
    void shouldAssignCorrectIndicesAndContentBasedOnQuestionIdOrder() {
      // given
      Long intvId = 1L;

      QuestionSet q1 = QuestionSet.create(intvId, "두 번째 질문입니다.");
      QuestionSet q2 = QuestionSet.create(intvId, "첫 번째 질문입니다.");
      QuestionSet q3 = QuestionSet.create(intvId, "세 번째 질문입니다.");
      ReflectionTestUtils.setField(q1, "id", 102L);
      ReflectionTestUtils.setField(q2, "id", 101L);
      ReflectionTestUtils.setField(q3, "id", 103L);

      given(loadQuestionSetPort.findAllByIntvId(intvId)).willReturn(List.of(q1, q2, q3));
      given(loadFeedbackPort.findSucceedFeedbacksByIntvId(intvId))
          .willReturn(
              List.of(
                  AiReportFeedbackItem.builder().questionSetId(101L).status("SUCCEED").build(),
                  AiReportFeedbackItem.builder().questionSetId(102L).status("SUCCEED").build(),
                  AiReportFeedbackItem.builder().questionSetId(103L).status("SUCCEED").build()));

      // when
      aiReportRequestor.requestToAi(report);

      // then
      var captor =
          ArgumentCaptor.forClass(
              com.gamyeon.report.infrastructure.external.dto.AiReportRequest.class);
      verify(aiReportClient).requestGenerate(captor.capture());

      var sentFeedbacks = captor.getValue().getFeedbacks();
      assertThat(captor.getValue().getCallback()).contains("http://test-server.com");

      var first = findById(sentFeedbacks, 101L);
      assertThat(first.getIndex()).isEqualTo(1);
      assertThat(first.getQuestionContent()).isEqualTo("첫 번째 질문입니다.");

      var second = findById(sentFeedbacks, 102L);
      assertThat(second.getIndex()).isEqualTo(2);
      assertThat(second.getQuestionContent()).isEqualTo("두 번째 질문입니다.");

      var third = findById(sentFeedbacks, 103L);
      assertThat(third.getIndex()).isEqualTo(3);
      assertThat(third.getQuestionContent()).isEqualTo("세 번째 질문입니다.");
    }

    @Test
    @DisplayName("AI 요청 성공 시 report.fail()과 saveReportPort.save()는 호출되지 않는다")
    void shouldNotFailReportOnSuccess() {
      // given
      given(loadQuestionSetPort.findAllByIntvId(any())).willReturn(List.of());
      given(loadFeedbackPort.findSucceedFeedbacksByIntvId(any())).willReturn(List.of());

      // when
      aiReportRequestor.requestToAi(report);

      // then
      assertThat(report.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
      verify(saveReportPort, never()).save(any());
    }

    private AiReportFeedbackItem findById(List<AiReportFeedbackItem> list, Long id) {
      return list.stream()
          .filter(f -> f.getQuestionSetId().equals(id))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Feedback not found for ID: " + id));
    }
  }

  // ── handleAiFailure() Fallback 분기 검증 ───────────────────────────────

  @Nested
  @DisplayName("handleAiFailure() — Fallback 분기")
  class FallbackBranch {

    @Test
    @DisplayName("Case 1: FeignException 발생 시 report.fail()이 저장되고 예외가 재throw된다")
    void case1_feignExceptionShouldFailReportAndRethrow() {
      // given
      FeignException feignException = mock(FeignException.class);

      // when & then
      assertThatThrownBy(() -> aiReportRequestor.handleAiFailure(report, feignException))
          .isInstanceOf(ReportGenerationFailedException.class);

      // saveFailIndependently()가 호출되었는지 간접 검증
      // (REQUIRES_NEW 트랜잭션은 통합테스트에서 검증)
      verify(saveReportPort).save(report);
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
    }

    @Test
    @DisplayName("Case 2: CallNotPermittedException 발생 시 IN_PROGRESS 유지, save() 미호출")
    void case2_circuitOpenShouldKeepInProgressAndNotSave() {
      // given — 실제 CircuitBreaker 인스턴스에서 CallNotPermittedException 생성
      CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("test");
      CallNotPermittedException exception =
          CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

      // when
      aiReportRequestor.handleAiFailure(report, exception);

      // then
      assertThat(report.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
      verify(saveReportPort, never()).save(any());
    }

    @Test
    @DisplayName("Case 3: SocketTimeoutException(cause) 발생 시 IN_PROGRESS 유지, save() 미호출")
    void case3_socketTimeoutShouldKeepInProgressAndNotSave() {
      // given — Feign이 감싸는 방식 재현: cause가 SocketTimeoutException
      RuntimeException timeoutWrapper =
          new RuntimeException("feign timeout", new SocketTimeoutException("Read timed out"));

      // when
      aiReportRequestor.handleAiFailure(report, timeoutWrapper);

      // then
      assertThat(report.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
      verify(saveReportPort, never()).save(any());
    }

    @Test
    @DisplayName("Case 3: java.util.concurrent.TimeoutException 발생 시 IN_PROGRESS 유지")
    void case3_concurrentTimeoutShouldKeepInProgress() {
      // given
      java.util.concurrent.TimeoutException timeoutException =
          new java.util.concurrent.TimeoutException("LLM slow call");

      // when
      aiReportRequestor.handleAiFailure(report, timeoutException);

      // then
      assertThat(report.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
      verify(saveReportPort, never()).save(any());
    }
  }
}
