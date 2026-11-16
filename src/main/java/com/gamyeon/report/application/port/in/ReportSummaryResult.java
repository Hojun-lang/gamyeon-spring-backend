package com.gamyeon.report.application.port.in;

import com.gamyeon.report.domain.Report;
import java.util.List;

public record ReportSummaryResult(
    Long intvId,
    Long reportId,
    String reportStatus,
    Integer totalScore,
    Integer answeredCount,
    List<String> strengths,
    List<String> weaknesses) {

  public static ReportSummaryResult from(Report report) {
    return new ReportSummaryResult(
        report.getIntvId(),
        report.getId(),
        report.getStatus().name(),
        report.getTotalScore(),
        report.getAnsweredCount(),
        report.getStrengths(),
        report.getWeaknesses());
  }
}
