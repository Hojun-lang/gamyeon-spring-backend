package com.gamyeon.report.infrastructure.external;

import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvRepository;
import com.gamyeon.report.application.port.out.LoadIntvPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntvLoadAdapter implements LoadIntvPort {

  private final IntvRepository intvRepository;

  @Override
  public Optional<Intv> findById(Long intvId) {
    return intvRepository.findById(intvId);
  }

  // N+1 개선용 — JpaRepository 내장 findAllById() 활용 (IN 쿼리 1회 실행)
  @Override
  public List<Intv> findAllByIds(List<Long> intvIds) {
    if (intvIds.isEmpty()) {
      return List.of();
    }
    return intvRepository.findAllByIds(intvIds);
  }
}
