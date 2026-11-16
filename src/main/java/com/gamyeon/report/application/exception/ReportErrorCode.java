package com.gamyeon.report.application.exception;

import com.gamyeon.common.response.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ReportErrorCode implements ErrorCode {
  REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "리포트를 찾을 수 없습니다.", "RPRT-N001"),
  REPORT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 리포트에 접근할 권한이 없습니다.", "RPRT-F001"),
  REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 리포트가 생성된 세션입니다.", "RPRT-B002"),
  REPORT_GENERATION_CONDITION_NOT_MET(
      HttpStatus.BAD_REQUEST, "리포트 생성 조건이 충족되지 않았습니다.", "RPRT-B001"),
  REPORT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 리포트 생성에 실패했습니다.", "RPRT-E001");

  private final HttpStatus status;
  private final String message;
  private final String code;

  ReportErrorCode(HttpStatus status, String message, String code) {
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
