package com.gamyeon.report.infrastructure.external;

import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.report.application.port.out.LoadQuestionSetPort;
import com.gamyeon.report.infrastructure.persistence.ReportQuestionSetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("reportQuestionSetLoadAdapter")
@RequiredArgsConstructor
public class QuestionSetLoadAdapter implements LoadQuestionSetPort {

  private final ReportQuestionSetRepository reportQuestionSetRepository;

  @Override
  public List<QuestionSet> findAllByIntvId(Long intvId) {
    return reportQuestionSetRepository.findAllByIntvIdOrderByQuestionOrderAsc(intvId);
  }
}
