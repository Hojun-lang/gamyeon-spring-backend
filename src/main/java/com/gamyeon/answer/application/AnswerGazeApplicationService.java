package com.gamyeon.answer.application;

import com.gamyeon.answer.application.port.in.SendAnswerGazeSegmentCommand;
import com.gamyeon.answer.application.port.in.SendAnswerGazeSegmentUseCase;
import com.gamyeon.answer.application.port.out.AnswerGazeSegmentPayload;
import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.application.port.out.SendAnswerGazeToAiPort;
import com.gamyeon.answer.domain.AnswerErrorCode;
import com.gamyeon.answer.domain.AnswerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AnswerGazeApplicationService implements SendAnswerGazeSegmentUseCase {

  private final LoadQuestionSetPort loadQuestionSetPort;
  private final SendAnswerGazeToAiPort sendAnswerGazeToAiPort;

  public AnswerGazeApplicationService(
      LoadQuestionSetPort loadQuestionSetPort, SendAnswerGazeToAiPort sendAnswerGazeToAiPort) {
    this.loadQuestionSetPort = loadQuestionSetPort;
    this.sendAnswerGazeToAiPort = sendAnswerGazeToAiPort;
  }

  @Override
  public void send(SendAnswerGazeSegmentCommand command) {
    log.info("Sending answer gaze segment. questionSetId={}", command.questionSetId());
    if (!loadQuestionSetPort.existsById(command.questionSetId())) {
      throw new AnswerException(AnswerErrorCode.QUESTION_SET_NOT_FOUND);
    }

    try {
      sendAnswerGazeToAiPort.send(
          new AnswerGazeSegmentPayload(command.questionSetId(), command.body()));
    } catch (Exception e) {
      log.warn(
          "Failed to forward gaze segment to AI server. questionSetId={}",
          command.questionSetId(),
          e);
    }
  }
}
