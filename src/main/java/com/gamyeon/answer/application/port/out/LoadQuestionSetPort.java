package com.gamyeon.answer.application.port.out;

import java.util.List;
import java.util.Optional;

public interface LoadQuestionSetPort {

  boolean existsById(Long questionSetId);

  Optional<AnswerQuestionInfo> findById(Long questionSetId);

  List<AnswerQuestionInfo> findAllByIntvIdOrderByQuestionOrderAsc(Long intvId);

  String getQuestionContent(Long questionSetId);
}
