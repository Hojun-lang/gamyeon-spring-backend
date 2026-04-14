package com.gamyeon.user.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.user.application.port.outbound.TokenPort;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("JWT 인증 필터 - 토큰 검증 및 경로 접근 제어")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private TokenPort tokenPort;
  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(tokenPort, new ObjectMapper());
    SecurityContextHolder.clearContext();
  }

  // ── 정상 동작 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("TJAF-001 - 유효한 토큰으로 요청 시 userId가 SecurityContext에 저장되어야 한다")
  void shouldStoreUserIdInSecurityContextWhenTokenIsValid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Authorization", "Bearer valid-token");

    given(tokenPort.getUserId("valid-token")).willReturn(1L);

    filter.doFilterInternal(request, response, filterChain);

    assertNotNull(
        SecurityContextHolder.getContext().getAuthentication(),
        "인증 객체가 SecurityContext에 저장되어야 합니다");
    assertEquals(
        1L,
        SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
        "SecurityContext의 principal이 userId와 일치해야 합니다");
    verify(filterChain).doFilter(request, response);
  }

  // ── 401 케이스 ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("TJAF-002 - Authorization 헤더가 없으면 401을 반환해야 한다")
  void shouldReturn401WhenAuthorizationHeaderIsMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertEquals(401, response.getStatus(), "Authorization 헤더가 없으면 401을 반환해야 합니다");
    assertTrue(
        response.getContentAsString().contains(CommonErrorCode.UNAUTHORIZED.getCode()),
        "응답 본문에 UNAUTHORIZED 에러코드가 포함되어야 합니다");
    verify(filterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
  }

  @Test
  @DisplayName("TJAF-003 - Bearer 접두사가 없는 Authorization 헤더면 401을 반환해야 한다")
  void shouldReturn401WhenAuthorizationHeaderHasNoBearerPrefix() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Authorization", "Basic some-token");

    filter.doFilterInternal(request, response, filterChain);

    assertEquals(401, response.getStatus(), "Bearer 접두사가 없으면 401을 반환해야 합니다");
    verify(filterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
  }

  @Test
  @DisplayName("TJAF-004 - 만료된 토큰이면 401과 EXPIRED_TOKEN 에러코드를 반환해야 한다")
  void shouldReturn401WithExpiredTokenCodeWhenTokenIsExpired() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Authorization", "Bearer expired-token");

    given(tokenPort.getUserId("expired-token"))
        .willThrow(new ExpiredJwtException(null, null, "expired"));

    filter.doFilterInternal(request, response, filterChain);

    assertEquals(401, response.getStatus(), "만료된 토큰이면 401을 반환해야 합니다");
    assertTrue(
        response.getContentAsString().contains(CommonErrorCode.EXPIRED_TOKEN.getCode()),
        "응답 본문에 EXPIRED_TOKEN 에러코드가 포함되어야 합니다");
    verify(filterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
  }

  @Test
  @DisplayName("TJAF-005 - 유효하지 않은 토큰이면 401과 INVALID_TOKEN 에러코드를 반환해야 한다")
  void shouldReturn401WithInvalidTokenCodeWhenTokenIsInvalid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Authorization", "Bearer invalid-token");

    given(tokenPort.getUserId("invalid-token")).willThrow(new JwtException("invalid"));

    filter.doFilterInternal(request, response, filterChain);

    assertEquals(401, response.getStatus(), "유효하지 않은 토큰이면 401을 반환해야 합니다");
    assertTrue(
        response.getContentAsString().contains(CommonErrorCode.INVALID_TOKEN.getCode()),
        "응답 본문에 INVALID_TOKEN 에러코드가 포함되어야 합니다");
    verify(filterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
  }

  // ── public 경로 skip ─────────────────────────────────────────────────────

  @Test
  @DisplayName("TJAF-006 - 로그인 경로는 필터를 건너뛰어야 한다")
  void shouldSkipFilterWhenPathIsLoginEndpoint() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/auth/login/google");

    assertTrue(filter.shouldNotFilter(request), "로그인 경로는 필터를 건너뛰어야 합니다");
  }

  @Test
  @DisplayName("TJAF-007 - 토큰 재발급 경로는 필터를 건너뛰어야 한다")
  void shouldSkipFilterWhenPathIsReissueEndpoint() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/auth/reissue");

    assertTrue(filter.shouldNotFilter(request), "토큰 재발급 경로는 필터를 건너뛰어야 합니다");
  }

  @Test
  @DisplayName("TJAF-008 - 헬스체크 경로는 필터를 건너뛰어야 한다")
  void shouldSkipFilterWhenPathIsHealthEndpoint() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/health");

    assertTrue(filter.shouldNotFilter(request), "헬스체크 경로는 필터를 건너뛰어야 합니다");
  }

  @Test
  @DisplayName("TJAF-009 - 보호된 API 경로는 필터가 적용되어야 한다")
  void shouldNotSkipFilterWhenPathIsProtectedEndpoint() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/users/me");

    assertFalse(filter.shouldNotFilter(request), "보호된 API 경로는 필터가 적용되어야 합니다");
  }
}
