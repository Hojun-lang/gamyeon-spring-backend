package com.gamyeon.report.application.service;

import com.gamyeon.report.application.port.in.ReportSummaryQueryUseCase;
import com.gamyeon.report.application.port.in.ReportSummaryResult;
import com.gamyeon.report.application.port.out.LoadReportPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportSummaryQueryService implements ReportSummaryQueryUseCase {

  private final LoadReportPort loadReportPort;

  @Override
  @Transactional(readOnly = true)
  public List<ReportSummaryResult> findAllByIntvIds(List<Long> intvIds) {
    if (intvIds.isEmpty()) {
      return List.of();
    }
    return loadReportPort.findAllByIntvIds(intvIds).stream()
        .map(ReportSummaryResult::from)
        .toList();
  }
}
