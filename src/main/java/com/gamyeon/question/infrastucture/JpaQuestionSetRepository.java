package com.gamyeon.question.infrastucture;

import com.gamyeon.question.domain.QuestionSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaQuestionSetRepository extends JpaRepository<QuestionSet, Long> {

  List<QuestionSet> findAllByIntvId(Long intvId);

  List<QuestionSet> findAllByIntvIdOrderByQuestionOrderAsc(Long intvId);

  boolean existsByIntvId(Long intvId);
}
