package com.gamyeon.user.application.port.inbound;

public interface AuthUseCase {

  LoginResult login(OAuthLoginCommand command);

  LoginResult reissue(String refreshToken);

  LoginResult restore(String restoreToken);

  void logout(Long userId);
}
