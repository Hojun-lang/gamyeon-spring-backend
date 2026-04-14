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
import com.gamyeon.user.domain.OAuthProvider;
import com.gamyeon.user.domain.RefreshToken;
import com.gamyeon.user.domain.User;
import com.gamyeon.user.domain.UserDomainException;
import com.gamyeon.user.domain.UserErrorCode;

public class AuthService implements AuthUseCase {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OAuthPort oAuthPort;
  private final TokenPort tokenPort;
  private final NicknameResolver nicknameResolver;

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      OAuthPort oAuthPort,
      TokenPort tokenPort,
      NicknameResolver nicknameResolver) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.oAuthPort = oAuthPort;
    this.tokenPort = tokenPort;
    this.nicknameResolver = nicknameResolver;
  }

  public LoginResult login(OAuthLoginCommand command) {
    OAuthProvider provider = command.getProvider();
    String authCode = command.getAuthorizationCode();

    String oauthAccessToken =
        oAuthPort.getAccessToken(provider, authCode, command.getCodeVerifier());
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

    ensureLoginAllowed(user);

    return issueTokens(user);
  }

  public LoginResult reissue(String refreshTokenValue) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenValue)
            .orElseThrow(() -> new CommonException(CommonErrorCode.EXPIRED_TOKEN));

    if (refreshToken.isExpired()) {
      refreshTokenRepository.deleteByUserId(refreshToken.getUserId());
      throw new CommonException(CommonErrorCode.EXPIRED_TOKEN);
    }

    User user =
        userRepository
            .findById(refreshToken.getUserId())
            .orElseThrow(() -> new UserDomainException(UserErrorCode.USER_NOT_FOUND));

    ensureLoginAllowed(user);

    refreshTokenRepository.deleteByUserId(user.getId());
    return issueTokens(user);
  }

  public void logout(Long userId) {
    refreshTokenRepository.deleteByUserId(userId);
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
