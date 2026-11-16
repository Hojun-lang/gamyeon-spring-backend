package com.gamyeon.report.application.port.in;

import com.gamyeon.report.infrastructure.web.dto.ReportDetailResponse;

public interface GetReportDetailUseCase {

  /** 리포트 상세 조회 - REPORTS.report_data(jsonb) 파싱 후 반환 - 리포트 없음 → RPRT-N001 */
  /*
   * 타 사용자의 리포트 조회 시도시에도 리포트 없음으로 반환
   */
  ReportDetailResponse getDetail(Long intvId, Long userId);
}
