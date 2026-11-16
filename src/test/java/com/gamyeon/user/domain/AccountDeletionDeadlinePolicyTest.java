package com.gamyeon.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AccountDeletionDeadlinePolicyTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  @Test
  void calculatesFirstMidnightAfterFullSevenDays() {
    AccountDeletionDeadlinePolicy policy =
        new AccountDeletionDeadlinePolicy(
            Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), SEOUL), SEOUL);

    assertEquals(
        LocalDateTime.of(2026, 7, 27, 0, 0),
        policy.deletionDeadline(LocalDateTime.of(2026, 7, 19, 15, 30)));
  }

  @Test
  void calculatesStrictHardDeleteThresholdAtSeoulMidnight() {
    AccountDeletionDeadlinePolicy policy =
        new AccountDeletionDeadlinePolicy(
            Clock.fixed(Instant.parse("2026-07-26T15:00:00Z"), SEOUL), SEOUL);

    assertEquals(LocalDateTime.of(2026, 7, 20, 0, 0), policy.hardDeleteThreshold());
    assertTrue(policy.isRestorable(LocalDateTime.of(2026, 7, 20, 0, 1)));
    assertFalse(policy.isRestorable(LocalDateTime.of(2026, 7, 19, 15, 30)));
  }
}
