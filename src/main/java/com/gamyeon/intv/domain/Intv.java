package com.gamyeon.intv.domain;

import com.gamyeon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "intvs")
public class Intv extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  private String title;

  @Enumerated(EnumType.STRING)
  private IntvStatus status;

  private LocalDateTime startedAt;

  private LocalDateTime finishedAt;

  private LocalDateTime pausedAt;

  private Long durationSeconds;

  @Column(nullable = false)
  private Long totalPausedSeconds;

  protected Intv() {}

  private Intv(Long userId, String title) {
    this.userId = userId;
    this.title = title;
    this.status = IntvStatus.READY;
    this.totalPausedSeconds = 0L;
  }

  public static Intv create(Long userId, String title) {
    return new Intv(userId, title);
  }

  public void updateTitle(String title) {
    this.title = title;
  }

  public void start() {
    if (status != IntvStatus.READY) {
      throw new IntvException(IntvErrorCode.DO_NOT_START);
    }
    this.startedAt = LocalDateTime.now();
    this.status = IntvStatus.IN_PROGRESS;
  }

  public void pause() {
    if (status != IntvStatus.IN_PROGRESS || pausedAt != null) {
      throw new IntvException(IntvErrorCode.DO_NOT_PAUSE);
    }
    this.pausedAt = LocalDateTime.now();
    this.status = IntvStatus.PAUSED;
  }

  public void resume() {
    if (status != IntvStatus.PAUSED || pausedAt == null) {
      throw new IntvException(IntvErrorCode.DO_NOT_RESUME);
    }
    long pausedSeconds = Duration.between(pausedAt, LocalDateTime.now()).toSeconds();
    this.totalPausedSeconds += pausedSeconds;
    this.pausedAt = null;
    this.status = IntvStatus.IN_PROGRESS;
  }

  public void finish() {
    if (status != IntvStatus.IN_PROGRESS) {
      throw new IntvException(IntvErrorCode.DO_NOT_FINISH);
    }
    LocalDateTime now = LocalDateTime.now();
    this.finishedAt = now;
    long totalElapsedSeconds = Duration.between(startedAt, now).getSeconds();
    long actualDurationSeconds = totalElapsedSeconds - totalPausedSeconds;
    this.durationSeconds = actualDurationSeconds;
    this.status = IntvStatus.FINISHED;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getTitle() {
    return title;
  }

  public IntvStatus getStatus() {
    return status;
  }
}
