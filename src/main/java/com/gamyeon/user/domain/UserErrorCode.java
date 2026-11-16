package com.gamyeon.user.domain;

import com.gamyeon.common.response.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {

  // ── V : Validation ────────────────────────────────
  INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "닉네임은 1~8자의 한글, 영어, 숫자만 허용됩니다.", "USER-V003"),

  // ── B : Business ──────────────────────────────────
  DEACTIVATED_USER(HttpStatus.BAD_REQUEST, "탈퇴하거나 정지된 계정입니다.", "USER-B002"),
  OAUTH_LOGIN_USER(HttpStatus.BAD_REQUEST, "소셜 로그인으로 가입된 계정입니다.", "USER-B003"),
  ACCOUNT_NOT_RESTORABLE(HttpStatus.BAD_REQUEST, "복구할 수 없는 계정입니다.", "USER-B004"),
  INVALID_RESTORE_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 계정 복구 토큰입니다.", "USER-B005"),

  // ── N : NotFound ──────────────────────────────────
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", "USER-N001");

  private final HttpStatus status;
  private final String message;
  private final String code;

  UserErrorCode(HttpStatus status, String message, String code) {
    this.status = status;
    this.message = message;
    this.code = code;
  }

  @Override
  public HttpStatus getStatus() {
    return status;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String getCode() {
    return code;
  }
}
