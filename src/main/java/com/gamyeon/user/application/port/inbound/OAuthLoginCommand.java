package com.gamyeon.user.application.port.inbound;

import com.gamyeon.user.domain.OAuthProvider;

public class OAuthLoginCommand {

  private final OAuthProvider provider;
  private final String authorizationCode;
  private final String codeVerifier;

  private OAuthLoginCommand(OAuthProvider provider, String authorizationCode, String codeVerifier) {
    this.provider = provider;
    this.authorizationCode = authorizationCode;
    this.codeVerifier = codeVerifier;
  }

  public static OAuthLoginCommand of(
      OAuthProvider provider, String authorizationCode, String codeVerifier) {
    return new OAuthLoginCommand(provider, authorizationCode, codeVerifier);
  }

  public OAuthProvider getProvider() {
    return provider;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public String getCodeVerifier() {
    return codeVerifier;
  }
}
