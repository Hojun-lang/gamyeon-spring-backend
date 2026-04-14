package com.gamyeon.user.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JWT 토큰 발급 및 검증")
class JwtProviderTest {

  private JwtProvider jwtProvider;

  @BeforeEach
  void setUp() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-secret-key-must-be-at-least-32-characters!!");
    properties.setAccessTokenExpiry(3_600_000L);
    properties.setRefreshTokenExpiry(604_800_000L);
    jwtProvider = new JwtProvider(properties);
  }

  @Test
  @DisplayName("TJP-001 - 발급된 액세스 토큰은 유효해야 한다")
  void shouldReturnTrueWhenAccessTokenIsValid() {
    String token = jwtProvider.createAccessToken(1L);

    assertTrue(jwtProvider.validateToken(token), "발급된 액세스 토큰은 유효해야 합니다");
  }

  @Test
  @DisplayName("TJP-002 - 액세스 토큰에서 올바른 userId를 추출할 수 있어야 한다")
  void shouldReturnCorrectUserIdWhenExtractingFromAccessToken() {
    String token = jwtProvider.createAccessToken(42L);

    assertEquals(42L, jwtProvider.getUserId(token), "액세스 토큰에서 추출한 userId가 일치해야 합니다");
  }

  @Test
  @DisplayName("TJP-003 - 리프레시 토큰에서 올바른 userId를 추출할 수 있어야 한다")
  void shouldReturnCorrectUserIdWhenExtractingFromRefreshToken() {
    String token = jwtProvider.createRefreshToken(7L);

    assertEquals(7L, jwtProvider.getUserId(token), "리프레시 토큰에서 추출한 userId가 일치해야 합니다");
  }

  @Test
  @DisplayName("TJP-004 - 잘못된 형식의 토큰은 유효하지 않아야 한다")
  void shouldReturnFalseWhenTokenFormatIsInvalid() {
    assertFalse(jwtProvider.validateToken("invalid.token.value"), "잘못된 형식의 토큰은 유효하지 않아야 합니다");
  }

  @Test
  @DisplayName("TJP-005 - 빈 문자열 토큰은 유효하지 않아야 한다")
  void shouldReturnFalseWhenTokenIsEmpty() {
    assertFalse(jwtProvider.validateToken(""), "빈 문자열 토큰은 유효하지 않아야 합니다");
  }

  @Test
  @DisplayName("TJP-006 - 만료된 토큰 검증 시 ExpiredJwtException이 발생해야 한다")
  void shouldThrowExpiredJwtExceptionWhenValidatingExpiredToken() {
    JwtProvider expiredProvider = providerWithExpiry(-1L);
    String expiredToken = expiredProvider.createAccessToken(1L);

    assertThrows(
        ExpiredJwtException.class,
        () -> expiredProvider.validateToken(expiredToken),
        "만료된 토큰 검증 시 ExpiredJwtException이 발생해야 합니다");
  }

  @Test
  @DisplayName("TJP-007 - 만료된 토큰으로 userId 조회 시 ExpiredJwtException이 발생해야 한다")
  void shouldThrowExpiredJwtExceptionWhenGettingUserIdFromExpiredToken() {
    JwtProvider expiredProvider = providerWithExpiry(-1L);
    String expiredToken = expiredProvider.createAccessToken(1L);

    assertThrows(
        ExpiredJwtException.class,
        () -> expiredProvider.getUserId(expiredToken),
        "만료된 토큰에서 userId 조회 시 ExpiredJwtException이 발생해야 합니다");
  }

  @Test
  @DisplayName("TJP-008 - 다른 시크릿으로 서명된 토큰은 유효하지 않아야 한다")
  void shouldReturnFalseWhenTokenIsSignedWithDifferentSecret() {
    JwtProperties otherProperties = new JwtProperties();
    otherProperties.setSecret("other-secret-key-must-be-at-least-32-characters!");
    otherProperties.setAccessTokenExpiry(3_600_000L);
    JwtProvider otherProvider = new JwtProvider(otherProperties);

    String tokenFromOther = otherProvider.createAccessToken(1L);

    assertFalse(jwtProvider.validateToken(tokenFromOther), "다른 시크릿으로 서명된 토큰은 유효하지 않아야 합니다");
  }

  // ── helpers ─────────────────────────────────────────────────────────────

  private JwtProvider providerWithExpiry(long accessTokenExpiry) {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-secret-key-must-be-at-least-32-characters!!");
    properties.setAccessTokenExpiry(accessTokenExpiry);
    return new JwtProvider(properties);
  }
}
