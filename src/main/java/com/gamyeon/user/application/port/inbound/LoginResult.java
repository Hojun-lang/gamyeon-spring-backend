package com.gamyeon.user.application.port.inbound;

public class LoginResult {

  private final String accessToken;
  private final String refreshToken;
  private final UserInfo user;
  private final String restoreToken;
  private final java.time.LocalDateTime restorableUntil;

  private LoginResult(
      String accessToken,
      String refreshToken,
      UserInfo user,
      String restoreToken,
      java.time.LocalDateTime restorableUntil) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.user = user;
    this.restoreToken = restoreToken;
    this.restorableUntil = restorableUntil;
  }

  public static LoginResult of(String accessToken, String refreshToken, UserInfo user) {
    return new LoginResult(accessToken, refreshToken, user, null, null);
  }

  public static LoginResult restoreRequired(
      String restoreToken, java.time.LocalDateTime restorableUntil) {
    return new LoginResult(null, null, null, restoreToken, restorableUntil);
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public UserInfo getUser() {
    return user;
  }

  public boolean isRestoreRequired() {
    return restoreToken != null;
  }

  public String getRestoreToken() {
    return restoreToken;
  }

  public java.time.LocalDateTime getRestorableUntil() {
    return restorableUntil;
  }
}
