package com.gamyeon.user.application.port.inbound;

import com.gamyeon.user.domain.OAuthProvider;

public class OAuthLoginCommand {

  private final OAuthProvider provider;
  private final String authorizationCode;
  private final String codeVerifier;
  private final String redirectUri;

  private OAuthLoginCommand(
      OAuthProvider provider, String authorizationCode, String codeVerifier, String redirectUri) {
    this.provider = provider;
    this.authorizationCode = authorizationCode;
    this.codeVerifier = codeVerifier;
    this.redirectUri = redirectUri;
  }

  public static OAuthLoginCommand of(
      OAuthProvider provider, String authorizationCode, String codeVerifier, String redirectUri) {
    return new OAuthLoginCommand(provider, authorizationCode, codeVerifier, redirectUri);
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

  public String getRedirectUri() {
    return redirectUri;
  }
}
