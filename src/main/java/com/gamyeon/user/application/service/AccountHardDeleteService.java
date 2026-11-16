package com.gamyeon.user.application.service;

import com.gamyeon.common.storage.application.port.out.StorageDeletePort;
import com.gamyeon.user.application.port.outbound.AccountDataDeletionPort;
import com.gamyeon.user.application.port.outbound.UserRepository;
import com.gamyeon.user.domain.AccountDeletionDeadlinePolicy;
import com.gamyeon.user.domain.User;
import com.gamyeon.user.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountHardDeleteService {

  private final UserRepository userRepository;
  private final AccountDataDeletionPort accountDataDeletionPort;
  private final StorageDeletePort storageDeletePort;
  private final AccountDeletionDeadlinePolicy deadlinePolicy;

  public List<Long> findTargetIds(int batchSize) {
    return userRepository.findHardDeleteTargetIds(
        UserStatus.WITHDREW, deadlinePolicy.hardDeleteThreshold(), batchSize);
  }

  @Transactional
  public boolean hardDelete(Long userId) {
    User user = userRepository.findByIdForUpdate(userId).orElse(null);
    LocalDateTime threshold = deadlinePolicy.hardDeleteThreshold();
    if (user == null
        || !user.isWithdrew()
        || user.getWithdrawnAt() == null
        || !user.getWithdrawnAt().isBefore(threshold)) {
      return false;
    }

    storageDeletePort.deleteAll(accountDataDeletionPort.findFileKeys(userId));
    accountDataDeletionPort.deleteAllByUserId(userId);
    userRepository.delete(user);
    return true;
  }
}
