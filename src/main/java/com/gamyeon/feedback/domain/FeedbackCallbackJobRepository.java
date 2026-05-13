package com.gamyeon.feedback.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FeedbackCallbackJobRepository {

  FeedbackCallbackJob save(FeedbackCallbackJob job);

  Optional<FeedbackCallbackJob> findById(Long id);

  Optional<FeedbackCallbackJob> findByRequestId(String requestId);

  Optional<FeedbackCallbackJob> findByQuestionSetId(Long questionSetId);

  Optional<FeedbackCallbackJob> findNextProcessingCandidate(LocalDateTime now);

  Optional<FeedbackCallbackJob> findNextStaleProcessingJob(LocalDateTime staleBefore);
}
