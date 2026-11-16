package com.gamyeon.user.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.common.config.SecurityFilterConfigurer;
import com.gamyeon.user.application.port.outbound.TokenPort;
import com.gamyeon.user.application.port.outbound.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class JwtSecurityFilterConfigurer implements SecurityFilterConfigurer {

  private final TokenPort tokenPort;
  private final ObjectMapper objectMapper;
  private final String internalApiKey;
  private final UserRepository userRepository;

  public JwtSecurityFilterConfigurer(
      TokenPort tokenPort,
      ObjectMapper objectMapper,
      UserRepository userRepository,
      @Value("${internal.api-key}") String internalApiKey) {
    this.tokenPort = tokenPort;
    this.objectMapper = objectMapper;
    this.userRepository = userRepository;
    this.internalApiKey = internalApiKey;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http.addFilterBefore(
            new InternalApiKeyFilter(internalApiKey, objectMapper),
            UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(
            new JwtAuthenticationFilter(tokenPort, objectMapper, userRepository),
            UsernamePasswordAuthenticationFilter.class);
  }
}
