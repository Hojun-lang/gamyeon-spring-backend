package com.gamyeon.intv.infrastructure;

import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvRepository;
import com.gamyeon.intv.domain.IntvStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IntvRepositoryImpl implements IntvRepository {

  private final JpaIntvRepository jpaIntvRepository;

  @Override
  public Intv save(Intv intv) {
    return jpaIntvRepository.save(intv);
  }

  @Override
  public Optional<Intv> findById(Long id) {
    return jpaIntvRepository.findById(id);
  }

  @Override
  public List<FinishedIntvDailyCountInfo> findFinishedIntvCountByDateAndUserId(
      Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
    return jpaIntvRepository
        .findFinishedIntvCountByDateAndUserId(
            userId, IntvStatus.FINISHED.name(), startDateTime, endDateTime)
        .stream()
        .map(JpaIntvRepository.FinishedIntvDailyCountProjection::toInfo)
        .toList();
  }

  // Report N+1 개선용 — JpaRepository 내장 findAllById() 활용 (IN 쿼리 1회)
  @Override
  public List<Intv> findAllByIds(List<Long> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return jpaIntvRepository.findAllById(ids);
  }
}
