package com.gamyeon.intv.domain;

import com.gamyeon.common.response.SuccessCode;
import org.springframework.http.HttpStatus;

public enum IntvSuccessCode implements SuccessCode {
  INTV_CREATED(HttpStatus.CREATED, "INTV-S001", "면접을 생성했습니다."),
  INTV_STARTED(HttpStatus.OK, "INTV-S002", "면접을 시작했습니다."),
  INTV_PAUSED(HttpStatus.OK, "INTV-S003", "면접을 중단했습니다."),
  INTV_RESUMED(HttpStatus.OK, "INTV-S004", "면접을 재개했습니다."),
  INTV_FINISHED(HttpStatus.OK, "INTV-S005", "면접을 종료했습니다."),
  INTV_UPDATED(HttpStatus.OK, "INTV-S006", "면접 제목을 수정했습니다."),
  INTV_FINISHED_STATS_FETCHED(HttpStatus.OK, "INTV-S007", "완료된 면접 통계를 조회했습니다."),
  INTV_RESUME_CONTEXT_FETCHED(HttpStatus.OK, "INTV-S008", "면접 재개 정보를 조회했습니다."),
  INTV_LIST_FETCHED(HttpStatus.OK, "INTV-S009", "면접 기록을 조회했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  IntvSuccessCode(HttpStatus status, String code, String message) {
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
