package com.gamyeon.feedback.infrastructure.persistence;

import com.gamyeon.feedback.domain.FeedbackCallbackJob;
import com.gamyeon.feedback.domain.FeedbackCallbackJobRepository;
import com.gamyeon.feedback.domain.FeedbackCallbackJobStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedbackCallbackJobRepositoryAdapter implements FeedbackCallbackJobRepository {

  private final JpaFeedbackCallbackJobRepository jpaFeedbackCallbackJobRepository;

  @Override
  public FeedbackCallbackJob save(FeedbackCallbackJob job) {
    return jpaFeedbackCallbackJobRepository.saveAndFlush(job);
  }

  @Override
  public Optional<FeedbackCallbackJob> findById(Long id) {
    return jpaFeedbackCallbackJobRepository.findById(id);
  }

  @Override
  public Optional<FeedbackCallbackJob> findByRequestId(String requestId) {
    if (requestId == null || requestId.isBlank()) {
      return Optional.empty();
    }
    return jpaFeedbackCallbackJobRepository.findByRequestId(requestId);
  }

  @Override
  public Optional<FeedbackCallbackJob> findByQuestionSetId(Long questionSetId) {
    return jpaFeedbackCallbackJobRepository.findByQuestionSetId(questionSetId);
  }

  @Override
  public Optional<FeedbackCallbackJob> findNextProcessingCandidate(LocalDateTime now) {
    return jpaFeedbackCallbackJobRepository
        .findProcessingCandidates(
            FeedbackCallbackJobStatus.RECEIVED,
            FeedbackCallbackJobStatus.RETRY_WAITING,
            now,
            PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  @Override
  public Optional<FeedbackCallbackJob> findNextStaleProcessingJob(LocalDateTime staleBefore) {
    return jpaFeedbackCallbackJobRepository
        .findStaleProcessingJobs(
            FeedbackCallbackJobStatus.PROCESSING, staleBefore, PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }
}
