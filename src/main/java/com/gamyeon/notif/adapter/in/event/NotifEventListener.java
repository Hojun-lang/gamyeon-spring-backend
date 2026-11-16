package com.gamyeon.notif.adapter.in.event;

import com.gamyeon.notif.application.port.in.CreateNotifUseCase;
import com.gamyeon.notif.application.port.in.event.NotifPublishEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifEventListener {

  private final CreateNotifUseCase createNotifUseCase;

  /**
   * [이벤트 기반 비동기 알림 수신부] 타 모듈의 DB 트랜잭션이 '성공적으로 Commit 된 직후(AFTER_COMMIT)'에만 작동하며, 메인 스레드에 병목을 주지 않도록
   * 별도 스레드(@Async)에서 알림을 생성 및 발송합니다.
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleNotifPublishEvent(NotifPublishEvent event) {
    log.info("알림 이벤트 수신 처리 시작 - userId: {}, type: {}", event.userId(), event.type());
    try {
      createNotifUseCase.createAndPush(event);
    } catch (Exception e) {
      // 알림 발송이 실패해도 메인 비즈니스(리포트 생성 등) 롤백에 영향을 주지 않도록 격리
      log.error("비동기 알림 생성 및 푸시 중 오류 발생 - userId: {}", event.userId(), e);
    }
  }
}
