package com.gamyeon.answer.adapter.out.external;

import com.gamyeon.answer.application.port.out.AnswerAnalysisTarget;
import com.gamyeon.answer.application.port.out.RequestAnswerSttAnalysisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignAnswerAnalysisAdapter implements RequestAnswerSttAnalysisPort {

  private final PythonAnswerAnalysisFeignClient pythonAnswerAnalysisFeignClient;

  @Override
  public void request(AnswerAnalysisTarget target) {
    log.info(
        "Calling Python STT analysis API. requestId={}, answerId={}, intvId={}, questionSetId={}, mediaFileKey={}",
        target.requestId(),
        target.answerId(),
        target.intvId(),
        target.questionSetId(),
        target.mediaFileKey());
    pythonAnswerAnalysisFeignClient.requestAnalysis(PythonAnswerAnalysisRequest.from(target));
  }
}
