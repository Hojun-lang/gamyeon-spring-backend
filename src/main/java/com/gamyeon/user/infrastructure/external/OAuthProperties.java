package com.gamyeon.user.infrastructure.external;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

  private Provider google = new Provider();
  private Provider kakao = new Provider();

  public Provider getGoogle() {
    return google;
  }

  public void setGoogle(Provider google) {
    this.google = google;
  }

  public Provider getKakao() {
    return kakao;
  }

  public void setKakao(Provider kakao) {
    this.kakao = kakao;
  }

  public static class Provider {

    private static final Logger log = LoggerFactory.getLogger(Provider.class);

    private String clientId;
    private String clientSecret;
    private List<String> redirectUris = new ArrayList<>();

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
    }

    public List<String> getRedirectUris() {
      return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
      this.redirectUris = redirectUris;
    }

    /**
     * 프론트엔드가 전달한 redirectUri가 허용 목록에 있는지 검증합니다.
     *
     * <p>ERROR: 환경변수 바인딩 실패로 목록이 비어 있는 경우
     *
     * <p>ERROR: 허용 목록에 없는 URI가 요청된 경우 (잘못된 클라이언트 설정 또는 비정상 요청)
     *
     * <p>DEBUG: 검증 성공 시 URI 출력
     */
    public boolean isAllowedRedirectUri(String redirectUri) {
      // ERROR: 환경변수 OAUTH_*_REDIRECT_URIS 바인딩 자체가 실패한 경우
      if (redirectUris.isEmpty()) {
        log.error(
            "[OAuth] redirectUris 목록이 비어 있습니다. "
                + "환경변수 OAUTH_*_REDIRECT_URIS가 올바르게 설정되었는지 확인하세요.");
        return false;
      }

      // DEBUG: 허용 목록 전체 출력
      log.debug("[OAuth] 등록된 redirectUris 허용 목록 (총 {}개): {}", redirectUris.size(), redirectUris);

      boolean allowed = redirectUri != null && redirectUris.contains(redirectUri);

      if (allowed) {
        log.debug("[OAuth] redirectUri 검증 성공: '{}'", redirectUri);
      } else {
        // ERROR: 허용되지 않은 URI — 잘못된 클라이언트 설정이거나 비정상 요청
        log.error("[OAuth] 허용되지 않은 redirectUri 요청: '{}'. 허용 목록: {}", redirectUri, redirectUris);
      }

      return allowed;
    }
  }
}
