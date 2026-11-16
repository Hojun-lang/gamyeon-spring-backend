package com.gamyeon.user.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  private String secret;
  private long accessTokenExpiry = 3_600_000L;
  private long refreshTokenExpiry = 604_800_000L;
  private long restoreTokenExpiry = 600_000L;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getAccessTokenExpiry() {
    return accessTokenExpiry;
  }

  public void setAccessTokenExpiry(long accessTokenExpiry) {
    this.accessTokenExpiry = accessTokenExpiry;
  }

  public long getRefreshTokenExpiry() {
    return refreshTokenExpiry;
  }

  public void setRefreshTokenExpiry(long refreshTokenExpiry) {
    this.refreshTokenExpiry = refreshTokenExpiry;
  }

  public long getRestoreTokenExpiry() {
    return restoreTokenExpiry;
  }

  public void setRestoreTokenExpiry(long restoreTokenExpiry) {
    this.restoreTokenExpiry = restoreTokenExpiry;
  }
}
