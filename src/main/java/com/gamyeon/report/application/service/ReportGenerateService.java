package com.gamyeon.report.application.service;

import com.gamyeon.report.application.port.in.GenerateReportUseCase;
import com.gamyeon.report.application.port.in.TriggerType;
import com.gamyeon.report.application.port.out.LoadFeedbackPort;
import com.gamyeon.report.application.port.out.LoadReportPort;
import com.gamyeon.report.application.port.out.SaveReportPort;
import com.gamyeon.report.domain.Report;
import com.gamyeon.report.domain.ReportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerateService implements GenerateReportUseCase {

  @Value("${report.total-question-count:7}")
  private int totalQuestionCount;

  private static final int MIN_FEEDBACK_COUNT = 3;

  private final LoadReportPort loadReportPort;
  private final SaveReportPort saveReportPort;
  private final LoadFeedbackPort loadFeedbackPort;
  private final AiReportRequestor aiReportRequestor;

  // ── 단일 진입점 ────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public void tryTriggerReportGeneration(Long intvId, TriggerType triggerType) {

    // Pessimistic Write Lock 획득
    Report report =
        loadReportPort
            .findByIntvIdWithLock(intvId)
            .orElseGet(
                () -> {
                  log.warn("[Report] REPORTS 레코드 없음 - intvId={}", intvId);
                  return null;
                });

    if (report == null) return;

    // IN_PROGRESS가 아니면 이미 처리된 건 — 즉시 리턴
    if (report.getStatus() != ReportStatus.IN_PROGRESS) {
      log.info("[Report] 이미 처리된 리포트 - intvId={}, status={}", intvId, report.getStatus());
      return;
    }

    int succeedCount = loadFeedbackPort.countSucceedByIntvId(intvId);

    switch (triggerType) {
      case INTERVIEW_FINISHED, FEEDBACK_SAVED -> handleEventTrigger(report, succeedCount);
      case SCHEDULER -> handleSchedulerTrigger(report, succeedCount);
    }
    // @Transactional 커밋 → Lock 해제
    // scheduleAiRequest()에 등록된 afterCommit()은 커밋 완료 후 실행됨
  }

  // ── 이벤트 트리거 분기 ─────────────────────────────────────────────────────

  private void handleEventTrigger(Report report, int succeedCount) {
    if (succeedCount >= totalQuestionCount) {
      log.info("[Report] 피드백 {}개 완료 — AI 요청 진행 - intvId={}", succeedCount, report.getIntvId());
      scheduleAiRequest(report);
    } else {
      log.info("[Report] 피드백 {}개 수집 중 — 대기 - intvId={}", succeedCount, report.getIntvId());
    }
  }

  // ── 스케줄러 트리거 분기 ───────────────────────────────────────────────────

  private void handleSchedulerTrigger(Report report, int succeedCount) {
    if (succeedCount >= MIN_FEEDBACK_COUNT) {
      log.info("[Report] 스케줄러 강제 실행 - intvId={}, 피드백 수={}", report.getIntvId(), succeedCount);
      scheduleAiRequest(report);
    } else {
      log.warn("[Report] 피드백 부족 FAILED 처리 - intvId={}, 피드백 수={}", report.getIntvId(), succeedCount);
      report.fail();
      saveReportPort.save(report);
    }
  }

  // ── 트랜잭션 커밋 이후 AI 요청 보장 ──────────────────────────────────────────

  private void scheduleAiRequest(Report report) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            // Lock 커밋 완료 후 — 트랜잭션 외부에서 AI 요청
            aiReportRequestor.requestToAi(report);
          }
        });
  }
}
