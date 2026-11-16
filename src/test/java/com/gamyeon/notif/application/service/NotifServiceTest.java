package com.gamyeon.notif.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.gamyeon.notif.application.port.in.event.NotifPublishEvent;
import com.gamyeon.notif.domain.Notif;
import com.gamyeon.notif.domain.NotifType;
import com.gamyeon.notif.infrastructure.persistence.NotifRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class NotifServiceTest {

  @InjectMocks private NotifService notifService;

  @Mock private NotifRepository notifRepository;

  //  새롭게 추가된 의존성 Mock 객체 주입
  @Mock private NoticeSyncService noticeSyncService;

  @Test
  @DisplayName("SSE 구독을 요청하면 SseEmitter 객체를 반환하고 초기 연결 이벤트를 발송하며 공지사항 동기화를 트리거한다.")
  void subscribe_success() {
    // given
    Long userId = 100L;

    // when
    SseEmitter emitter = notifService.subscribe(userId);

    // then
    assertThat(emitter).isNotNull();
    assertThat(emitter.getTimeout()).isEqualTo(60L * 1000 * 5);

    // 공지사항 동기화 로직이 1번 잘 호출되었는지 검증
    verify(noticeSyncService, times(1)).syncMissingNoticesAsync(userId);
  }

  @Test
  @DisplayName("알림 단건 읽음 처리 시, 본인의 알림이면 isRead 상태가 true로 변경된다.")
  void readNotif_success() {
    // given
    Long userId = 1L;
    Long notifId = 10L;
    Notif notif =
        Notif.builder()
            .userId(userId)
            .type(NotifType.REPORT_SUCCESS)
            .title("제목")
            .content("내용")
            .intvId(50L)
            .build();

    given(notifRepository.findById(notifId)).willReturn(Optional.of(notif));

    // when
    notifService.readNotif(userId, notifId);

    // then
    assertThat(notif.isRead()).isTrue();
  }

  @Test
  @DisplayName("타인의 알림을 읽음 처리 시도하면 IllegalArgumentException 예외가 발생한다.")
  void readNotif_throwsException_whenNotOwner() {
    // given
    Long requesterId = 999L;
    Long ownerId = 1L;
    Long notifId = 10L;
    Notif notif =
        Notif.builder()
            .userId(ownerId)
            .type(NotifType.NOTICE)
            .title("제목")
            .content("내용")
            .noticeId(5L)
            .build();

    given(notifRepository.findById(notifId)).willReturn(Optional.of(notif));

    // when & then
    assertThatThrownBy(() -> notifService.readNotif(requesterId, notifId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("본인의 알림만 읽음 처리할 수 있습니다.");
  }

  @Test
  @DisplayName("이벤트를 전달받아 알림 생성 시, DB save가 정상 호출된다.")
  void createAndPush_success() {
    // given
    NotifPublishEvent event = new NotifPublishEvent(1L, NotifType.NOTICE, "공지", "내용", 77L);
    Notif savedMock =
        Notif.builder()
            .userId(1L)
            .type(NotifType.NOTICE)
            .title("공지")
            .content("내용")
            .noticeId(77L)
            .build();

    given(notifRepository.save(any(Notif.class))).willReturn(savedMock);

    // when
    notifService.createAndPush(event);

    // then
    verify(notifRepository, times(1)).save(any(Notif.class));
  }
}
