package com.gamyeon.report.application.exception;

// ReportNotFoundException의 부모 클래스와 동일하게 맞춰주세요 (예: CustomException, BusinessException 등)
public class ReportAccessDeniedException extends RuntimeException {

  private final Long intvId;

  public ReportAccessDeniedException(Long intvId) {
    super(ReportErrorCode.REPORT_ACCESS_DENIED.getMessage());
    this.intvId = intvId;
  }

  public Long getIntvId() {
    return intvId;
  }

  public ReportErrorCode getErrorCode() {
    return ReportErrorCode.REPORT_ACCESS_DENIED;
  }
}
