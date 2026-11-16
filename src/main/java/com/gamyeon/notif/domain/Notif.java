package com.gamyeon.notif.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 실용적 아키텍처에 맞춰 도메인 모델과 영속성 객체를 하나로 통합한 엔티티입니다. 비즈니스 로직(markAsRead)과 데이터베이스 테이블 매핑 정보를 모두 포함합니다. */
@Entity
@Table(name = "NOTIFICATIONS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notif {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotifType type;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  // 다형성 라우팅을 위한 대상 ID 필드
  @Column(name = "notice_id")
  private Long noticeId;

  @Column(name = "intv_id")
  private Long intvId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  public Notif(
      Long userId, NotifType type, String title, String content, Long noticeId, Long intvId) {
    this.userId = userId;
    this.type = type;
    this.title = title;
    this.content = content;
    this.noticeId = noticeId;
    this.intvId = intvId;
    this.isRead = false; // 생성 시 기본값은 읽지 않음
  }

  /** [비즈니스 로직] 알림 읽음 처리 JPA의 더티 체킹(Dirty Checking)을 통해 트랜잭션 종료 시 자동으로 Update 쿼리가 발생합니다. */
  public void markAsRead() {
    if (!this.isRead) {
      this.isRead = true;
    }
  }
}
