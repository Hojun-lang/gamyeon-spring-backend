package com.gamyeon.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

  private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{1,8}$");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, length = 8)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OAuthProvider provider;

  @Column(nullable = false)
  private String providerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  @Column private LocalDateTime withdrawnAt;

  protected User() {}

  private User(String email, String nickname, OAuthProvider provider, String providerId) {
    this.email = email;
    this.nickname = nickname;
    this.provider = provider;
    this.providerId = providerId;
    this.status = UserStatus.ACTIVE;
  }

  public static User create(
      String email, String nickname, OAuthProvider provider, String providerId) {
    return new User(email, nickname, provider, providerId);
  }

  public void updateNickname(String nickname) {
    validateNickname(nickname);
    this.nickname = nickname;
  }

  public void withdraw() {
    withdraw(Clock.systemDefaultZone());
  }

  public void withdraw(Clock clock) {
    if (this.status == UserStatus.WITHDREW) {
      throw new UserDomainException(UserErrorCode.DEACTIVATED_USER);
    }
    this.status = UserStatus.WITHDREW;
    this.withdrawnAt = LocalDateTime.now(clock);
  }

  public void restore() {
    if (this.status != UserStatus.WITHDREW || this.withdrawnAt == null) {
      throw new UserDomainException(UserErrorCode.ACCOUNT_NOT_RESTORABLE);
    }
    this.status = UserStatus.ACTIVE;
    this.withdrawnAt = null;
  }

  public void validateNickname(String nickname) {
    if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
      throw new UserDomainException(UserErrorCode.INVALID_NICKNAME_FORMAT);
    }
  }

  public boolean isActive() {
    return this.status == UserStatus.ACTIVE || this.status == UserStatus.WARNED;
  }

  public boolean isBanned() {
    return this.status == UserStatus.BANNED;
  }

  public boolean isWithdrew() {
    return this.status == UserStatus.WITHDREW;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getNickname() {
    return nickname;
  }

  public OAuthProvider getProvider() {
    return provider;
  }

  public String getProviderId() {
    return providerId;
  }

  public UserStatus getStatus() {
    return status;
  }

  public LocalDateTime getWithdrawnAt() {
    return withdrawnAt;
  }
}
