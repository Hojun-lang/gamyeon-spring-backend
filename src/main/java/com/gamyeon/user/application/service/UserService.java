package com.gamyeon.user.application.service;

import com.gamyeon.user.application.port.inbound.NicknameUpdateCommand;
import com.gamyeon.user.application.port.inbound.UserInfo;
import com.gamyeon.user.application.port.inbound.UserUseCase;
import com.gamyeon.user.application.port.outbound.RefreshTokenRepository;
import com.gamyeon.user.application.port.outbound.UserRepository;
import com.gamyeon.user.domain.User;
import com.gamyeon.user.domain.UserDomainException;
import com.gamyeon.user.domain.UserErrorCode;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

public class UserService implements UserUseCase {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final Clock clock;

  public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
    this(userRepository, refreshTokenRepository, Clock.systemDefaultZone());
  }

  public UserService(
      UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, Clock clock) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.clock = clock;
  }

  public UserInfo getMyInfo(Long userId) {
    User user = findActiveUser(userId);
    return UserInfo.from(user);
  }

  public UserInfo updateNickname(NicknameUpdateCommand command) {
    User user = findActiveUser(command.getUserId());
    user.updateNickname(command.getNickname());
    userRepository.save(user);
    return UserInfo.from(user);
  }

  @Transactional
  public void withdraw(Long userId) {
    User user = findActiveUser(userId);
    user.withdraw(clock);
    userRepository.save(user);
    refreshTokenRepository.deleteByUserId(userId);
  }

  private User findActiveUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserDomainException(UserErrorCode.USER_NOT_FOUND));
    if (user.isBanned() || user.isWithdrew()) {
      throw new UserDomainException(UserErrorCode.DEACTIVATED_USER);
    }
    return user;
  }
}
