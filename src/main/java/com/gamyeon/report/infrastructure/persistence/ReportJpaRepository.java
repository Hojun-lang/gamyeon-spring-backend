package com.gamyeon.report.infrastructure.persistence;

import com.gamyeon.report.domain.Report;
import com.gamyeon.report.domain.ReportStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportJpaRepository extends JpaRepository<Report, Long> {

  // 일반 조회 — 상세/목록/삭제
  Optional<Report> findByIntvId(Long intvId);

  boolean existsByIntvId(Long intvId);

  // Pessimistic Write Lock — tryTriggerReportGeneration 전용
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM Report r WHERE r.intvId = :intvId")
  Optional<Report> findByIntvIdWithLock(@Param("intvId") Long intvId);

  // 스케줄러 — IN_PROGRESS & created_at 기준 5분 경과 건 탐색
  @Query("SELECT r FROM Report r WHERE r.status = :status AND r.createdAt < :threshold")
  List<Report> findAllByStatusAndCreatedAtBefore(
      @Param("status") ReportStatus status, @Param("threshold") LocalDateTime threshold);

  List<Report> findAllByIntvIdIn(List<Long> intvIds);
}
