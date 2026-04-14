package com.gamyeon.user.infrastructure.web;

import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.exception.CommonException;
import com.gamyeon.common.response.SuccessResponse;
import com.gamyeon.user.application.port.inbound.AuthUseCase;
import com.gamyeon.user.application.port.inbound.LoginResult;
import com.gamyeon.user.application.port.inbound.OAuthLoginCommand;
import com.gamyeon.user.domain.OAuthProvider;
import com.gamyeon.user.domain.UserSuccessCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

  private final AuthUseCase authUseCase;

  public AuthController(AuthUseCase authUseCase) {
    this.authUseCase = authUseCase;
  }

  @PostMapping("/login/{provider}")
  public ResponseEntity<SuccessResponse<LoginResponse>> login(
      @PathVariable String provider, @Valid @RequestBody LoginRequest request) {
    log.info("Received login request. provider={}", provider);

    OAuthProvider oAuthProvider = parseProvider(provider);
    OAuthLoginCommand command =
        OAuthLoginCommand.of(oAuthProvider, request.authorizationCode(), request.codeVerifier());
    LoginResult result = authUseCase.login(command);
    return ResponseEntity.ok(
        SuccessResponse.of(UserSuccessCode.USER_LOGIN, LoginResponse.from(result)));
  }

  @PostMapping("/reissue")
  public ResponseEntity<SuccessResponse<LoginResponse>> reissue(
      @Valid @RequestBody ReissueRequest request) {
    log.info("Received token reissue request.");

    LoginResult result = authUseCase.reissue(request.refreshToken());
    return ResponseEntity.ok(
        SuccessResponse.of(UserSuccessCode.USER_TOKEN_REISSUED, LoginResponse.from(result)));
  }

  @PostMapping("/logout")
  public ResponseEntity<SuccessResponse<Void>> logout(@AuthenticationPrincipal Long userId) {
    log.info("Received logout request. userId={}", userId);

    authUseCase.logout(userId);
    return ResponseEntity.ok(SuccessResponse.of(UserSuccessCode.USER_LOGOUT, null));
  }

  private OAuthProvider parseProvider(String provider) {
    return switch (provider.toLowerCase()) {
      case "google" -> OAuthProvider.GOOGLE;
      case "kakao" -> OAuthProvider.KAKAO;
      default -> throw new CommonException(CommonErrorCode.INVALID_INPUT);
    };
  }

  public record LoginRequest(@NotBlank String authorizationCode, @NotBlank String codeVerifier) {}

  public record ReissueRequest(@NotBlank String refreshToken) {}

  public record LoginResponse(String accessToken, String refreshToken, UserResponse user) {
    public static LoginResponse from(LoginResult result) {
      return new LoginResponse(
          result.getAccessToken(), result.getRefreshToken(), UserResponse.from(result.getUser()));
    }
  }
}
