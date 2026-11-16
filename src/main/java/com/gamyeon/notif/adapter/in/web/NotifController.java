package com.gamyeon.notif.adapter.in.web;

import com.gamyeon.common.response.ApiResponse;
import com.gamyeon.notif.application.port.in.GetNotifListUseCase;
import com.gamyeon.notif.application.port.in.ReadAllNotifsUseCase;
import com.gamyeon.notif.application.port.in.ReadNotifUseCase;
import com.gamyeon.notif.application.port.in.SubscribeNotifUseCase;
import com.gamyeon.notif.application.port.in.dto.NotifListResponse;
import com.gamyeon.notif.domain.NotifSuccessCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifs")
@RequiredArgsConstructor
@Slf4j
public class NotifController {

  private final SubscribeNotifUseCase subscribeNotifUseCase;
  private final GetNotifListUseCase getNotifListUseCase;
  private final ReadNotifUseCase readNotifUseCase;
  private final ReadAllNotifsUseCase readAllNotifsUseCase;

  /** 1. SSE 커넥션 구독 API 브라우저의 EventSource 요청을 수락합니다. (공통 포맷을 타지 않고 SseEmitter를 직접 반환) */
  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> subscribe(@AuthenticationPrincipal Long userId) {
    log.info("Received SSE subscribe request. userId={}", userId);
    SseEmitter emitter = subscribeNotifUseCase.subscribe(userId);
    return ResponseEntity.ok(emitter);
  }

  /** 2. 알림 목록 최신순 조회 API */
  @GetMapping
  public ResponseEntity<ApiResponse<NotifListResponse>> getNotifications(
      @AuthenticationPrincipal Long userId,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "5") int size) {
    log.info(
        "Received get notification list request. userId={}, cursorId={}, size={}",
        userId,
        cursorId,
        size);
    NotifListResponse responseDto = getNotifListUseCase.getNotifs(userId, cursorId, size);

    return ApiResponse.success(NotifSuccessCode.NOTIF_LIST_FETCHED, responseDto);
  }

  /** 3. 개별 알림 읽음 처리 API */
  @PatchMapping("/{notifId}/read")
  public ResponseEntity<ApiResponse<Void>> readNotification(
      @AuthenticationPrincipal Long userId, @PathVariable Long notifId) {
    log.info("Received read notification request. userId={}, notifId={}", userId, notifId);
    readNotifUseCase.readNotif(userId, notifId);

    return ApiResponse.success(NotifSuccessCode.NOTIF_READ);
  }

  /** 4. 모든 알림 일괄 읽음 처리 API */
  @PatchMapping("/read-all")
  public ResponseEntity<ApiResponse<Void>> readAllNotifications(
      @AuthenticationPrincipal Long userId) {
    log.info("Received read all notifications request. userId={}", userId);
    readAllNotifsUseCase.readAllNotifs(userId);

    return ApiResponse.success(NotifSuccessCode.NOTIF_ALL_READ);
  }
}
