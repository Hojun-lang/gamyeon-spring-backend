package com.gamyeon.answer.adapter.out.persistence;

import com.gamyeon.answer.domain.AnswerAnalysisJob;
import com.gamyeon.answer.domain.AnswerAnalysisJobStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAnswerAnalysisJobRepository extends JpaRepository<AnswerAnalysisJob, Long> {

  Optional<AnswerAnalysisJob> findByRequestId(String requestId);

  Optional<AnswerAnalysisJob> findTopByAnswerIdOrderByCreatedAtDesc(Long answerId);

  @Query(
      "select count(job) > 0 from AnswerAnalysisJob job where job.answerId = :answerId and job.status in :statuses")
  boolean existsByAnswerIdAndStatusIn(
      @Param("answerId") Long answerId,
      @Param("statuses") Collection<AnswerAnalysisJobStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select job
      from AnswerAnalysisJob job
      where job.status = :queuedStatus
         or (job.status = :retryWaitingStatus and job.nextRetryAt <= :now)
      order by job.createdAt asc
      """)
  List<AnswerAnalysisJob> findDispatchCandidates(
      @Param("queuedStatus") AnswerAnalysisJobStatus queuedStatus,
      @Param("retryWaitingStatus") AnswerAnalysisJobStatus retryWaitingStatus,
      @Param("now") LocalDateTime now,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select job
      from AnswerAnalysisJob job
      where job.status = :sendingStatus
        and job.lastAttemptAt <= :staleBefore
      order by job.lastAttemptAt asc
      """)
  List<AnswerAnalysisJob> findStaleSendingJobs(
      @Param("sendingStatus") AnswerAnalysisJobStatus sendingStatus,
      @Param("staleBefore") LocalDateTime staleBefore,
      Pageable pageable);
}
