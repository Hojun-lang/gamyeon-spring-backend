package com.gamyeon.answer.adapter.out.persistence;

import com.gamyeon.answer.domain.AnswerAnalysisJob;
import com.gamyeon.answer.domain.AnswerAnalysisJobRepository;
import com.gamyeon.answer.domain.AnswerAnalysisJobStatus;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AnswerAnalysisJobRepositoryAdapter implements AnswerAnalysisJobRepository {

  private static final EnumSet<AnswerAnalysisJobStatus> ACTIVE_STATUSES =
      EnumSet.of(
          AnswerAnalysisJobStatus.QUEUED,
          AnswerAnalysisJobStatus.SENDING,
          AnswerAnalysisJobStatus.SENT,
          AnswerAnalysisJobStatus.RETRY_WAITING);

  private final JpaAnswerAnalysisJobRepository jpaAnswerAnalysisJobRepository;

  @Override
  public AnswerAnalysisJob save(AnswerAnalysisJob job) {
    return jpaAnswerAnalysisJobRepository.save(job);
  }

  @Override
  public Optional<AnswerAnalysisJob> findById(Long jobId) {
    return jpaAnswerAnalysisJobRepository.findById(jobId);
  }

  @Override
  public Optional<AnswerAnalysisJob> findByRequestId(String requestId) {
    return jpaAnswerAnalysisJobRepository.findByRequestId(requestId);
  }

  @Override
  public Optional<AnswerAnalysisJob> findLatestByAnswerId(Long answerId) {
    return jpaAnswerAnalysisJobRepository.findTopByAnswerIdOrderByCreatedAtDesc(answerId);
  }

  @Override
  public Optional<AnswerAnalysisJob> findNextDispatchCandidate(LocalDateTime now) {
    return jpaAnswerAnalysisJobRepository
        .findDispatchCandidates(
            AnswerAnalysisJobStatus.QUEUED,
            AnswerAnalysisJobStatus.RETRY_WAITING,
            now,
            PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  @Override
  public Optional<AnswerAnalysisJob> findNextStaleSendingJob(LocalDateTime staleBefore) {
    return jpaAnswerAnalysisJobRepository
        .findStaleSendingJobs(AnswerAnalysisJobStatus.SENDING, staleBefore, PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  @Override
  public boolean existsActiveJobByAnswerId(Long answerId) {
    return jpaAnswerAnalysisJobRepository.existsByAnswerIdAndStatusIn(answerId, ACTIVE_STATUSES);
  }
}
