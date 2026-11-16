package com.gamyeon.user.infrastructure.scheduler;

import com.gamyeon.user.application.port.outbound.RefreshTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * 만료된 refresh token을 주기적으로 정리한다.
   *
   * <p>기본 주기: 매일 새벽 3시 (cron = "0 0 3 * * *")
   *
   * <p>application.yml 에서 {@code jwt.refresh-token-cleanup.cron} 으로 변경 가능하다.
   */
  @Scheduled(cron = "${jwt.refresh-token-cleanup.cron:0 0 3 * * *}")
  @Transactional
  public void deleteExpiredTokens() {
    LocalDateTime now = LocalDateTime.now();
    log.info("[RefreshToken] 만료 토큰 정리 시작 - 기준 시각: {}", now);

    int deleted = refreshTokenRepository.deleteAllExpiredBefore(now);

    if (deleted > 0) {
      log.info("[RefreshToken] 만료 토큰 정리 완료 - 삭제 건수: {}건", deleted);
    } else {
      log.debug("[RefreshToken] 만료 토큰 정리 - 삭제 대상 없음");
    }
  }
}
