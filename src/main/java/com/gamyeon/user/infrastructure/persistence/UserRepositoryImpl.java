package com.gamyeon.user.infrastructure.persistence;

import com.gamyeon.user.application.port.outbound.UserRepository;
import com.gamyeon.user.domain.OAuthProvider;
import com.gamyeon.user.domain.User;
import com.gamyeon.user.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

  private final UserJpaRepository jpaRepository;

  public UserRepositoryImpl(UserJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId) {
    return jpaRepository.findByProviderAndProviderId(provider, providerId);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return jpaRepository.findByEmail(email);
  }

  @Override
  public Optional<User> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public Optional<User> findByIdForUpdate(Long id) {
    return jpaRepository.findByIdForUpdate(id);
  }

  @Override
  public List<Long> findHardDeleteTargetIds(
      UserStatus status, LocalDateTime withdrawnBefore, int batchSize) {
    return jpaRepository.findHardDeleteTargetIds(
        status, withdrawnBefore, PageRequest.of(0, batchSize));
  }

  @Override
  public User save(User user) {
    return jpaRepository.save(user);
  }

  @Override
  public void delete(User user) {
    jpaRepository.delete(user);
  }
}
