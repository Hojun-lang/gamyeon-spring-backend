package com.gamyeon.intv.presentation.dto.response;

import com.gamyeon.report.application.port.in.ReportSummaryResult;
import java.util.List;

public record IntvReportSummaryResponse(
    Long reportId,
    String reportStatus,
    Integer totalScore,
    Integer answeredCount,
    List<String> strengths,
    List<String> weaknesses) {

  public static IntvReportSummaryResponse from(ReportSummaryResult info) {
    if (info == null) {
      return null;
    }
    return new IntvReportSummaryResponse(
        info.reportId(),
        info.reportStatus(),
        info.totalScore(),
        info.answeredCount(),
        info.strengths(),
        info.weaknesses());
  }
}
