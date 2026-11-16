package com.gamyeon.report.application.service;

import com.gamyeon.answer.domain.Answer;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlPort;
import com.gamyeon.intv.domain.Intv;
import com.gamyeon.report.application.exception.ReportAccessDeniedException;
import com.gamyeon.report.application.port.in.DeleteReportUseCase;
import com.gamyeon.report.application.port.in.GetReportDetailUseCase;
import com.gamyeon.report.application.port.out.LoadAnswerPort;
import com.gamyeon.report.application.port.out.LoadIntvPort;
import com.gamyeon.report.application.port.out.LoadReportPort;
import com.gamyeon.report.application.port.out.SaveReportPort;
import com.gamyeon.report.domain.Report;
import com.gamyeon.report.infrastructure.web.dto.ReportDetailResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportQueryService implements GetReportDetailUseCase, DeleteReportUseCase {

  private final LoadReportPort loadReportPort;
  private final SaveReportPort saveReportPort;
  private final LoadIntvPort loadIntvPort;
  private final LoadAnswerPort loadAnswerPort;
  private final StoragePresignedUrlPort storagePresignedUrlPort;

  // ── 상세 조회 ──────────────────────────────────────────────────────────────
  /*
   * 리포트가 존재하지 않거나, 타 사용자 소유인 경우 모두 403 반환
   * (존재 여부 노출 방지를 위해 두 케이스를 동일하게 처리)
   */
  @Override
  @Transactional(readOnly = true)
  public ReportDetailResponse getDetail(Long intvId, Long userId) {
    Report report = loadReportPort.findByIntvId(intvId).orElse(null);

    if (report == null || !report.getUserId().equals(userId)) {
      log.warn(
          "[Report] 권한 없는 접근 시도 - intvId={}, requestUserId={}, ownerUserId={}",
          intvId,
          userId,
          report != null ? report.getUserId() : null);
      throw new ReportAccessDeniedException(intvId);
    }

    Intv intv = loadIntvPort.findById(intvId).orElse(null);
    List<Answer> answers = loadAnswerPort.findByIntvIdOrderByAnswerOrder(intvId);
    return ReportDetailResponse.builder()
        .intvId(intvId)
        .intvStatus(intv != null ? intv.getStatus().name() : null)
        .reportStatus(report.getStatus().name())
        .report(
            report.getStatus().name().equals("SUCCEED") ? parseReportData(report, answers) : null)
        .build();
  }

  // ── 삭제 ──────────────────────────────────────────────────────────────────
  /*
   * 리포트가 존재하지 않거나, 타 사용자 소유인 경우 모두 403 반환
   */
  @Override
  @Transactional
  public void delete(Long intvId, Long userId) {
    Report report = loadReportPort.findByIntvId(intvId).orElse(null);

    if (report == null || !report.getUserId().equals(userId)) {
      log.warn(
          "[Report] 권한 없는 삭제 시도 - intvId={}, requestUserId={}, ownerUserId={}",
          intvId,
          userId,
          report != null ? report.getUserId() : null);
      throw new ReportAccessDeniedException(intvId);
    }

    saveReportPort.delete(report);
  }

  // ── report_data jsonb + Answer 미디어 파싱 ────────────────────────────────
  @SuppressWarnings("unchecked")
  private ReportDetailResponse.ReportDetail parseReportData(Report report, List<Answer> answers) {
    if (report.getReportData() == null) return null;

    try {
      java.util.Map<String, Object> data = (java.util.Map<String, Object>) report.getReportData();

      // questionSummaries + mediaUrl 조합
      List<ReportDetailResponse.QuestionSummary> questionSummaries = null;
      Object rawSummaries = data.get("question_summaries");
      if (rawSummaries instanceof List<?> rawList) {
        questionSummaries =
            rawList.stream()
                .filter(item -> item instanceof java.util.Map)
                .map(
                    item -> {
                      java.util.Map<String, Object> map = (java.util.Map<String, Object>) item;

                      // questionSetId 추출 (index 또는 id 필드에서)
                      Long questionSetId = extractQuestionSetId(map);
                      Answer answer =
                          answers.stream()
                              .filter(a -> a.getQuestionSetId().equals(questionSetId))
                              .findFirst()
                              .orElse(null);

                      Object rawFeedback = map.get("feedback");
                      ReportDetailResponse.QuestionFeedback feedback = null;
                      if (rawFeedback instanceof java.util.Map<?, ?> fb) {
                        java.util.Map<String, Object> fbMap = (java.util.Map<String, Object>) fb;
                        feedback =
                            ReportDetailResponse.QuestionFeedback.builder()
                                .characteristic((String) fbMap.get("characteristic"))
                                .strength((String) fbMap.get("strength"))
                                .improvement((String) fbMap.get("improvement"))
                                .build();
                      }

                      return ReportDetailResponse.QuestionSummary.builder()
                          .index((Integer) map.get("index"))
                          .question((String) map.get("question"))
                          .answerSummary((String) map.get("answer_summary"))
                          .feedbackBadges(
                              map.get("feedback_badges") instanceof List<?>
                                  ? (List<String>) map.get("feedback_badges")
                                  : new ArrayList<>())
                          .feedback(feedback)
                          .mediaUrl(
                              answer != null && answer.getFileUrl() != null
                                  ? storagePresignedUrlPort.createReadUrl(answer.getFileUrl())
                                  : null)
                          .build();
                    })
                .toList();
      }

      return ReportDetailResponse.ReportDetail.builder()
          .totalScore(toInteger(data.get("total_score")))
          .reportAccuracy((String) data.get("report_accuracy"))
          .jobCategory((String) data.get("job_category"))
          .answeredCount(toInteger(data.get("answered_count")))
          .avgAnswerDurationMs(
              data.get("avg_answer_duration_ms") != null
                  ? Long.valueOf(data.get("avg_answer_duration_ms").toString())
                  : null)
          .createdAt(
              data.get("created_at") != null
                  ? java.time.LocalDateTime.ofInstant(
                      java.time.Instant.parse((String) data.get("created_at")),
                      java.time.ZoneOffset.UTC)
                  : null)
          .competencyScores(convertCompetencyScores(data.get("competency_scores")))
          .strengths((List<String>) data.get("strengths"))
          .weaknesses((List<String>) data.get("weaknesses"))
          .questionSummaries(questionSummaries)
          .build();

    } catch (Exception e) {
      log.error("[Report] report_data 파싱 실패 - intvId={}", report.getIntvId(), e);
      return null;
    }
  }

  private Long extractQuestionSetId(java.util.Map<String, Object> map) {
    // questionSummaries의 Map에서 questionSetId 추출
    // 1순위: questionSetId 필드
    Object questionSetId = map.get("question_set_id");
    if (questionSetId instanceof Number) {
      return ((Number) questionSetId).longValue();
    }
    // 2순위: index 필드로 대체 (1, 2, 3...)
    Object index = map.get("index");
    if (index instanceof Number) {
      return ((Number) index).longValue();
    }
    return 0L; // fallback
  }

  private Integer toInteger(Object value) {
    if (value instanceof Number n) return n.intValue();
    return null;
  }

  @SuppressWarnings("unchecked")
  private java.util.Map<String, Integer> convertCompetencyScores(Object raw) {
    if (!(raw instanceof java.util.Map<?, ?> map)) return null;
    java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
    map.forEach(
        (key, value) -> {
          String camel = toCamelCase((String) key);
          result.put(camel, toInteger(value));
        });
    return result;
  }

  private String toCamelCase(String snake) {
    StringBuilder sb = new StringBuilder();
    boolean upper = false;
    for (char c : snake.toCharArray()) {
      if (c == '_') {
        upper = true;
      } else if (upper) {
        sb.append(Character.toUpperCase(c));
        upper = false;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
