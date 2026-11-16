package com.gamyeon.user.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.response.ApiResponse;
import com.gamyeon.common.response.ErrorCode;
import com.gamyeon.user.application.port.outbound.TokenPort;
import com.gamyeon.user.application.port.outbound.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final List<String> PUBLIC_PATHS =
      List.of(
          "/api/v1/auth/login/**",
          "/api/v1/auth/reissue",
          "/api/v1/auth/account/restore",
          "/internal/**",
          "/health",
          "/actuator/**");

  private final TokenPort tokenPort;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public JwtAuthenticationFilter(TokenPort tokenPort, ObjectMapper objectMapper) {
    this(tokenPort, objectMapper, null);
  }

  public JwtAuthenticationFilter(
      TokenPort tokenPort, ObjectMapper objectMapper, UserRepository userRepository) {
    this.tokenPort = tokenPort;
    this.objectMapper = objectMapper;
    this.userRepository = userRepository;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // 토큰 추출 로직을 별도 메서드로 분리하여 호출
    String token = resolveToken(request);

    // 토큰이 헤더에도 없고, 쿠키에도 없으면 에러 처리
    if (token == null) {
      writeError(response, CommonErrorCode.UNAUTHORIZED);
      return;
    }

    try {
      Long userId = tokenPort.getUserId(token);
      if (userRepository != null
          && userRepository.findById(userId).filter(user -> user.isActive()).isEmpty()) {
        writeError(response, CommonErrorCode.UNAUTHORIZED);
        return;
      }

      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(userId, null, List.of());
      SecurityContextHolder.getContext().setAuthentication(auth);

      filterChain.doFilter(request, response);

    } catch (ExpiredJwtException e) {
      writeError(response, CommonErrorCode.EXPIRED_TOKEN);
    } catch (JwtException | IllegalArgumentException e) {
      writeError(response, CommonErrorCode.INVALID_TOKEN);
    }
  }

  /** HttpServletRequest에서 토큰을 추출하는 메서드 1순위: Authorization 헤더 2순위: Cookie (accessToken) */
  private String resolveToken(HttpServletRequest request) {
    // 1. Authorization 헤더에서 확인
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    // 2. 쿠키에서 확인 (프론트엔드 SSE 등 헤더 주입이 어려울 때 우회용)
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("accessToken".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }

    return null;
  }

  private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, null)));
  }
}
