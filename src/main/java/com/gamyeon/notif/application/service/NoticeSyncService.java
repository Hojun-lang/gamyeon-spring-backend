package com.gamyeon.notif.application.service;

import com.gamyeon.dashboard.application.port.outbound.NoticeRepository;
import com.gamyeon.dashboard.domain.Notice;
import com.gamyeon.notif.application.port.in.event.NotifPublishEvent;
import com.gamyeon.notif.domain.NotifType;
import com.gamyeon.notif.infrastructure.persistence.NotifRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 유저가 접속했을 때 백그라운드에서 누락된 공지사항을 채워주는 클래스
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeSyncService {

  private final NoticeRepository noticeRepository;
  private final NotifRepository notifRepository;
  private final ApplicationEventPublisher eventPublisher;

  /** 유저 접속 시 비동기로 실행되어 누락된 공지사항을 알림으로 발송합니다. */
  @Async
  @Transactional
  public void syncMissingNoticesAsync(Long userId) {
    try {
      // 1. 유저가 이미 받은 공지사항 ID 목록 세팅
      Set<Long> receivedNoticeIds =
          notifRepository.findReceivedNoticeIdsByUserId(userId, NotifType.NOTICE).stream()
              .collect(Collectors.toSet());

      // 2. 대시보드 모듈에서 전체 공지사항 조회
      List<Notice> allNotices = noticeRepository.findAll();

      // 3. 아직 받지 못한 공지사항이 있다면 알림 이벤트 발행!
      for (Notice notice : allNotices) {
        if (!receivedNoticeIds.contains(notice.getId())) {

          // 기존에 만들어둔 알림 이벤트 재활용 (자동으로 DB저장 + SSE 푸시까지 연결)
          eventPublisher.publishEvent(
              new NotifPublishEvent(
                  userId,
                  NotifType.NOTICE,
                  "[공지] " + notice.getTitle(),
                  notice.getContent(), // 공지 내용
                  notice.getId()));

          log.info("[NoticeSync] 유저 {}에게 누락된 공지사항(ID: {}) 동기화 이벤트 발행 완료", userId, notice.getId());
        }
      }
    } catch (Exception e) {
      log.error("[NoticeSync] 공지사항 동기화 중 오류 발생 - userId: {}", userId, e);
    }
  }
}
