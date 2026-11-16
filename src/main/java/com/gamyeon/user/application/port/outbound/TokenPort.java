package com.gamyeon.user.application.port.outbound;

import com.gamyeon.user.domain.OAuthProvider;

public interface TokenPort {

  String createAccessToken(Long userId);

  String createRefreshToken(Long userId);

  long getRefreshTokenExpiry();

  String createRestoreToken(Long userId, OAuthProvider provider);

  RestoreTokenClaims getRestoreTokenClaims(String token);

  record RestoreTokenClaims(Long userId, OAuthProvider provider) {}

  boolean validateToken(String token);

  Long getUserId(String token);
}
