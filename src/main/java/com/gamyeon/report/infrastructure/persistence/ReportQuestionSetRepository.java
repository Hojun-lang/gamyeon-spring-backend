package com.gamyeon.report.infrastructure.persistence;

import com.gamyeon.question.domain.QuestionSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 리포트 모듈에서 오직 조회(Read) 목적으로만 사용하는 전용 리포지토리입니다. question.domain의 코드를 수정하지 않고 리포트 모듈의 요구사항을 충족합니다.
 */
public interface ReportQuestionSetRepository extends JpaRepository<QuestionSet, Long> {

  // 리포트 생성에 꼭 필요한 '면접 ID별 질문 리스트' 조회 메서드
  List<QuestionSet> findAllByIntvId(Long intvId);

  List<QuestionSet> findAllByIntvIdOrderByQuestionOrderAsc(Long intvId);
}
