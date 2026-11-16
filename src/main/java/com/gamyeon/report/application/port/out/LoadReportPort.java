package com.gamyeon.report.application.port.out;

import com.gamyeon.report.domain.Report;
import com.gamyeon.report.domain.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoadReportPort {

  Optional<Report> findByIntvId(Long intvId);

  // tryTriggerReportGeneration 전용 — Pessimistic Write Lock
  Optional<Report> findByIntvIdWithLock(Long intvId);

  // 스케줄러 전용 — IN_PROGRESS & 생성 시각 기준 경과 건
  List<Report> findAllTimedOut(ReportStatus status, LocalDateTime threshold);

  List<Report> findAllByIntvIds(List<Long> intvIds);
}
