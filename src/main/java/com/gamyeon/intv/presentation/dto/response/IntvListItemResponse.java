package com.gamyeon.intv.presentation.dto.response;

import com.gamyeon.intv.application.dto.result.IntvListItemInfo;
import java.time.LocalDateTime;

public record IntvListItemResponse(
    Long intvId,
    String title,
    String intvStatus,
    Long durationSeconds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    IntvReportSummaryResponse report) {

  public static IntvListItemResponse from(IntvListItemInfo info) {
    return new IntvListItemResponse(
        info.intvId(),
        info.title(),
        info.intvStatus().name(),
        info.durationSeconds(),
        info.createdAt(),
        info.updatedAt(),
        IntvReportSummaryResponse.from(info.report()));
  }
}
