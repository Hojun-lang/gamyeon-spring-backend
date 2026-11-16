package com.gamyeon.notif.domain;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 알림(Notif) 도메인 전역에서 사용하는 성공 코드 명세 Enum입니다. 프로젝트 공통 ApiResponse 구조에 주입되어 사용됩니다. */
@Getter
public enum NotifSuccessCode
    implements com.gamyeon.common.response.SuccessCode { // 전역 SuccessCode 인터페이스 상속
  NOTIF_LIST_FETCHED(HttpStatus.OK, "NTF-S000", "success"),
  NOTIF_READ(HttpStatus.OK, "NTF-S000", "success"),
  NOTIF_ALL_READ(HttpStatus.OK, "NTF-S000", "success");

  private final HttpStatus status;
  private final String code;
  private final String message;

  NotifSuccessCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }
}
