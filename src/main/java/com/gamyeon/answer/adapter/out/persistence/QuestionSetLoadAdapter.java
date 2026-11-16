package com.gamyeon.answer.adapter.out.persistence;

import com.gamyeon.answer.application.port.out.AnswerQuestionInfo;
import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.domain.AnswerErrorCode;
import com.gamyeon.answer.domain.AnswerException;
import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.question.infrastucture.JpaQuestionSetRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("answerQuestionSetLoadAdapter")
@RequiredArgsConstructor
public class QuestionSetLoadAdapter implements LoadQuestionSetPort {

  private final JpaQuestionSetRepository jpaQuestionSetRepository;

  @Override
  public boolean existsById(Long questionSetId) {
    return jpaQuestionSetRepository.existsById(questionSetId);
  }

  @Override
  public Optional<AnswerQuestionInfo> findById(Long questionSetId) {
    return jpaQuestionSetRepository.findById(questionSetId).map(this::toInfo);
  }

  @Override
  public List<AnswerQuestionInfo> findAllByIntvIdOrderByQuestionOrderAsc(Long intvId) {
    return jpaQuestionSetRepository.findAllByIntvIdOrderByQuestionOrderAsc(intvId).stream()
        .map(this::toInfo)
        .toList();
  }

  @Override
  public String getQuestionContent(Long questionSetId) {

    QuestionSet questionSet =
        jpaQuestionSetRepository
            .findById(questionSetId)
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.QUESTION_SET_NOT_FOUND));

    return questionSet.getContent();
  }

  private AnswerQuestionInfo toInfo(QuestionSet questionSet) {
    return new AnswerQuestionInfo(
        questionSet.getId(), questionSet.getIntvId(), questionSet.getQuestionOrder());
  }
}
