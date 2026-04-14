package com.gamyeon.user.infrastructure.external;

import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.exception.CommonException;
import com.gamyeon.user.application.port.outbound.OAuthPort;
import com.gamyeon.user.domain.OAuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class OAuthAdapter implements OAuthPort {

  private static final Logger log = LoggerFactory.getLogger(OAuthAdapter.class);

  private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
  private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
  private static final String KAKAO_USERINFO_URL = "https://kapi.kakao.com/v2/user/me";

  private final WebClient webClient;
  private final OAuthProperties oAuthProperties;

  public OAuthAdapter(WebClient webClient, OAuthProperties oAuthProperties) {
    this.webClient = webClient;
    this.oAuthProperties = oAuthProperties;
  }

  @Override
  public String getAccessToken(
      OAuthProvider provider, String authorizationCode, String codeVerifier) {
    try {
      return switch (provider) {
        case GOOGLE -> fetchGoogleAccessToken(authorizationCode, codeVerifier);
        case KAKAO -> fetchKakaoAccessToken(authorizationCode, codeVerifier);
      };
    } catch (WebClientResponseException e) {
      log.warn(
          "[OAuth] {} 토큰 발급 실패 - status={}, body={}",
          provider,
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new CommonException(CommonErrorCode.UNAUTHORIZED);
    } catch (WebClientException e) {
      log.error("[OAuth] {} 토큰 요청 중 네트워크 오류: {}", provider, e.getMessage(), e);
      throw new CommonException(CommonErrorCode.INTERNAL_ERROR);
    }
  }

  @Override
  public OAuthUserInfo getUserInfo(OAuthProvider provider, String accessToken) {
    try {
      return switch (provider) {
        case GOOGLE -> fetchGoogleUserInfo(accessToken);
        case KAKAO -> fetchKakaoUserInfo(accessToken);
      };
    } catch (WebClientResponseException e) {
      log.warn(
          "[OAuth] {} 사용자 정보 조회 실패 - status={}, body={}",
          provider,
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new CommonException(CommonErrorCode.UNAUTHORIZED);
    } catch (WebClientException e) {
      log.error("[OAuth] {} 사용자 정보 요청 중 네트워크 오류: {}", provider, e.getMessage(), e);
      throw new CommonException(CommonErrorCode.INTERNAL_ERROR);
    }
  }

  private String fetchGoogleAccessToken(String authorizationCode, String codeVerifier) {
    OAuthProperties.Provider google = oAuthProperties.getGoogle();
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("code", authorizationCode);
    params.add("client_id", google.getClientId());
    params.add("client_secret", google.getClientSecret());
    params.add("redirect_uri", google.getRedirectUri());
    params.add("grant_type", "authorization_code");
    params.add("code_verifier", codeVerifier);

    TokenResponse response =
        webClient
            .post()
            .uri(GOOGLE_TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(params))
            .retrieve()
            .bodyToMono(TokenResponse.class)
            .block();

    return response.accessToken();
  }

  private GoogleUserInfo fetchGoogleUserInfo(String accessToken) {
    return webClient
        .get()
        .uri(GOOGLE_USERINFO_URL)
        .headers(h -> h.setBearerAuth(accessToken))
        .retrieve()
        .bodyToMono(GoogleUserInfo.class)
        .block();
  }

  private String fetchKakaoAccessToken(String authorizationCode, String codeVerifier) {
    OAuthProperties.Provider kakao = oAuthProperties.getKakao();
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("code", authorizationCode);
    params.add("client_id", kakao.getClientId());
    params.add("client_secret", kakao.getClientSecret());
    params.add("redirect_uri", kakao.getRedirectUri());
    params.add("grant_type", "authorization_code");
    params.add("code_verifier", codeVerifier);

    TokenResponse response =
        webClient
            .post()
            .uri(KAKAO_TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(params))
            .retrieve()
            .bodyToMono(TokenResponse.class)
            .block();

    return response.accessToken();
  }

  private KakaoUserInfo fetchKakaoUserInfo(String accessToken) {
    return webClient
        .get()
        .uri(KAKAO_USERINFO_URL)
        .headers(h -> h.setBearerAuth(accessToken))
        .retrieve()
        .bodyToMono(KakaoUserInfo.class)
        .block();
  }

  private record TokenResponse(
      @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken) {}
}
