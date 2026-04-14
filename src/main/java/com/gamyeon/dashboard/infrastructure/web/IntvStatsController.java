package com.gamyeon.dashboard.infrastructure.web;

import com.gamyeon.common.response.SuccessResponse;
import com.gamyeon.dashboard.application.port.inbound.DailyStat;
import com.gamyeon.dashboard.application.port.inbound.IntvStatsUseCase;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class IntvStatsController {

  private final IntvStatsUseCase intvStatsUseCase;

  public IntvStatsController(IntvStatsUseCase intvStatsUseCase) {
    this.intvStatsUseCase = intvStatsUseCase;
  }

  @GetMapping("/api/v1/intvs/statsmockup")
  public ResponseEntity<SuccessResponse<List<DailyStat>>> getIntvStats(
      @AuthenticationPrincipal Long userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    log.info(
        "Received intv stats request. userId={}, startDate={}, endDate={}",
        userId,
        startDate,
        endDate);

    List<DailyStat> stats = intvStatsUseCase.getIntvStats(userId, startDate, endDate);
    return ResponseEntity.ok(SuccessResponse.of(stats));
  }
}
