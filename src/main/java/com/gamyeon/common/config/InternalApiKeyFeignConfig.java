package com.gamyeon.common.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalApiKeyFeignConfig {

  @Value("${internal.api-key}")
  private String internalApiKey;

  @Bean
  public RequestInterceptor internalApiKeyInterceptor() {
    return requestTemplate -> requestTemplate.header("X-Internal-API-Key", internalApiKey);
  }
}
