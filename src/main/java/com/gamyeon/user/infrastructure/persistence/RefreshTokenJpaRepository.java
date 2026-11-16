package com.gamyeon.user.infrastructure.persistence;

import com.gamyeon.user.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  void deleteByUserId(Long userId);

  @Modifying
  @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :threshold")
  int deleteAllExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
