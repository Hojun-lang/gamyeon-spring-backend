package com.gamyeon.intv.application.usecase;

import com.gamyeon.intv.application.dto.result.IntvListInfo;
import com.gamyeon.intv.domain.IntvStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface GetIntvListUseCase {

  IntvListInfo getIntvs(Long userId, List<IntvStatus> statuses, Pageable pageable);
}
