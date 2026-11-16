package com.gamyeon.question.application.port.out;

public interface LoadPreparationPort {

  PreparationForQuestionGeneration loadByIntvId(Long intvId);
}
