package com.gamyeon.report.application.exception;

import com.gamyeon.common.response.SuccessCode;
import org.springframework.http.HttpStatus;

public enum ReportSuccessCode implements SuccessCode {
  REPORT_DETAIL_SUCCESS(HttpStatus.OK, "RPRT-S000", "success"),
  REPORT_DELETE_SUCCESS(HttpStatus.OK, "RPRT-S000", "success"),
  REPORT_CALLBACK_SUCCESS(HttpStatus.OK, "RPRT-S000", "success");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ReportSuccessCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  @Override
  public HttpStatus getStatus() {
    return status;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
