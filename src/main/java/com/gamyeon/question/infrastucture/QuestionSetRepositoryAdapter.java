package com.gamyeon.question.infrastucture;

import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.question.domain.QuestionSetRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionSetRepositoryAdapter implements QuestionSetRepository {

  private final JpaQuestionSetRepository jpaQuestionSetRepository;

  public QuestionSetRepositoryAdapter(JpaQuestionSetRepository jpaQuestionSetRepository) {
    this.jpaQuestionSetRepository = jpaQuestionSetRepository;
  }

  @Override
  public void saveAll(List<QuestionSet> questionSets) {
    jpaQuestionSetRepository.saveAll(questionSets);
  }

  @Override
  public List<QuestionSet> getAllByIntvId(Long intvId) {
    return jpaQuestionSetRepository.findAllByIntvIdOrderByQuestionOrderAsc(intvId);
  }

  @Override
  public boolean existsByIntvId(Long intvId) {
    return jpaQuestionSetRepository.existsByIntvId(intvId);
  }
}
