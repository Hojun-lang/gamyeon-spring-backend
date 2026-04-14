package com.gamyeon.user.infrastructure.di;

import com.gamyeon.user.application.port.inbound.AuthUseCase;
import com.gamyeon.user.application.port.inbound.UserUseCase;
import com.gamyeon.user.application.port.outbound.OAuthPort;
import com.gamyeon.user.application.port.outbound.RefreshTokenRepository;
import com.gamyeon.user.application.port.outbound.TokenPort;
import com.gamyeon.user.application.port.outbound.UserRepository;
import com.gamyeon.user.application.service.AuthService;
import com.gamyeon.user.application.service.NicknameResolver;
import com.gamyeon.user.application.service.UserService;
import com.gamyeon.user.infrastructure.external.OAuthAdapter;
import com.gamyeon.user.infrastructure.external.OAuthProperties;
import com.gamyeon.user.infrastructure.security.JwtProperties;
import com.gamyeon.user.infrastructure.security.JwtProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, OAuthProperties.class})
public class UserConfig {

  @Bean
  public TokenPort tokenPort(JwtProperties jwtProperties) {
    return new JwtProvider(jwtProperties);
  }

  @Bean
  public NicknameResolver nicknameResolver() {
    return new NicknameResolver();
  }

  @Bean
  public WebClient webClient() {
    return WebClient.builder().build();
  }

  @Bean
  public OAuthPort oAuthAdapter(WebClient webClient, OAuthProperties oAuthProperties) {
    return new OAuthAdapter(webClient, oAuthProperties);
  }

  @Bean
  public AuthUseCase authUseCase(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      OAuthPort oAuthPort,
      TokenPort tokenPort,
      NicknameResolver nicknameResolver) {
    return new AuthService(
        userRepository, refreshTokenRepository, oAuthPort, tokenPort, nicknameResolver);
  }

  @Bean
  public UserUseCase userUseCase(
      UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
    return new UserService(userRepository, refreshTokenRepository);
  }
}
