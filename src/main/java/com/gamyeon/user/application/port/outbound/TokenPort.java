package com.gamyeon.user.application.port.outbound;

public interface TokenPort {

  String createAccessToken(Long userId);

  String createRefreshToken(Long userId);

  long getRefreshTokenExpiry();

  boolean validateToken(String token);

  Long getUserId(String token);
}
