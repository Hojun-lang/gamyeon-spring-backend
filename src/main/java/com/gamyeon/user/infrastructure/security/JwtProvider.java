package com.gamyeon.user.infrastructure.security;

import com.gamyeon.user.application.port.outbound.TokenPort;
import com.gamyeon.user.domain.OAuthProvider;
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
  private final long restoreTokenExpiry;
  private static final String TOKEN_TYPE = "tokenType";
  private static final String PROVIDER = "provider";
  private static final String RESTORE = "RESTORE";

  public JwtProvider(JwtProperties properties) {
    this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiry = properties.getAccessTokenExpiry();
    this.refreshTokenExpiry = properties.getRefreshTokenExpiry();
    this.restoreTokenExpiry = properties.getRestoreTokenExpiry();
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
  public String createRestoreToken(Long userId, OAuthProvider provider) {
    Date now = new Date();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim(TOKEN_TYPE, RESTORE)
        .claim(PROVIDER, provider.name())
        .issuedAt(now)
        .expiration(new Date(now.getTime() + restoreTokenExpiry))
        .signWith(secretKey)
        .compact();
  }

  @Override
  public RestoreTokenClaims getRestoreTokenClaims(String token) {
    Claims claims = getClaims(token);
    if (!RESTORE.equals(claims.get(TOKEN_TYPE, String.class))) {
      throw new JwtException("Not an account restore token");
    }
    return new RestoreTokenClaims(
        Long.parseLong(claims.getSubject()),
        OAuthProvider.valueOf(claims.get(PROVIDER, String.class)));
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
    Claims claims = getClaims(token);
    if (RESTORE.equals(claims.get(TOKEN_TYPE, String.class))) {
      throw new JwtException("Account restore token cannot authenticate API requests");
    }
    return Long.parseLong(claims.getSubject());
  }

  public Claims getClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
