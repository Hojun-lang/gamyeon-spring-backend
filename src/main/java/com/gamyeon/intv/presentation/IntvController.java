package com.gamyeon.intv.presentation;

import com.gamyeon.common.response.ApiResponse;
import com.gamyeon.intv.application.dto.command.ChangeStateIntvCommand;
import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import com.gamyeon.intv.application.dto.result.IntvInfo;
import com.gamyeon.intv.application.usecase.ChangeStateUseCase;
import com.gamyeon.intv.application.usecase.CreateUseCase;
import com.gamyeon.intv.application.usecase.GetFinishedIntvStatsUseCase;
import com.gamyeon.intv.application.usecase.UpdateTitleUseCase;
import com.gamyeon.intv.domain.IntvSuccessCode;
import com.gamyeon.intv.presentation.dto.request.IntvRequest;
import com.gamyeon.intv.presentation.dto.response.FinishedIntvDailyCountResponse;
import com.gamyeon.intv.presentation.dto.response.IntvResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intvs")
@Slf4j
public class IntvController {

  private final CreateUseCase createUseCase;
  private final ChangeStateUseCase changeStateUseCase;
  private final UpdateTitleUseCase updateTitleUseCase;
  private final GetFinishedIntvStatsUseCase getFinishedIntvStatsUseCase;

  public IntvController(
      CreateUseCase createUseCase,
      ChangeStateUseCase changeStateUseCase,
      UpdateTitleUseCase updateTitleUseCase,
      GetFinishedIntvStatsUseCase getFinishedIntvStatsUseCase) {
    this.createUseCase = createUseCase;
    this.changeStateUseCase = changeStateUseCase;
    this.updateTitleUseCase = updateTitleUseCase;
    this.getFinishedIntvStatsUseCase = getFinishedIntvStatsUseCase;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<IntvResponse>> create(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody IntvRequest request) {
    log.info("Received create intv request. userId={}, title={}", userId, request.title());
    IntvInfo info = createUseCase.create(request.toCreateCommand(userId));

    return ApiResponse.success(IntvSuccessCode.INTV_CREATED, IntvResponse.from(info));
  }

  @PatchMapping("/{intvId}")
  public ResponseEntity<ApiResponse<IntvResponse>> update(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long intvId,
      @Valid @RequestBody IntvRequest request) {
    log.info(
        "Received update intv request. userId={}, intvId={}, title={}",
        userId,
        intvId,
        request.title());
    IntvInfo info = updateTitleUseCase.updateTitle(request.toUpdateCommand(userId, intvId));

    return ApiResponse.success(IntvSuccessCode.INTV_UPDATED, IntvResponse.from(info));
  }

  @PatchMapping("/{intvId}/start")
  public ResponseEntity<ApiResponse<Void>> start(
      @AuthenticationPrincipal Long userId, @PathVariable Long intvId) {
    log.info("Received start intv request. userId={}, intvId={}", userId, intvId);
    changeStateUseCase.start(new ChangeStateIntvCommand(userId, intvId));

    return ApiResponse.success(IntvSuccessCode.INTV_STARTED);
  }

  @PatchMapping("/{intvId}/pause")
  public ResponseEntity<ApiResponse<Void>> pauseIntv(
      @AuthenticationPrincipal Long userId, @PathVariable Long intvId) {
    log.info("Received pause intv request. userId={}, intvId={}", userId, intvId);
    changeStateUseCase.pause(new ChangeStateIntvCommand(userId, intvId));

    return ApiResponse.success(IntvSuccessCode.INTV_PAUSED);
  }

  @PatchMapping("/{intvId}/resume")
  public ResponseEntity<ApiResponse<Void>> resumeIntv(
      @AuthenticationPrincipal Long userId, @PathVariable Long intvId) {
    log.info("Received resume intv request. userId={}, intvId={}", userId, intvId);
    changeStateUseCase.resume(new ChangeStateIntvCommand(userId, intvId));

    return ApiResponse.success(IntvSuccessCode.INTV_RESUMED);
  }

  @PatchMapping("/{intvId}/finish")
  public ResponseEntity<ApiResponse<Void>> finishIntv(
      @AuthenticationPrincipal Long userId, @PathVariable Long intvId) {
    log.info("Received finish intv request. userId={}, intvId={}", userId, intvId);
    changeStateUseCase.finish(new ChangeStateIntvCommand(userId, intvId));

    return ApiResponse.success(IntvSuccessCode.INTV_FINISHED);
  }

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<List<FinishedIntvDailyCountResponse>>> getFinishedIntvStats(
      @AuthenticationPrincipal Long userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    log.info(
        "Received finished intv stats request. userId={}, startDate={}, endDate={}",
        userId,
        startDate,
        endDate);

    List<FinishedIntvDailyCountInfo> infos =
        getFinishedIntvStatsUseCase.getFinishedIntvStats(userId, startDate, endDate);
    List<FinishedIntvDailyCountResponse> response =
        infos.stream().map(FinishedIntvDailyCountResponse::from).toList();

    return ApiResponse.success(IntvSuccessCode.INTV_FINISHED_STATS_FETCHED, response);
  }
}
