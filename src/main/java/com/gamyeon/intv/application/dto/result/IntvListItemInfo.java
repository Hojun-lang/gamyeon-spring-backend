package com.gamyeon.intv.application.dto.result;

import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvStatus;
import com.gamyeon.report.application.port.in.ReportSummaryResult;
import java.time.LocalDateTime;

public record IntvListItemInfo(
    Long intvId,
    String title,
    IntvStatus intvStatus,
    Long durationSeconds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    ReportSummaryResult report) {

  public static IntvListItemInfo from(Intv intv, ReportSummaryResult report) {
    return new IntvListItemInfo(
        intv.getId(),
        intv.getTitle(),
        intv.getStatus(),
        intv.getDurationSeconds(),
        intv.getCreatedAt(),
        intv.getUpdatedAt(),
        report);
  }
}
