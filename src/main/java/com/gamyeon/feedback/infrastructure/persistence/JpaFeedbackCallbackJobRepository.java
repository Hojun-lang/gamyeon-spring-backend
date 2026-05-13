package com.gamyeon.feedback.infrastructure.persistence;

import com.gamyeon.feedback.domain.FeedbackCallbackJob;
import com.gamyeon.feedback.domain.FeedbackCallbackJobStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaFeedbackCallbackJobRepository extends JpaRepository<FeedbackCallbackJob, Long> {

  Optional<FeedbackCallbackJob> findByRequestId(String requestId);

  Optional<FeedbackCallbackJob> findByQuestionSetId(Long questionSetId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select job
      from FeedbackCallbackJob job
      where job.status = :receivedStatus
         or (job.status = :retryWaitingStatus and job.nextRetryAt <= :now)
      order by job.createdAt asc
      """)
  List<FeedbackCallbackJob> findProcessingCandidates(
      @Param("receivedStatus") FeedbackCallbackJobStatus receivedStatus,
      @Param("retryWaitingStatus") FeedbackCallbackJobStatus retryWaitingStatus,
      @Param("now") LocalDateTime now,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select job
      from FeedbackCallbackJob job
      where job.status = :processingStatus
        and job.lastAttemptAt <= :staleBefore
      order by job.lastAttemptAt asc
      """)
  List<FeedbackCallbackJob> findStaleProcessingJobs(
      @Param("processingStatus") FeedbackCallbackJobStatus processingStatus,
      @Param("staleBefore") LocalDateTime staleBefore,
      Pageable pageable);
}
