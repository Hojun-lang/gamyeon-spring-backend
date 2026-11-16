package com.gamyeon.user.application.service;

import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.exception.CommonException;
import com.gamyeon.user.application.port.inbound.AuthUseCase;
import com.gamyeon.user.application.port.inbound.LoginResult;
import com.gamyeon.user.application.port.inbound.OAuthLoginCommand;
import com.gamyeon.user.application.port.inbound.UserInfo;
import com.gamyeon.user.application.port.outbound.OAuthPort;
import com.gamyeon.user.application.port.outbound.RefreshTokenRepository;
import com.gamyeon.user.application.port.outbound.TokenPort;
import com.gamyeon.user.application.port.outbound.UserRepository;
import com.gamyeon.user.domain.AccountDeletionDeadlinePolicy;
import com.gamyeon.user.domain.OAuthProvider;
import com.gamyeon.user.domain.RefreshToken;
import com.gamyeon.user.domain.User;
import com.gamyeon.user.domain.UserDomainException;
import com.gamyeon.user.domain.UserErrorCode;
import io.jsonwebtoken.JwtException;
import org.springframework.transaction.annotation.Transactional;

public class AuthService implements AuthUseCase {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OAuthPort oAuthPort;
  private final TokenPort tokenPort;
  private final NicknameResolver nicknameResolver;
  private final AccountDeletionDeadlinePolicy deadlinePolicy;

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      OAuthPort oAuthPort,
      TokenPort tokenPort,
      NicknameResolver nicknameResolver) {
    this(
        userRepository,
        refreshTokenRepository,
        oAuthPort,
        tokenPort,
        nicknameResolver,
        new AccountDeletionDeadlinePolicy(
            java.time.Clock.system(AccountDeletionDeadlinePolicy.DEFAULT_ZONE),
            AccountDeletionDeadlinePolicy.DEFAULT_ZONE));
  }

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      OAuthPort oAuthPort,
      TokenPort tokenPort,
      NicknameResolver nicknameResolver,
      AccountDeletionDeadlinePolicy deadlinePolicy) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.oAuthPort = oAuthPort;
    this.tokenPort = tokenPort;
    this.nicknameResolver = nicknameResolver;
    this.deadlinePolicy = deadlinePolicy;
  }

  public LoginResult login(OAuthLoginCommand command) {
    OAuthProvider provider = command.getProvider();
    String authCode = command.getAuthorizationCode();

    String oauthAccessToken =
        oAuthPort.getAccessToken(
            provider, authCode, command.getCodeVerifier(), command.getRedirectUri());
    OAuthPort.OAuthUserInfo oAuthUserInfo = oAuthPort.getUserInfo(provider, oauthAccessToken);

    String email = resolveEmail(provider, oAuthUserInfo);
    String nickname = nicknameResolver.resolve(oAuthUserInfo.getNickname(), email);

    User user =
        userRepository
            .findByProviderAndProviderId(provider, oAuthUserInfo.getProviderId())
            .orElseGet(
                () -> {
                  User newUser =
                      User.create(email, nickname, provider, oAuthUserInfo.getProviderId());
                  return userRepository.save(newUser);
                });

    if (user.isWithdrew()) {
      if (!deadlinePolicy.isRestorable(user.getWithdrawnAt())) {
        throw new UserDomainException(UserErrorCode.ACCOUNT_NOT_RESTORABLE);
      }
      return LoginResult.restoreRequired(
          tokenPort.createRestoreToken(user.getId(), user.getProvider()),
          deadlinePolicy.deletionDeadline(user.getWithdrawnAt()));
    }

    ensureLoginAllowed(user);

    return issueTokens(user);
  }

  @Transactional
  public LoginResult reissue(String refreshTokenValue) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenValue)
            .orElseThrow(() -> new CommonException(CommonErrorCode.EXPIRED_TOKEN));

    // 만료된 경우: 예외만 던짐 (delete는 하지 않음)
    // - @Transactional 내에서 RuntimeException 발생 시 rollback되므로 delete가 무의미
    // - 만료 토큰 정리는 스케줄러에 위임
    if (refreshToken.isExpired()) {
      throw new CommonException(CommonErrorCode.EXPIRED_TOKEN);
    }

    User user =
        userRepository
            .findById(refreshToken.getUserId())
            .orElseThrow(() -> new UserDomainException(UserErrorCode.USER_NOT_FOUND));

    ensureLoginAllowed(user);

    // deleteByUserId + save가 하나의 트랜잭션으로 묶임
    // → save 실패 시 delete도 함께 rollback → 기존 토큰 보존, 500 방지
    refreshTokenRepository.deleteByUserId(user.getId());
    return issueTokens(user);
  }

  @Transactional
  public void logout(Long userId) {
    refreshTokenRepository.deleteByUserId(userId);
  }

  @Override
  @Transactional
  public LoginResult restore(String restoreTokenValue) {
    TokenPort.RestoreTokenClaims claims;
    try {
      claims = tokenPort.getRestoreTokenClaims(restoreTokenValue);
    } catch (JwtException | IllegalArgumentException e) {
      throw new UserDomainException(UserErrorCode.INVALID_RESTORE_TOKEN);
    }

    User user =
        userRepository
            .findByIdForUpdate(claims.userId())
            .orElseThrow(() -> new UserDomainException(UserErrorCode.ACCOUNT_NOT_RESTORABLE));

    if (user.getProvider() != claims.provider()
        || !user.isWithdrew()
        || !deadlinePolicy.isRestorable(user.getWithdrawnAt())) {
      throw new UserDomainException(UserErrorCode.ACCOUNT_NOT_RESTORABLE);
    }

    user.restore();
    userRepository.save(user);
    refreshTokenRepository.deleteByUserId(user.getId());
    return issueTokens(user);
  }

  private String resolveEmail(OAuthProvider provider, OAuthPort.OAuthUserInfo userInfo) {
    String email = userInfo.getEmail();
    if (email != null && !email.isBlank()) {
      return email;
    }
    // 이메일 동의 거부(또는 비즈니스 앱 미전환) 시 합성 이메일 사용
    return provider.name().toLowerCase()
        + "_"
        + userInfo.getProviderId()
        + "@"
        + provider.name().toLowerCase()
        + ".local";
  }

  private void ensureLoginAllowed(User user) {
    if (user.isBanned() || user.isWithdrew()) {
      throw new UserDomainException(UserErrorCode.DEACTIVATED_USER);
    }
  }

  private LoginResult issueTokens(User user) {
    String accessToken = tokenPort.createAccessToken(user.getId());
    String refreshTokenValue = tokenPort.createRefreshToken(user.getId());

    RefreshToken refreshToken =
        RefreshToken.create(user.getId(), refreshTokenValue, tokenPort.getRefreshTokenExpiry());
    refreshTokenRepository.save(refreshToken);

    return LoginResult.of(accessToken, refreshTokenValue, UserInfo.from(user));
  }
}
