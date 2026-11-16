package com.gamyeon.intv.application;

import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerRepository;
import com.gamyeon.intv.application.dto.command.ChangeStateIntvCommand;
import com.gamyeon.intv.application.dto.command.CreateIntvCommand;
import com.gamyeon.intv.application.dto.command.UpdateIntvCommand;
import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import com.gamyeon.intv.application.dto.result.IntvInfo;
import com.gamyeon.intv.application.dto.result.IntvListInfo;
import com.gamyeon.intv.application.dto.result.IntvListItemInfo;
import com.gamyeon.intv.application.dto.result.QuestionProgressInfo;
import com.gamyeon.intv.application.dto.result.ResumeContextInfo;
import com.gamyeon.intv.application.usecase.ChangeStateUseCase;
import com.gamyeon.intv.application.usecase.CreateUseCase;
import com.gamyeon.intv.application.usecase.GetFinishedIntvStatsUseCase;
import com.gamyeon.intv.application.usecase.GetIntvListUseCase;
import com.gamyeon.intv.application.usecase.GetResumeContextUseCase;
import com.gamyeon.intv.application.usecase.UpdateTitleUseCase;
import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvErrorCode;
import com.gamyeon.intv.domain.IntvException;
import com.gamyeon.intv.domain.IntvRepository;
import com.gamyeon.intv.domain.IntvStatus;
import com.gamyeon.intv.domain.event.InterviewFinishedEvent;
import com.gamyeon.preparation.application.port.in.PreparationUseCase;
import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.question.domain.QuestionSetRepository;
import com.gamyeon.report.application.port.in.ReportSummaryQueryUseCase;
import com.gamyeon.report.application.port.in.ReportSummaryResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IntvApplicationService
    implements CreateUseCase,
        ChangeStateUseCase,
        UpdateTitleUseCase,
        GetFinishedIntvStatsUseCase,
        GetIntvListUseCase,
        GetResumeContextUseCase {

  private final IntvRepository intvRepository;
  private final QuestionSetRepository questionSetRepository;
  private final AnswerRepository answerRepository;
  private final ReportSummaryQueryUseCase reportSummaryQueryUseCase;
  private final PreparationUseCase preparationUseCase;
  private final ApplicationEventPublisher eventPublisher;

  private static final int TITLE_MAX_LENGTH = 255;

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

  public IntvApplicationService(
      IntvRepository intvRepository,
      QuestionSetRepository questionSetRepository,
      AnswerRepository answerRepository,
      ReportSummaryQueryUseCase reportSummaryQueryUseCase,
      PreparationUseCase preparationUseCase,
      ApplicationEventPublisher eventPublisher) {
    this.intvRepository = intvRepository;
    this.questionSetRepository = questionSetRepository;
    this.answerRepository = answerRepository;
    this.reportSummaryQueryUseCase = reportSummaryQueryUseCase;
    this.preparationUseCase = preparationUseCase;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public IntvInfo create(CreateIntvCommand command) {
    Intv intv = Intv.create(command.userId(), command.title());
    Intv savedIntv = intvRepository.save(intv);

    preparationUseCase.create(savedIntv.getId());
    return IntvInfo.from(savedIntv);
  }

  @Override
  public IntvInfo updateTitle(UpdateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.updateTitle(command.title());
    return IntvInfo.from(intv);
  }

  @Override
  public void start(ChangeStateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.start();
  }

  @Override
  public void pause(ChangeStateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.pause();
  }

  @Override
  public void resume(ChangeStateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.resume();
  }

  @Override
  public void finish(ChangeStateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.finish();

    eventPublisher.publishEvent(new InterviewFinishedEvent(intv.getId(), intv.getUserId()));

    // 실시간 알림 이벤트 발행
    eventPublisher.publishEvent(
        new com.gamyeon.notif.application.port.in.event.NotifPublishEvent(
            intv.getUserId(),
            com.gamyeon.notif.domain.NotifType.REPORT_PROCESSING,
            buildNotifTitle("분석중", intv.getTitle()),
            "면접 분석이 시작되었어요.",
            intv.getId()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<FinishedIntvDailyCountInfo> getFinishedIntvStats(
      Long userId, LocalDate startDate, LocalDate endDate) {
    validatePeriod(startDate, endDate);

    List<FinishedIntvDailyCountInfo> counts =
        intvRepository.findFinishedIntvCountByDateAndUserId(
            userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

    return fillEmptyDates(startDate, endDate, counts);
  }

  @Override
  @Transactional(readOnly = true)
  public IntvListInfo getIntvs(Long userId, List<IntvStatus> statuses, Pageable pageable) {
    Page<Intv> intvPage = intvRepository.findAllByUserIdAndStatuses(userId, statuses, pageable);
    List<Long> finishedIntvIds =
        intvPage.getContent().stream()
            .filter(intv -> intv.getStatus() == IntvStatus.FINISHED)
            .map(Intv::getId)
            .toList();
    Map<Long, ReportSummaryResult> reportByIntvId =
        reportSummaryQueryUseCase.findAllByIntvIds(finishedIntvIds).stream()
            .collect(Collectors.toMap(ReportSummaryResult::intvId, Function.identity()));
    List<IntvListItemInfo> content =
        intvPage.getContent().stream()
            .map(intv -> IntvListItemInfo.from(intv, reportByIntvId.get(intv.getId())))
            .toList();

    return new IntvListInfo(
        content,
        intvPage.getNumber(),
        intvPage.getSize(),
        intvPage.getTotalElements(),
        intvPage.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public ResumeContextInfo getResumeContext(Long userId, Long intvId) {
    Intv intv = getOwnedIntv(userId, intvId);
    List<QuestionSet> questions = questionSetRepository.getAllByIntvId(intv.getId());
    Map<Long, Answer> answersByQuestionSetId =
        answerRepository.findAllByIntvId(intv.getId()).stream()
            .collect(
                Collectors.toMap(
                    Answer::getQuestionSetId, Function.identity(), (left, right) -> left));

    List<QuestionProgressInfo> questionProgresses =
        questions.stream()
            .map(
                question ->
                    QuestionProgressInfo.of(question, answersByQuestionSetId.get(question.getId())))
            .toList();

    int answeredCount =
        (int) questionProgresses.stream().filter(QuestionProgressInfo::answered).count();
    QuestionProgressInfo nextQuestion =
        questionProgresses.stream()
            .filter(question -> !question.answered())
            .findFirst()
            .orElse(null);

    return new ResumeContextInfo(
        intv.getId(),
        intv.getStatus(),
        questions.size(),
        answeredCount,
        nextQuestion == null && !questions.isEmpty(),
        nextQuestion == null ? null : nextQuestion.questionSetId(),
        nextQuestion == null ? null : nextQuestion.questionOrder(),
        nextQuestion == null ? null : nextQuestion.content(),
        questionProgresses);
  }

  private Intv getOwnedIntv(Long userId, Long intvId) {
    Intv intv =
        intvRepository
            .findById(intvId)
            .orElseThrow(() -> new IntvException(IntvErrorCode.INTV_NOT_FOUND));

    if (!intv.getUserId().equals(userId)) {
      throw new IntvException(IntvErrorCode.INTV_FORBIDDEN);
    }

    return intv;
  }

  private void validatePeriod(LocalDate startDate, LocalDate endDate) {
    if (startDate.isAfter(endDate)) {
      throw new IntvException(IntvErrorCode.INVALID_PERIOD);
    }
  }

  private List<FinishedIntvDailyCountInfo> fillEmptyDates(
      LocalDate startDate, LocalDate endDate, List<FinishedIntvDailyCountInfo> counts) {
    List<FinishedIntvDailyCountInfo> results = new ArrayList<>();
    int countIndex = 0;

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (countIndex < counts.size() && counts.get(countIndex).date().isEqual(date)) {
        results.add(counts.get(countIndex));
        countIndex++;
      } else {
        results.add(new FinishedIntvDailyCountInfo(date, 0L));
      }
    }

    return results;
  }
}
