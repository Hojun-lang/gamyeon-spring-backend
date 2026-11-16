package com.gamyeon.user.application.service;

public class NicknameResolver {

  private static final int MAX_NICKNAME_LENGTH = 8;

  /**
   * OAuth 제공자 닉네임을 정제하여 반환한다.
   *
   * <p>불허 문자(공백·특수문자 등)는 제거하고, 8자를 초과하면 잘라낸다. 정제 후 비어있으면 이메일 localPart로 대체한다.
   *
   * <p>허용 문자: 한글(가-힣), 영문(a-z, A-Z), 숫자(0-9), 언더스코어(_)
   */
  public String resolve(String providerNickname, String email) {
    if (providerNickname != null && !providerNickname.isBlank()) {
      String sanitized = sanitize(providerNickname);
      if (!sanitized.isEmpty()) {
        return sanitized;
      }
    }
    return extractFromEmail(email);
  }

  private String sanitize(String nickname) {
    // 허용 문자 이외 제거 후 최대 길이 truncate
    String cleaned = nickname.replaceAll("[^가-힣a-zA-Z0-9_]", "");
    if (cleaned.length() > MAX_NICKNAME_LENGTH) {
      cleaned = cleaned.substring(0, MAX_NICKNAME_LENGTH);
    }
    return cleaned;
  }

  private String extractFromEmail(String email) {
    if (email == null || email.isBlank()) {
      return "user" + (int) (Math.random() * 100000);
    }
    String localPart = email.contains("@") ? email.split("@")[0] : email;
    // 이메일 localPart도 동일 규칙으로 정제
    localPart = sanitize(localPart);
    if (localPart.isEmpty()) {
      return "user" + (int) (Math.random() * 100000);
    }
    return localPart;
  }
}
