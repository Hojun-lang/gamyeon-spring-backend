package com.gamyeon.user.infrastructure.persistence;

import com.gamyeon.user.application.port.outbound.RefreshTokenRepository;
import com.gamyeon.user.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository jpaRepository;

  public RefreshTokenRepositoryImpl(RefreshTokenJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    return jpaRepository.save(refreshToken);
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return jpaRepository.findByToken(token);
  }

  @Override
  public void deleteByUserId(Long userId) {
    jpaRepository.deleteByUserId(userId);
  }

  @Override
  public int deleteAllExpiredBefore(LocalDateTime threshold) {
    return jpaRepository.deleteAllExpiredBefore(threshold);
  }
}
