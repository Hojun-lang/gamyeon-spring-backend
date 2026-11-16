package com.gamyeon.notif.infrastructure.scheduler;

import com.gamyeon.notif.application.service.NotifService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Nginx 및 AWS ALB의 Idle Timeout(60초) 연결 끊김 방지를 위한 하트비트 엔진입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifHeartbeatScheduler {

  private final NotifService notifService;

  /** 매 30초마다 동작하여 파이프가 연결된 모든 브라우저에 핑을 날립니다. */
  @Scheduled(fixedRate = 30000) // 30초 (단위: ms)
  public void scheduleHeartbeat() {
    notifService.sendHeartbeat();
  }
}
