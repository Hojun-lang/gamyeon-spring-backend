package com.gamyeon.report.application.port.out;

import com.gamyeon.intv.domain.Intv;
import java.util.List;
import java.util.Optional;

public interface LoadIntvPort {
  Optional<Intv> findById(Long intvId);

  // N+1 개선용 — IN 쿼리 일괄 조회
  List<Intv> findAllByIds(List<Long> intvIds);
}
