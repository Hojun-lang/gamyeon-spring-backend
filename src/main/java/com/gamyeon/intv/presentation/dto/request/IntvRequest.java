package com.gamyeon.intv.presentation.dto.request;

import com.gamyeon.intv.application.dto.command.CreateIntvCommand;
import com.gamyeon.intv.application.dto.command.UpdateIntvCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IntvRequest(
    @NotBlank(message = "면접 제목은 필수입니다.")
        @Pattern(
            regexp = "^[가-힣a-zA-Z0-9 ]{1,20}$",
            message = "면접 제목은 공백을 포함한 한글, 영어, 숫자 1~20자만 가능합니다.")
        String title) {

  public CreateIntvCommand toCreateCommand(Long userId) {
    return new CreateIntvCommand(userId, title);
  }

  public UpdateIntvCommand toUpdateCommand(Long userId, Long intvId) {
    return new UpdateIntvCommand(userId, intvId, title);
  }
}
