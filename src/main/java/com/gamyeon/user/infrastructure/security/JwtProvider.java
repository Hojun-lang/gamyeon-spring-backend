package com.gamyeon.user.infrastructure.security;

import com.gamyeon.user.application.port.outbound.TokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

public class JwtProvider implements TokenPort {

  private final SecretKey secretKey;
  private final long accessTokenExpiry;
  private final long refreshTokenExpiry;

  public JwtProvider(JwtProperties properties) {
    this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiry = properties.getAccessTokenExpiry();
    this.refreshTokenExpiry = properties.getRefreshTokenExpiry();
  }

  @Override
  public String createAccessToken(Long userId) {
    Date now = new Date();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(new Date(now.getTime() + accessTokenExpiry))
        .signWith(secretKey)
        .compact();
  }

  @Override
  public String createRefreshToken(Long userId) {
    Date now = new Date();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(new Date(now.getTime() + refreshTokenExpiry))
        .signWith(secretKey)
        .compact();
  }

  @Override
  public long getRefreshTokenExpiry() {
    return refreshTokenExpiry;
  }

  @Override
  public boolean validateToken(String token) {
    try {
      getClaims(token);
      return true;
    } catch (ExpiredJwtException e) {
      throw e;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public Long getUserId(String token) {
    return Long.parseLong(getClaims(token).getSubject());
  }

  public Claims getClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
