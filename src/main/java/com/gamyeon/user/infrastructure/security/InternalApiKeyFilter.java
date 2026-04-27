package com.gamyeon.user.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalApiKeyFilter extends OncePerRequestFilter {

  private static final String INTERNAL_PATH_PREFIX = "/internal/";
  private static final String API_KEY_HEADER = "X-Internal-API-Key";

  private final String internalApiKey;
  private final ObjectMapper objectMapper;

  public InternalApiKeyFilter(String internalApiKey, ObjectMapper objectMapper) {
    this.internalApiKey = internalApiKey;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String key = request.getHeader(API_KEY_HEADER);
    if (!internalApiKey.equals(key)) {
      response.setStatus(CommonErrorCode.FORBIDDEN.getStatus().value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response
          .getWriter()
          .write(objectMapper.writeValueAsString(ErrorResponse.of(CommonErrorCode.FORBIDDEN)));
      return;
    }
    filterChain.doFilter(request, response);
  }
}
