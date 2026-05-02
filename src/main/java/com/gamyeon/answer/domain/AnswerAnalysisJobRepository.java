package com.gamyeon.answer.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AnswerAnalysisJobRepository {

  AnswerAnalysisJob save(AnswerAnalysisJob job);

  Optional<AnswerAnalysisJob> findById(Long jobId);

  Optional<AnswerAnalysisJob> findByRequestId(String requestId);

  Optional<AnswerAnalysisJob> findLatestByAnswerId(Long answerId);

  Optional<AnswerAnalysisJob> findNextDispatchCandidate(LocalDateTime now);

  Optional<AnswerAnalysisJob> findNextStaleSendingJob(LocalDateTime staleBefore);

  boolean existsActiveJobByAnswerId(Long answerId);
}
