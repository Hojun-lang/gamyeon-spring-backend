package com.gamyeon.user.infrastructure.scheduler;

import com.gamyeon.user.application.service.AccountHardDeleteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountHardDeleteScheduler {

  private final AccountHardDeleteService hardDeleteService;

  @Value("${user.account-deletion.batch-size:100}")
  private int batchSize;

  @Scheduled(
      cron = "${user.account-deletion.cron:0 0 0 * * *}",
      zone = "${user.account-deletion.zone:Asia/Seoul}")
  public void deleteWithdrawnAccounts() {
    int deletedCount = 0;
    int failedCount = 0;

    while (true) {
      List<Long> targetIds = hardDeleteService.findTargetIds(batchSize);
      if (targetIds.isEmpty()) {
        break;
      }

      int completedInBatch = 0;
      for (Long userId : targetIds) {
        try {
          if (hardDeleteService.hardDelete(userId)) {
            deletedCount++;
          }
          completedInBatch++;
        } catch (RuntimeException e) {
          failedCount++;
          log.error("[AccountDeletion] hard delete failed. userId={}", userId, e);
        }
      }

      if (targetIds.size() < batchSize || completedInBatch == 0 || failedCount > 0) {
        break;
      }
    }

    log.info(
        "[AccountDeletion] hard delete completed. deletedCount={}, failedCount={}",
        deletedCount,
        failedCount);
  }
}
