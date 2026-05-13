package com.gamyeon.feedback.infrastructure.persistence;

import com.gamyeon.feedback.application.port.out.LoadQuestionSetPort;
import com.gamyeon.question.domain.QuestionSet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionSetRepository
    extends JpaRepository<QuestionSet, Long>, LoadQuestionSetPort {
  // 조회 전용 쿼리이므로 question 모듈에 영향을 주지 않음

  @Query("SELECT qs.intvId FROM QuestionSet qs WHERE qs.id = :id")
  @Override
  Optional<Long> findIntvIdById(@Param("id") Long id);
}
