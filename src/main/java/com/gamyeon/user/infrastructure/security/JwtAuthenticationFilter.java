package com.gamyeon.user.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.response.ApiResponse;
import com.gamyeon.common.response.ErrorCode;
import com.gamyeon.user.application.port.outbound.TokenPort;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
          "/internal/**",
          "/health",
          "/actuator/**");

  private final TokenPort tokenPort;
  private final ObjectMapper objectMapper;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public JwtAuthenticationFilter(TokenPort tokenPort, ObjectMapper objectMapper) {
    this.tokenPort = tokenPort;
    this.objectMapper = objectMapper;
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

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      writeError(response, CommonErrorCode.UNAUTHORIZED);
      return;
    }

    String token = authHeader.substring(7);

    try {
      Long userId = tokenPort.getUserId(token);

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
