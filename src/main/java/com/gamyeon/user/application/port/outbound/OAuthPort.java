package com.gamyeon.user.application.port.outbound;

import com.gamyeon.user.domain.OAuthProvider;

public interface OAuthPort {

  String getAccessToken(OAuthProvider provider, String authorizationCode, String codeVerifier);

  OAuthUserInfo getUserInfo(OAuthProvider provider, String accessToken);

  interface OAuthUserInfo {
    String getEmail();

    String getNickname();

    String getProviderId();
  }
}
