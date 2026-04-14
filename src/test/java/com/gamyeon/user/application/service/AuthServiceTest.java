package com.gamyeon.user.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.exception.CommonException;
import com.gamyeon.user.application.port.inbound.LoginResult;
import com.gamyeon.user.application.port.inbound.OAuthLoginCommand;
import com.gamyeon.user.application.port.outbound.OAuthPort;
import com.gamyeon.user.application.port.outbound.OAuthPort.OAuthUserInfo;
import com.gamyeon.user.application.port.outbound.RefreshTokenRepository;
import com.gamyeon.user.application.port.outbound.TokenPort;
import com.gamyeon.user.application.port.outbound.UserRepository;
import com.gamyeon.user.domain.OAuthProvider;
import com.gamyeon.user.domain.RefreshToken;
import com.gamyeon.user.domain.User;
import com.gamyeon.user.domain.UserDomainException;
import com.gamyeon.user.domain.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("인증 서비스 - 로그인 / 토큰 재발급 / 로그아웃")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private OAuthPort oAuthPort;
  @Mock private TokenPort tokenPort;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userRepository, refreshTokenRepository, oAuthPort, tokenPort, new NicknameResolver());
  }

  // ── login ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("TAS-001 - 신규 유저 로그인 시 유저를 저장하고 JWT를 발급해야 한다")
  void shouldSaveUserAndIssueJwtWhenNewUserLogsIn() {
    OAuthLoginCommand command =
        OAuthLoginCommand.of(OAuthProvider.GOOGLE, "auth-code", "code-verifier");

    given(oAuthPort.getAccessToken(OAuthProvider.GOOGLE, "auth-code", "code-verifier"))
        .willReturn("oauth-token");
    given(oAuthPort.getUserInfo(OAuthProvider.GOOGLE, "oauth-token"))
        .willReturn(mockUserInfo("google-123", "test@gmail.com", "테스터"));
    given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
        .willReturn(Optional.empty());
    given(userRepository.save(any(User.class))).willAnswer(i -> withId(i.getArgument(0), 1L));
    given(tokenPort.createAccessToken(1L)).willReturn("access-token");
    given(tokenPort.createRefreshToken(1L)).willReturn("refresh-token");
    given(tokenPort.getRefreshTokenExpiry()).willReturn(604_800_000L);

    LoginResult result = authService.login(command);

    assertEquals("access-token", result.getAccessToken(), "발급된 액세스 토큰이 일치해야 합니다");
    assertEquals("refresh-token", result.getRefreshToken(), "발급된 리프레시 토큰이 일치해야 합니다");
    verify(userRepository).save(any(User.class));
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("TAS-002 - 기존 유저 로그인 시 유저를 저장하지 않고 JWT를 발급해야 한다")
  void shouldNotSaveUserWhenExistingUserLogsIn() {
    User existingUser = existingUser(1L, OAuthProvider.GOOGLE, "google-123");
    OAuthLoginCommand command =
        OAuthLoginCommand.of(OAuthProvider.GOOGLE, "auth-code", "code-verifier");

    given(oAuthPort.getAccessToken(OAuthProvider.GOOGLE, "auth-code", "code-verifier"))
        .willReturn("oauth-token");
    given(oAuthPort.getUserInfo(OAuthProvider.GOOGLE, "oauth-token"))
        .willReturn(mockUserInfo("google-123", "test@gmail.com", "테스터"));
    given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
        .willReturn(Optional.of(existingUser));
    given(tokenPort.createAccessToken(1L)).willReturn("access-token");
    given(tokenPort.createRefreshToken(1L)).willReturn("refresh-token");
    given(tokenPort.getRefreshTokenExpiry()).willReturn(604_800_000L);

    authService.login(command);

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("TAS-003 - 정지된 유저가 로그인하면 예외가 발생해야 한다")
  void shouldThrowExceptionWhenBannedUserLogsIn() {
    User bannedUser = userWithStatus(1L, OAuthProvider.GOOGLE, "google-123", UserStatus.BANNED);
    OAuthLoginCommand command =
        OAuthLoginCommand.of(OAuthProvider.GOOGLE, "auth-code", "code-verifier");

    given(oAuthPort.getAccessToken(OAuthProvider.GOOGLE, "auth-code", "code-verifier"))
        .willReturn("oauth-token");
    given(oAuthPort.getUserInfo(OAuthProvider.GOOGLE, "oauth-token"))
        .willReturn(mockUserInfo("google-123", "test@gmail.com", "테스터"));
    given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
        .willReturn(Optional.of(bannedUser));

    assertThrows(
        UserDomainException.class, () -> authService.login(command), "정지된 유저 로그인 시 예외가 발생해야 합니다");
  }

  @Test
  @DisplayName("TAS-004 - 탈퇴한 유저가 로그인하면 예외가 발생해야 한다")
  void shouldThrowExceptionWhenWithdrewUserLogsIn() {
    User withdrewUser = userWithStatus(1L, OAuthProvider.GOOGLE, "google-123", UserStatus.WITHDREW);
    OAuthLoginCommand command =
        OAuthLoginCommand.of(OAuthProvider.GOOGLE, "auth-code", "code-verifier");

    given(oAuthPort.getAccessToken(OAuthProvider.GOOGLE, "auth-code", "code-verifier"))
        .willReturn("oauth-token");
    given(oAuthPort.getUserInfo(OAuthProvider.GOOGLE, "oauth-token"))
        .willReturn(mockUserInfo("google-123", "test@gmail.com", "테스터"));
    given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
        .willReturn(Optional.of(withdrewUser));

    assertThrows(
        UserDomainException.class, () -> authService.login(command), "탈퇴한 유저 로그인 시 예외가 발생해야 합니다");
  }

  // ── reissue ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("TAS-005 - 유효한 리프레시 토큰으로 새로운 토큰을 재발급해야 한다")
  void shouldIssueNewTokensWhenRefreshTokenIsValid() {
    User user = existingUser(1L, OAuthProvider.GOOGLE, "google-123");
    RefreshToken refreshToken = RefreshToken.create(1L, "valid-refresh-token", 604_800_000L);

    given(refreshTokenRepository.findByToken("valid-refresh-token"))
        .willReturn(Optional.of(refreshToken));
    given(userRepository.findById(1L)).willReturn(Optional.of(user));
    given(tokenPort.createAccessToken(1L)).willReturn("new-access-token");
    given(tokenPort.createRefreshToken(1L)).willReturn("new-refresh-token");
    given(tokenPort.getRefreshTokenExpiry()).willReturn(604_800_000L);

    LoginResult result = authService.reissue("valid-refresh-token");

    assertEquals("new-access-token", result.getAccessToken(), "재발급된 액세스 토큰이 일치해야 합니다");
    assertEquals("new-refresh-token", result.getRefreshToken(), "재발급된 리프레시 토큰이 일치해야 합니다");
    verify(refreshTokenRepository).deleteByUserId(1L);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("TAS-006 - 존재하지 않는 리프레시 토큰으로 재발급 시 예외가 발생해야 한다")
  void shouldThrowExceptionWhenRefreshTokenNotFound() {
    given(refreshTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

    CommonException exception =
        assertThrows(
            CommonException.class,
            () -> authService.reissue("unknown-token"),
            "존재하지 않는 리프레시 토큰으로 재발급 시 예외가 발생해야 합니다");

    assertEquals(
        CommonErrorCode.EXPIRED_TOKEN, exception.getErrorCode(), "EXPIRED_TOKEN 에러코드로 응답해야 합니다");
  }

  @Test
  @DisplayName("TAS-007 - 만료된 리프레시 토큰으로 재발급 시 예외가 발생해야 한다")
  void shouldThrowExceptionWhenRefreshTokenIsExpired() {
    RefreshToken expiredToken = RefreshToken.create(1L, "expired-token", -1L);

    given(refreshTokenRepository.findByToken("expired-token"))
        .willReturn(Optional.of(expiredToken));

    CommonException exception =
        assertThrows(
            CommonException.class,
            () -> authService.reissue("expired-token"),
            "만료된 리프레시 토큰으로 재발급 시 예외가 발생해야 합니다");

    assertEquals(
        CommonErrorCode.EXPIRED_TOKEN, exception.getErrorCode(), "EXPIRED_TOKEN 에러코드로 응답해야 합니다");
    verify(refreshTokenRepository).deleteByUserId(1L);
  }

  // ── logout ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("TAS-008 - 로그아웃 시 리프레시 토큰을 삭제해야 한다")
  void shouldDeleteRefreshTokenWhenUserLogsOut() {
    authService.logout(1L);

    verify(refreshTokenRepository).deleteByUserId(1L);
  }

  // ── helpers ─────────────────────────────────────────────────────────────

  private User withId(User user, Long id) {
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private User existingUser(Long id, OAuthProvider provider, String providerId) {
    User user = User.create("test@gmail.com", "테스터", provider, providerId);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private User userWithStatus(
      Long id, OAuthProvider provider, String providerId, UserStatus status) {
    User user = User.create("test@gmail.com", "테스터", provider, providerId);
    ReflectionTestUtils.setField(user, "id", id);
    ReflectionTestUtils.setField(user, "status", status);
    return user;
  }

  private OAuthUserInfo mockUserInfo(String providerId, String email, String nickname) {
    return new OAuthUserInfo() {
      @Override
      public String getProviderId() {
        return providerId;
      }

      @Override
      public String getEmail() {
        return email;
      }

      @Override
      public String getNickname() {
        return nickname;
      }
    };
  }
}
