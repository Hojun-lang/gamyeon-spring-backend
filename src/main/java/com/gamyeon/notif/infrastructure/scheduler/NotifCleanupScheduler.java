package com.gamyeon.notif.infrastructure.scheduler;

import com.gamyeon.notif.application.service.NotifService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** DB 용량 최적화를 위해 오래된 알림을 주기적으로 Hard Delete 하는 배치 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifCleanupScheduler {

  private final NotifService notifService;
  private static final int RETENTION_DAYS = 7; // 알림 보관 주기: 7일

  /** 매일 새벽 4시 0분 0초에 동작합니다. */
  @Scheduled(cron = "0 0 4 * * *")
  public void scheduleOldNotificationCleanup() {
    log.info("오래된 알림 데이터 배치 청소 스케줄러 가동");
    notifService.cleanupOldNotifications(RETENTION_DAYS);
  }
}
