package com.gamyeon.report.infrastructure.scheduler;

import com.gamyeon.report.application.port.in.GenerateReportUseCase;
import com.gamyeon.report.application.port.in.TriggerType;
import com.gamyeon.report.application.port.out.LoadReportPort;
import com.gamyeon.report.domain.Report;
import com.gamyeon.report.domain.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

  // application.yml 외부화, 기본값 5분
  @Value("${report.scheduler.timeout-minutes:5}")
  private int timeoutMinutes;

  private final LoadReportPort loadReportPort;
  private final GenerateReportUseCase generateReportUseCase;

  @Scheduled(fixedDelay = 60_000) // 1분 주기
  public void triggerTimedOutReports() {
    // updatedAt 기준 timeoutMinutes 초과 레코드만 처리
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

    List<Report> timedOutReports =
        loadReportPort.findAllTimedOut(ReportStatus.IN_PROGRESS, threshold);

    if (timedOutReports.isEmpty()) {
      return;
    }

    log.info("[Report] 스케줄러 실행 - 타임아웃 대상 {}건 (기준: {}분 초과)", timedOutReports.size(), timeoutMinutes);

    for (Report report : timedOutReports) {
      try {
        generateReportUseCase.tryTriggerReportGeneration(report.getIntvId(), TriggerType.SCHEDULER);
      } catch (Exception e) {
        // 1건 실패가 나머지 건에 영향을 주지 않도록 개별 catch
        log.error("[Report] 스케줄러 처리 실패 - intvId={}", report.getIntvId(), e);
      }
    }
  }
}
