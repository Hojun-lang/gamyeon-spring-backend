package com.gamyeon.notif.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamyeon.notif.application.port.in.dto.NotifListResponse;
import com.gamyeon.notif.application.port.in.event.NotifPublishEvent;
import com.gamyeon.notif.domain.Notif;
import com.gamyeon.notif.domain.NotifType;
import com.gamyeon.notif.infrastructure.persistence.NotifRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ActiveProfiles("test")
@SpringBootTest
@Transactional // 테스트 종료 후 DB 자동 롤백
class NotifIntegrationTest {

  @Autowired private NotifService notifService;

  @Autowired private NotifRepository notifRepository;

  @MockitoBean private S3Presigner s3Presigner;

  @Test
  @DisplayName("[DB 통합 검증] NOTICE 타입 알림 생성 시 noticeId에 값이 들어가고 intvId는 null이어야 한다.")
  void createNoticeTypeNotification_integration() {
    // given
    Long userId = 500L;
    NotifPublishEvent event =
        new NotifPublishEvent(userId, NotifType.NOTICE, "긴급 점검 공지", "서버 점검합니다.", 1004L);

    // when
    notifService.createAndPush(event);

    // then
    List<Notif> result = notifRepository.findByUserIdOrderByCreatedAtDesc(userId);
    assertThat(result).hasSize(1);

    Notif saved = result.get(0);
    assertThat(saved.getNoticeId()).isEqualTo(1004L);
    assertThat(saved.getIntvId()).isNull(); // 인터뷰 ID는 null 검증
    assertThat(saved.isRead()).isFalse();
  }

  @Test
  @DisplayName("[DB 통합 검증] REPORT_SUCCESS 타입 알림 생성 시 intvId에 값이 들어가고 noticeId는 null이어야 한다.")
  void createReportTypeNotification_integration() {
    // given
    Long userId = 500L;
    NotifPublishEvent event =
        new NotifPublishEvent(userId, NotifType.REPORT_SUCCESS, "분석 완료", "결과 확인하세요.", 8823L);

    // when
    notifService.createAndPush(event);

    // then
    List<Notif> result = notifRepository.findByUserIdOrderByCreatedAtDesc(userId);
    assertThat(result).hasSize(1);

    Notif saved = result.get(0);
    assertThat(saved.getIntvId()).isEqualTo(8823L);
    assertThat(saved.getNoticeId()).isNull();
  }

  @Test
  @DisplayName("[JPA 더티체킹 통합 검증] readNotif 호출 시 DB에 반영되어 unreadCount가 줄어든다.")
  void readNotif_dirtyChecking_integration() {
    // given
    Long userId = 777L;
    Notif notif1 =
        notifRepository.save(
            Notif.builder()
                .userId(userId)
                .type(NotifType.NOTICE)
                .title("A")
                .content("A")
                .noticeId(1L)
                .build());
    Notif notif2 =
        notifRepository.save(
            Notif.builder()
                .userId(userId)
                .type(NotifType.REPORT_FAILED)
                .title("B")
                .content("B")
                .intvId(2L)
                .build());

    // when: 1번 알림만 읽음 처리
    notifService.readNotif(userId, notif1.getId());

    // then: DB 조회 시 안 읽은 개수는 1개여야 함
    NotifListResponse response = notifService.getNotifs(userId, null, 10);
    assertThat(response.unreadCount()).isEqualTo(1);
    assertThat(response.notifs().get(1).isRead()).isTrue(); // 먼저 들어간 notif1이 하단(index 1)에 위치
  }
}
