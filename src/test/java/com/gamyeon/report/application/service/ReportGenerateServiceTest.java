package com.gamyeon.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.gamyeon.report.application.port.in.TriggerType;
import com.gamyeon.report.application.port.out.LoadFeedbackPort;
import com.gamyeon.report.application.port.out.LoadReportPort;
import com.gamyeon.report.application.port.out.SaveReportPort;
import com.gamyeon.report.domain.Report;
import com.gamyeon.report.domain.ReportStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ReportGenerateServiceTest {

  @InjectMocks private ReportGenerateService reportGenerateService;

  @Mock private LoadReportPort loadReportPort;
  @Mock private SaveReportPort saveReportPort;
  @Mock private LoadFeedbackPort loadFeedbackPort;
  @Mock private AiReportRequestor aiReportRequestor;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reportGenerateService, "totalQuestionCount", 7);
    // TransactionSynchronizationManager 활성화 (트랜잭션 컨텍스트 시뮬레이션)
    TransactionSynchronizationManager.initSynchronization();
  }

  // ✅ 각 테스트 후 TransactionSynchronizationManager 정리
  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  // ── 조기 리턴 케이스 ──────────────────────────────────────────────────────

  @Nested
  @DisplayName("조기 리턴 케이스")
  class EarlyReturn {

    @Test
    @DisplayName("Report 레코드가 없으면 아무것도 호출하지 않고 리턴한다")
    void shouldReturnEarlyWhenReportNotFound() {
      // given
      given(loadReportPort.findByIntvIdWithLock(1L)).willReturn(Optional.empty());

      // when
      reportGenerateService.tryTriggerReportGeneration(1L, TriggerType.FEEDBACK_SAVED);

      // then
      verify(loadFeedbackPort, never()).countSucceedByIntvId(any());
      verify(aiReportRequestor, never()).requestToAi(any());
    }

    @Test
    @DisplayName("status가 IN_PROGRESS가 아니면 조기 리턴한다")
    void shouldReturnEarlyWhenNotInProgress() {
      // given
      Report report = Report.createInProgress(1L, 7L);
      report.fail(); // FAILED 상태로 전환
      given(loadReportPort.findByIntvIdWithLock(1L)).willReturn(Optional.of(report));

      // when
      reportGenerateService.tryTriggerReportGeneration(1L, TriggerType.FEEDBACK_SAVED);

      // then
      verify(loadFeedbackPort, never()).countSucceedByIntvId(any());
      verify(aiReportRequestor, never()).requestToAi(any());
    }
  }

  // ── 이벤트 트리거 ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("이벤트 트리거 (FEEDBACK_SAVED / INTERVIEW_FINISHED)")
  class EventTrigger {

    @Test
    @DisplayName("피드백 7개 완료 시 TransactionSynchronization이 등록된다")
    void shouldRegisterSynchronizationWhenFeedbackComplete() {
      // given
      Report report = Report.createInProgress(1L, 7L);
      given(loadReportPort.findByIntvIdWithLock(1L)).willReturn(Optional.of(report));
      given(loadFeedbackPort.countSucceedByIntvId(1L)).willReturn(7);

      // when
      reportGenerateService.tryTriggerReportGeneration(1L, TriggerType.FEEDBACK_SAVED);

      // then — afterCommit 동기화가 등록되었는지 확인
      assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    }

    @Test
    @DisplayName("afterCommit() 실행 시 aiReportRequestor.requestToAi()가 호출된다")
    void shouldCallRequestToAiOnAfterCommit() {
      // given
      Report report = Report.createInProgress(1L, 7L);
      given(loadReportPort.findByIntvIdWithLock(1L)).willReturn(Optional.of(report));
      given(loadFeedbackPort.countSucceedByIntvId(1L)).willReturn(7);

      reportGenerateService.tryTriggerReportGeneration(1L, TriggerType.FEEDBACK_SAVED);

      // when — afterCommit 수동 실행 (트랜잭션 커밋 시뮬레이션)
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);

      // then
      verify(aiReportRequestor).requestToAi(report);
    }

    @Test
    @DisplayName("피드백 7개 미만이면 TransactionSynchronization이 등록되지 않는다")
    void shouldNotRegisterSynchronizationWhenFeedbackInsufficient() {
      // given
      Report report = Report.createInProgress(1L, 7L);
      given(loadReportPort.findByIntvIdWithLock(1L)).willReturn(Optional.of(report));
      given(loadFeedbackPort.countSucceedByIntvId(1L)).willReturn(5);

      // when
      reportGenerateService.tryTriggerReportGeneration(1L, TriggerType.FEEDBACK_SAVED);

      // then
      assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
      verify(aiReportRequestor, never()).requestToAi(any());
    }
  }

  // ── 스케줄러 트리거 ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("스케줄러 트리거 (SCHEDULER)")
  class SchedulerTrigger {

    @Test
    @DisplayName("피드백 3개 이상이면 TransactionSynchronization이 등록된다")
    void shouldRegisterSynchronizationWhenMinFeedbackMet() {
      // given
      Report report = Report.createInProgress(1L, 7L);
      given(loadReportPort.findByIntvIdWithLock(1L)).willReturn(Optional.of(report));
      given(loadFeedbackPort.countSucceedByIntvId(1L)).willReturn(3);

      // when
      reportGenerateService.tryTriggerReportGeneration(1L, TriggerType.SCHEDULER);

      // then
      assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    }

    @Test
    @DisplayName("피드백 3개 미만이면 report.fail()과 save()가 호출되고 AI 요청은 등록되지 않는다")
    void shouldFailReportWhenFeedbackBelowMinimum() {
      // given
      Report report = Report.createInProgress(1L, 7L);
      given(loadReportPort.findByIntvIdWithLock(1L)).willReturn(Optional.of(report));
      given(loadFeedbackPort.countSucceedByIntvId(1L)).willReturn(2);

      // when
      reportGenerateService.tryTriggerReportGeneration(1L, TriggerType.SCHEDULER);

      // then
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      verify(saveReportPort).save(report);
      assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
      verify(aiReportRequestor, never()).requestToAi(any());
    }
  }
}
