package com.gamyeon.report.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gamyeon.report.application.port.in.ReportSummaryResult;
import com.gamyeon.report.application.port.out.LoadReportPort;
import com.gamyeon.report.domain.Report;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportSummaryQueryServiceTest {

  @Mock private LoadReportPort loadReportPort;

  @Test
  void shouldReturnEmptyWithoutLoadingReportsWhenIntvIdsAreEmpty() {
    ReportSummaryQueryService service = new ReportSummaryQueryService(loadReportPort);

    List<ReportSummaryResult> result = service.findAllByIntvIds(List.of());

    assertEquals(List.of(), result);
    verify(loadReportPort, never()).findAllByIntvIds(List.of());
  }

  @Test
  void shouldConvertReportsToSummaryResults() {
    ReportSummaryQueryService service = new ReportSummaryQueryService(loadReportPort);
    Report report = Report.createInProgress(10L, 99L);
    ReflectionTestUtils.setField(report, "id", 100L);
    given(loadReportPort.findAllByIntvIds(List.of(10L))).willReturn(List.of(report));

    List<ReportSummaryResult> result = service.findAllByIntvIds(List.of(10L));

    assertEquals(1, result.size());
    assertEquals(10L, result.get(0).intvId());
    assertEquals(100L, result.get(0).reportId());
    assertEquals("IN_PROGRESS", result.get(0).reportStatus());
  }
}
