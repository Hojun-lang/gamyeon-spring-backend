package com.gamyeon.user.application.port.outbound;

import com.gamyeon.user.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {

  RefreshToken save(RefreshToken refreshToken);

  Optional<RefreshToken> findByToken(String token);

  void deleteByUserId(Long userId);

  /** threshold 이전에 만료된 토큰을 일괄 삭제한다. 삭제된 건수를 반환한다. */
  int deleteAllExpiredBefore(LocalDateTime threshold);
}
