package com.gamyeon.user.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class AccountDeletionDeadlinePolicy {

  public static final int RETENTION_DAYS = 7;
  public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

  private final Clock clock;
  private final ZoneId zoneId;

  public AccountDeletionDeadlinePolicy(Clock clock, ZoneId zoneId) {
    this.clock = clock;
    this.zoneId = zoneId;
  }

  public LocalDateTime deletionDeadline(LocalDateTime withdrawnAt) {
    LocalDateTime afterRetention = withdrawnAt.plusDays(RETENTION_DAYS);
    return afterRetention.toLocalDate().plusDays(1).atStartOfDay();
  }

  public LocalDateTime hardDeleteThreshold() {
    return LocalDate.now(clock.withZone(zoneId)).minusDays(RETENTION_DAYS).atStartOfDay();
  }

  public boolean isRestorable(LocalDateTime withdrawnAt) {
    return withdrawnAt != null
        && LocalDateTime.now(clock.withZone(zoneId)).isBefore(deletionDeadline(withdrawnAt));
  }
}
