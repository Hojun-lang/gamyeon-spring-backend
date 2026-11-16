package com.gamyeon.report.application.port.in;

import java.util.List;

public interface ReportSummaryQueryUseCase {

  List<ReportSummaryResult> findAllByIntvIds(List<Long> intvIds);
}
