package com.gamyeon.answer.domain;

import com.gamyeon.common.response.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AnswerErrorCode implements ErrorCode {
  ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "ANS-N001", "해당 답변을 찾을 수 없습니다."),
  QUESTION_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "QSTN-N001", "해당 질문 세트를 찾을 수 없습니다."),
  ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "ANS-C001", "해당 질문에는 이미 답변 영상이 등록되어 있습니다."),
  INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "ANS-B001", "지원하지 않는 영상 파일 형식입니다."),
  INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "ANS-B002", "contentType은 video/mp4 여야 합니다."),
  INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "ANS-B003", "파일 크기는 1바이트 이상이어야 합니다."),
  INTV_QUESTION_MISMATCH(HttpStatus.BAD_REQUEST, "ANS-B004", "면접과 질문이 일치하지 않습니다."),
  ANSWER_OUT_OF_ORDER(HttpStatus.BAD_REQUEST, "ANS-B005", "현재 답변해야 하는 질문이 아닙니다."),
  ANALYSIS_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "ANS-C002", "해당 답변은 이미 STT 분석 중입니다."),
  ANALYSIS_ALREADY_COMPLETED(HttpStatus.CONFLICT, "ANS-C003", "해당 답변은 이미 STT 분석이 완료되었습니다."),
  ANSWER_ANALYSIS_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "ANS-N002", "STT 분석 요청에 실패했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  AnswerErrorCode(HttpStatus status, String code, String message) {
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
