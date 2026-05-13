package com.gamyeon.feedback.infrastructure.persistence;

import com.gamyeon.feedback.application.port.out.SaveFeedbackPort;
import com.gamyeon.feedback.domain.Feedback;
import com.gamyeon.feedback.domain.FeedbackStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedbackPersistenceAdapter implements SaveFeedbackPort {

  private final FeedbackJpaRepository jpaRepository;

  public void save(Feedback domain) {
    FeedbackEntity saved = jpaRepository.save(FeedbackEntity.fromDomain(domain));
    domain.assignId(saved.getId()); // DB 생성 ID를 도메인 객체에 반영
  }

  public void update(Feedback domain) {
    FeedbackEntity entity =
        jpaRepository
            .findById(domain.getId())
            .orElseThrow(() -> new IllegalStateException("업데이트 대상 Feedback 없음: " + domain.getId()));
    FeedbackEntity updated = FeedbackEntity.fromDomain(domain);
    jpaRepository.save(updated);
  }

  @Override
  public boolean saveIfAbsent(Feedback domain) {
    if (jpaRepository.existsByQuestionSetId(domain.getQuestionSetId())) {
      return false;
    }

    try {
      FeedbackEntity saved = jpaRepository.saveAndFlush(FeedbackEntity.fromDomain(domain));
      domain.assignId(saved.getId());
      return true;
    } catch (DataIntegrityViolationException e) {
      return false;
    }
  }

  public Optional<Feedback> findByQuestionSetId(Long questionSetId) {
    return jpaRepository.findByQuestionSetId(questionSetId).map(FeedbackEntity::toDomain);
  }

  public boolean existsCompletedByQuestionSetId(Long questionSetId) {
    return jpaRepository.existsByQuestionSetIdAndStatusIn(
        questionSetId, List.of(FeedbackStatus.SUCCEED, FeedbackStatus.FAILED));
  }

  @Override
  public boolean existsByQuestionSetId(Long questionSetId) {
    return jpaRepository.existsByQuestionSetId(questionSetId);
  }
}
