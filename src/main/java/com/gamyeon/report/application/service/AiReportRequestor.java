package com.gamyeon.report.application.service;

import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.report.application.exception.ReportGenerationFailedException;
import com.gamyeon.report.application.port.out.LoadFeedbackPort;
import com.gamyeon.report.application.port.out.LoadQuestionSetPort;
import com.gamyeon.report.application.port.out.SaveReportPort;
import com.gamyeon.report.domain.Report;
import com.gamyeon.report.infrastructure.external.AiReportClient;
import com.gamyeon.report.infrastructure.external.dto.AiReportFeedbackItem;
import com.gamyeon.report.infrastructure.external.dto.AiReportRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiReportRequestor {

  private final LoadFeedbackPort loadFeedbackPort;
  private final LoadQuestionSetPort loadQuestionSetPort;
  private final SaveReportPort saveReportPort;
  private final AiReportClient aiReportClient;

  // After
  @Value("${server.callback-base-url}")
  private String callbackBaseUrl;

  // ── AI 요청 진입점 (트랜잭션 없음 — 상위에서 Lock 커밋 후 호출) ──────────────

  @CircuitBreaker(name = "aiReportClient", fallbackMethod = "handleAiFailure")
  public void requestToAi(Report report) {
    Long intvId = report.getIntvId();

    // 1. 질문 목록 ID 순 정렬 로드
    List<QuestionSet> questions =
        loadQuestionSetPort.findAllByIntvId(intvId).stream()
            .sorted(Comparator.comparing(QuestionSet::getId))
            .toList();

    // 2. SUCCEED 피드백 수집
    List<AiReportFeedbackItem> feedbacks = loadFeedbackPort.findSucceedFeedbacksByIntvId(intvId);

    // 3. index + questionContent 주입
    List<AiReportFeedbackItem> enrichedFeedbacks =
        feedbacks.stream()
            .map(
                f -> {
                  QuestionSet q =
                      questions.stream()
                          .filter(qs -> qs.getId().equals(f.getQuestionSetId()))
                          .findFirst()
                          .orElseThrow(
                              () ->
                                  new IllegalStateException(
                                      "QuestionSet 매핑 실패 - questionSetId=" + f.getQuestionSetId()));
                  int index = questions.indexOf(q) + 1;
                  return f.toBuilder().index(index).questionContent(q.getContent()).build();
                })
            .toList();

    // 4. AI 요청 객체 생성 및 전송
    AiReportRequest request =
        AiReportRequest.builder()
            .intvId(intvId)
            .userId(report.getUserId())
            .callback(callbackBaseUrl + "/internal/v1/reports/callback")
            .feedbacks(enrichedFeedbacks)
            .build();

    aiReportClient.requestGenerate(request);
    log.info("[Report] AI 리포트 생성 요청 완료 - intvId={}", intvId);
  }

  // ── Fallback — 예외 타입별 Case 1 / 2 / 3 분기 ──────────────────────────

  public void handleAiFailure(Report report, Throwable t) {

    // Case 2: 서킷 OPEN — IN_PROGRESS 유지, 스케줄러 가드레일이 재처리
    if (t instanceof CallNotPermittedException) {
      log.warn(
          "[Report][CircuitBreaker] 서킷 OPEN — 요청 차단됨, 스케줄러 재처리 대기 - intvId={}", report.getIntvId());
      return;
    }

    // Case 3: Feign read-timeout (180s 초과) — IN_PROGRESS 유지, Python Callback 대기
    if (t instanceof java.util.concurrent.TimeoutException
        || (t.getCause() != null && t.getCause() instanceof java.net.SocketTimeoutException)) {
      log.warn(
          "[Report][CircuitBreaker] Feign 타임아웃 — IN_PROGRESS 유지, Callback 대기 - intvId={}",
          report.getIntvId());
      return;
    }

    // Case 1: 그 외 Feign 실패 (네트워크 오류, 5xx 등) — 즉시 FAILED 처리
    log.error("[Report][CircuitBreaker] AI 요청 실패 — FAILED 처리 - intvId={}", report.getIntvId(), t);
    saveFailIndependently(report);
    throw new ReportGenerationFailedException(report.getIntvId());
  }

  // ── REQUIRES_NEW: 상위 트랜잭션 롤백과 무관하게 fail() 독립 커밋 ────────────

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveFailIndependently(Report report) {
    report.fail();
    saveReportPort.save(report);
    log.info("[Report] FAILED 상태 독립 저장 완료 - intvId={}", report.getIntvId());
  }
}
