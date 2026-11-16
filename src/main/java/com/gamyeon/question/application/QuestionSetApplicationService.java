package com.gamyeon.question.application;

import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvErrorCode;
import com.gamyeon.intv.domain.IntvException;
import com.gamyeon.question.application.port.in.CreateQuestionSetCommand;
import com.gamyeon.question.application.port.in.CreateQuestionSetUseCase;
import com.gamyeon.question.application.port.in.GetQuestionSetResult;
import com.gamyeon.question.application.port.in.GetQuestionSetUseCase;
import com.gamyeon.question.application.port.in.RequestCustomQuestionUseCase;
import com.gamyeon.question.application.port.out.GenerateCustomQuestionPort;
import com.gamyeon.question.application.port.out.LoadIntvPort;
import com.gamyeon.question.application.port.out.LoadPreparationPort;
import com.gamyeon.question.application.port.out.PreparationForQuestionGeneration;
import com.gamyeon.question.domain.CommonQuestion;
import com.gamyeon.question.domain.CommonQuestionRepository;
import com.gamyeon.question.domain.QuestionErrorCode;
import com.gamyeon.question.domain.QuestionException;
import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.question.domain.QuestionSetRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class QuestionSetApplicationService
    implements RequestCustomQuestionUseCase, CreateQuestionSetUseCase, GetQuestionSetUseCase {

  private final QuestionSetRepository questionSetRepository;
  private final CommonQuestionRepository commonQuestionRepository;
  private final LoadPreparationPort loadPreparationPort;
  private final LoadIntvPort loadIntvPort;
  private final GenerateCustomQuestionPort generateCustomQuestionPort;

  @Override
  public void generate(Long userId, Long intvId) {
    log.info("Generating question set. userId={}, intvId={}", userId, intvId);

    getOwnedIntv(userId, intvId);
    PreparationForQuestionGeneration preparation = loadPreparationPort.loadByIntvId(intvId);
    try {
      generateCustomQuestionPort.request(preparation);
    } catch (Exception e) {
      log.error("AI question generation request failed. intvId={}", intvId, e);
      create(new CreateQuestionSetCommand(intvId, "FAIL", List.of(), "AI server unavailable"));
    }
  }

  @Override
  public void create(CreateQuestionSetCommand command) {
    log.info(
        "Creating question set. intvId={}, status={}, customQuestionCount={}",
        command.intvId(),
        command.status(),
        command.questions() == null ? 0 : command.questions().size());

    loadIntvPort
        .loadById(command.intvId())
        .orElseThrow(() -> new IntvException(IntvErrorCode.INTV_NOT_FOUND));

    if (questionSetRepository.existsByIntvId(command.intvId())) {
      throw new QuestionException(QuestionErrorCode.ALREADY_EXIST);
    }

    // 맞춤 질문 0~4개 추출('FAIL' 이면 0으로 침)
    List<String> customQuestions;
    if (!Objects.equals(command.status(), "SUCCESS")) {
      customQuestions = List.of();
    } else {
      customQuestions = command.questions() == null ? List.of() : command.questions();
    }

    customQuestions = customQuestions.subList(0, Math.min(customQuestions.size(), 4));

    int commonQuestionCount = 7 - customQuestions.size(); // 가져올 공통 질문 개수 = 7 - 맞춤질문 개수

    List<String> commonQuestions =
        commonQuestionRepository.findRandomActiveQuestions(commonQuestionCount).stream()
            .map(CommonQuestion::getContent)
            .toList();

    List<String> finalQuestions = new ArrayList<>();
    finalQuestions.addAll(commonQuestions);
    finalQuestions.addAll(customQuestions);

    List<QuestionSet> questionSets =
        IntStream.range(0, finalQuestions.size())
            .mapToObj(
                index -> QuestionSet.create(command.intvId(), finalQuestions.get(index), index + 1))
            .toList();

    questionSetRepository.saveAll(questionSets);
    log.info(
        "Question set created. intvId={}, totalQuestionCount={}",
        command.intvId(),
        questionSets.size());
  }

  @Override
  public GetQuestionSetResult getQuestionSets(Long userId, Long intvId) {
    log.info("Loading question sets. userId={}, intvId={}", userId, intvId);

    Intv intv = getOwnedIntv(userId, intvId);

    List<QuestionSet> questionSets = questionSetRepository.getAllByIntvId(intvId);

    return GetQuestionSetResult.of(intv.getId(), questionSets);
  }

  private Intv getOwnedIntv(Long userId, Long intvId) {
    Intv intv =
        loadIntvPort
            .loadById(intvId)
            .orElseThrow(() -> new IntvException(IntvErrorCode.INTV_NOT_FOUND));

    if (!intv.getUserId().equals(userId)) {
      throw new IntvException(IntvErrorCode.INTV_FORBIDDEN);
    }

    return intv;
  }
}
