package com.gamyeon.user.infrastructure.web;

import com.gamyeon.common.response.SuccessResponse;
import com.gamyeon.user.application.port.inbound.NicknameUpdateCommand;
import com.gamyeon.user.application.port.inbound.UserInfo;
import com.gamyeon.user.application.port.inbound.UserUseCase;
import com.gamyeon.user.domain.UserSuccessCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

  private final UserUseCase userUseCase;

  public UserController(UserUseCase userUseCase) {
    this.userUseCase = userUseCase;
  }

  @GetMapping("/me")
  public ResponseEntity<SuccessResponse<UserResponse>> getMyInfo(
      @AuthenticationPrincipal Long userId) {
    log.info("Received getMyInfo request. userId={}", userId);

    UserInfo userInfo = userUseCase.getMyInfo(userId);
    return ResponseEntity.ok(SuccessResponse.of(UserResponse.from(userInfo)));
  }

  @PatchMapping("/me/nickname")
  public ResponseEntity<SuccessResponse<UserResponse>> updateNickname(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody NicknameUpdateRequest request) {
    log.info("Received updateNickname request. userId={}, nickname={}", userId, request.nickname());

    NicknameUpdateCommand command = NicknameUpdateCommand.of(userId, request.nickname());
    UserInfo userInfo = userUseCase.updateNickname(command);
    return ResponseEntity.ok(
        SuccessResponse.of(UserSuccessCode.USER_NICKNAME_UPDATED, UserResponse.from(userInfo)));
  }

  @DeleteMapping("/me")
  public ResponseEntity<SuccessResponse<Void>> withdraw(@AuthenticationPrincipal Long userId) {
    log.info("Received withdraw request. userId={}", userId);

    userUseCase.withdraw(userId);
    return ResponseEntity.ok(SuccessResponse.of(UserSuccessCode.USER_WITHDREW, null));
  }

  public record NicknameUpdateRequest(
      @NotBlank
          @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,8}$", message = "닉네임은 1~8자의 한글, 영어, 숫자만 허용됩니다.")
          String nickname) {}
}
