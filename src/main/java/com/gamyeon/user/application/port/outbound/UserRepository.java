package com.gamyeon.user.application.port.outbound;

import com.gamyeon.user.domain.OAuthProvider;
import com.gamyeon.user.domain.User;
import com.gamyeon.user.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

  Optional<User> findByEmail(String email);

  Optional<User> findById(Long id);

  Optional<User> findByIdForUpdate(Long id);

  List<Long> findHardDeleteTargetIds(
      UserStatus status, LocalDateTime withdrawnBefore, int batchSize);

  User save(User user);

  void delete(User user);
}
