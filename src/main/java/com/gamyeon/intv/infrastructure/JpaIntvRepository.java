package com.gamyeon.intv.infrastructure;

import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import com.gamyeon.intv.domain.Intv;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaIntvRepository extends JpaRepository<Intv, Long> {

  Page<Intv> findAllByUserId(Long userId, Pageable pageable);

  Page<Intv> findAllByUserIdAndStatusIn(
      Long userId, List<com.gamyeon.intv.domain.IntvStatus> statuses, Pageable pageable);

  @Query(
      value =
          """
      SELECT CAST(i.finished_at AS DATE) AS date, COUNT(*) AS count
      FROM intvs i
      WHERE i.user_id = :userId
        AND i.status = :status
        AND i.finished_at >= :startDateTime
        AND i.finished_at < :endDateTime
      GROUP BY CAST(i.finished_at AS DATE)
      ORDER BY CAST(i.finished_at AS DATE)
      """,
      nativeQuery = true)
  List<FinishedIntvDailyCountProjection> findFinishedIntvCountByDateAndUserId(
      @Param("userId") Long userId,
      @Param("status") String status,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime);

  interface FinishedIntvDailyCountProjection {
    LocalDate getDate();

    long getCount();

    default FinishedIntvDailyCountInfo toInfo() {
      return new FinishedIntvDailyCountInfo(getDate(), getCount());
    }
  }
}
