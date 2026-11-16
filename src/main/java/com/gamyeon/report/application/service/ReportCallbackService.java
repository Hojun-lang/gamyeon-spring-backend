package com.gamyeon.report.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvRepository;
import com.gamyeon.report.application.port.in.ReportCallbackUseCase;
import com.gamyeon.report.application.port.out.LoadReportPort;
import com.gamyeon.report.application.port.out.SaveReportPort;
import com.gamyeon.report.domain.Report;
import com.gamyeon.report.presentation.dto.request.ReportWebhookRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCallbackService implements ReportCallbackUseCase {

  private static final int TITLE_MAX_LENGTH = 255;

  private final LoadReportPort loadReportPort;
  private final SaveReportPort saveReportPort;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final IntvRepository intvRepository;

  @Override
  @Transactional
  public void processReportCallback(ReportWebhookRequest request) {
    log.info(
        "[Report] AI Callback 수신 - intvId={}, status={}", request.getIntvId(), request.getStatus());

    // 1. 리포트 조회 (Lock 활용)
    Report report =
        loadReportPort
            .findByIntvIdWithLock(request.getIntvId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException("존재하지 않는 리포트입니다. intvId: " + request.getIntvId()));

    // 1.5 알림 제목에 사용할 면접(Intv) 제목 조회
    Intv intv =
        intvRepository
            .findById(report.getIntvId())
            .orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 면접입니다. intvId: " + report.getIntvId()));
    String intvTitle = intv.getTitle();

    // 2. 상태에 따른 처리
    if ("SUCCEED".equals(request.getStatus()) && request.getReport() != null) {
      ReportWebhookRequest.ReportDetailData detail = request.getReport();

      //  Hibernate ClassCastException 해결: DTO 객체를 Map으로 변환하여 jsonb 저장 준비
      Map<String, Object> reportDataMap = objectMapper.convertValue(detail, Map.class);

      // 3. JSONB map에서 직접 추출 → DB 컬럼과 JSONB 값을 동일 소스로 통일
      int totalScore =
          reportDataMap.get("total_score") instanceof Number n
              ? n.intValue()
              : (detail.getTotalScore() != null ? detail.getTotalScore() : 0);
      int answeredCount =
          reportDataMap.get("answered_count") instanceof Number n
              ? n.intValue()
              : (detail.getAnsweredCount() != null ? detail.getAnsweredCount() : 0);

      log.info(
          "[Report] total_score from map={}, from dto={}",
          reportDataMap.get("total_score"),
          detail.getTotalScore());

      report.complete(
          totalScore,
          answeredCount,
          detail.getStrengths(),
          detail.getWeaknesses(),
          reportDataMap // JSONB 컬럼에 Map 형태로 저장
          );

      log.info("[Report] 리포트 생성 완료 - intvId={}", request.getIntvId());

      // 리포트 분석 완료 알림 (Report 엔티티가 가진 userId 활용)
      eventPublisher.publishEvent(
          new com.gamyeon.notif.application.port.in.event.NotifPublishEvent(
              report.getUserId(),
              com.gamyeon.notif.domain.NotifType.REPORT_SUCCESS,
              buildNotifTitle("분석 리포트 도착", intvTitle),
              "면접 분석 리포트가 완성되었습니다",
              report.getIntvId()));
    } else {
      report.fail();
      log.warn(
          "[Report] 리포트 생성 실패 혹은 데이터 누락 - intvId={}, errorMessage={}",
          request.getIntvId(),
          request.getErrorMessage());

      // 리포트 분석 실패 알림
      eventPublisher.publishEvent(
          new com.gamyeon.notif.application.port.in.event.NotifPublishEvent(
              report.getUserId(),
              com.gamyeon.notif.domain.NotifType.REPORT_FAILED,
              buildNotifTitle("분석 실패", intvTitle),
              "면접 분석 실패로 리포트를 발행할 수 없습니다",
              report.getIntvId()));
    }

    log.info("[Report] intvId={}, intvTitle='{}'", report.getIntvId(), intvTitle);

    // 4. 최종 상태 저장
    saveReportPort.save(report);
  }

  /**
   * "[태그] 제목" 형식의 알림 제목을 만듭니다. 제목이 없거나 빈 값이면 "면접"으로 대체하고, DB 컬럼 길이(255자)를 넘지 않도록 자릅니다.
   *
   * @param tag 알림 유형 태그 (예: "분석중")
   * @param baseTitle 면접 제목
   */
  private String buildNotifTitle(String tag, String baseTitle) {
    String base = (baseTitle != null && !baseTitle.isBlank()) ? baseTitle : "면접";
    String combined = "[" + tag + "] " + base;
    return combined.length() > TITLE_MAX_LENGTH
        ? combined.substring(0, TITLE_MAX_LENGTH)
        : combined;
  }
}
