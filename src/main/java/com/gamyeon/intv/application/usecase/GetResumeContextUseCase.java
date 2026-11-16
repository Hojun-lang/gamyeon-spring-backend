package com.gamyeon.intv.application.usecase;

import com.gamyeon.intv.application.dto.result.ResumeContextInfo;

public interface GetResumeContextUseCase {

  ResumeContextInfo getResumeContext(Long userId, Long intvId);
}
