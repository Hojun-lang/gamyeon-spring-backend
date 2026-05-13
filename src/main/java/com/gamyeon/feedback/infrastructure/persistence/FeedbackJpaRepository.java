package com.gamyeon.feedback.infrastructure.persistence;

import com.gamyeon.feedback.domain.FeedbackStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackJpaRepository extends JpaRepository<FeedbackEntity, Long> {

  boolean existsByQuestionSetIdAndStatusIn(Long questionSetId, List<FeedbackStatus> statuses);

  boolean existsByQuestionSetId(Long questionSetId);

  Optional<FeedbackEntity> findByQuestionSetId(Long questionSetId);

  // [추가] 해당 면접의 특정 상태(SUCCEED) 피드백 개수 조회
  int countByIntvIdAndStatus(Long intvId, FeedbackStatus status);

  // [추가] AI 요청을 위한 SUCCEED 피드백 리스트 조회
  List<FeedbackEntity> findAllByIntvIdAndStatus(Long intvId, FeedbackStatus status);
}
