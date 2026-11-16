package com.gamyeon.intv.domain;

import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IntvRepository {

  Intv save(Intv intv);

  Optional<Intv> findById(Long id);

  // Report BC N+1 개선용 추가
  List<Intv> findAllByIds(List<Long> ids);

  Page<Intv> findAllByUserIdAndStatuses(Long userId, List<IntvStatus> statuses, Pageable pageable);

  List<FinishedIntvDailyCountInfo> findFinishedIntvCountByDateAndUserId(
      Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
