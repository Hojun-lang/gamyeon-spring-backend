package com.gamyeon.notif.application.service;

import com.gamyeon.notif.application.port.in.*;
import com.gamyeon.notif.application.port.in.dto.NotifListResponse;
import com.gamyeon.notif.application.port.in.dto.NotifResponse;
import com.gamyeon.notif.application.port.in.event.NotifPublishEvent;
import com.gamyeon.notif.domain.Notif;
import com.gamyeon.notif.domain.NotifType;
import com.gamyeon.notif.infrastructure.persistence.NotifRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifService
    implements SubscribeNotifUseCase,
        GetNotifListUseCase,
        ReadNotifUseCase,
        ReadAllNotifsUseCase,
        CreateNotifUseCase {

  private final NotifRepository notifRepository;
  private final NoticeSyncService noticeSyncService;

  // 유저별 SSE 연결을 관리하는 메모리 저장소
  private final Map<Long, SseEmitter> emitterRepository = new ConcurrentHashMap<>();

  // SSE 타임아웃 5분 (Nginx 등 인프라 설정과 조율 필요)
  private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 5;

  @Override
  @Transactional
  public void createAndPush(NotifPublishEvent event) {
    // 1. targetId 분기 처리 (NOTICE 타입이면 noticeId에, 아니면 intvId에 할당)
    Long noticeId = (event.type() == NotifType.NOTICE) ? event.targetId() : null;
    Long intvId = (event.type() != NotifType.NOTICE) ? event.targetId() : null;

    // 2. 통합 Notif 엔티티 생성 및 DB 저장
    Notif notif =
        Notif.builder()
            .userId(event.userId())
            .type(event.type())
            .title(event.title())
            .content(event.content())
            .noticeId(noticeId)
            .intvId(intvId)
            .build();

    Notif savedNotif = notifRepository.save(notif);

    // 3. DB 저장이 완료되면, 실시간 SSE 스트림으로 클라이언트에게 푸시
    NotifResponse responseDto = NotifResponse.from(savedNotif);
    sendNotifToClient(event.userId(), responseDto);
  }

  @Override
  public SseEmitter subscribe(Long userId) {
    // 1. 기존 연결이 있다면 제거 (중복 연결 방지)
    if (emitterRepository.containsKey(userId)) {
      emitterRepository.get(userId).complete();
      emitterRepository.remove(userId);
    }
    // 1.5 비동기 공지사항 동기화 트리거 (메인 스레드 블로킹 없음)
    noticeSyncService.syncMissingNoticesAsync(userId);

    // 2. 새로운 Emitter 생성 및 저장
    SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
    emitterRepository.put(userId, emitter);

    // 3. 메모리 누수 방지 (가장 중요)
    emitter.onCompletion(() -> emitterRepository.remove(userId));
    emitter.onTimeout(
        () -> {
          emitter.complete();
          emitterRepository.remove(userId);
        });
    emitter.onError(
        (e) -> {
          emitter.completeWithError(e);
          emitterRepository.remove(userId);
        });

    // 4. 초기 연결 더미 데이터 전송 (503 에러 방지)
    try {
      emitter.send(SseEmitter.event().name("connect").data("connected"));
    } catch (IOException e) {
      log.error("SSE 초기 연결 이벤트 전송 실패 - userId: {}", userId, e);
      emitterRepository.remove(userId);
    }

    return emitter;
  }

  @Override
  @Transactional(readOnly = true)
  public NotifListResponse getNotifs(Long userId, Long cursorId, int size) {
    // 1. 안 읽은 알림 카운트 쿼리 실행
    int unreadCount = (int) notifRepository.countByUserIdAndIsReadFalse(userId);

    // 2. 요청된 size만큼만 가져오도록 Pageable 설정 (0페이지 고정, 개수는 size만큼)
    org.springframework.data.domain.Pageable pageable =
        org.springframework.data.domain.PageRequest.of(0, size);

    // 3. cursorId 유무에 따른 No-Offset DB 조회 분기 처리
    List<Notif> notifs;
    if (cursorId == null) {
      // 처음 알림창을 열었을 때 (최신 5개)
      notifs = notifRepository.findByUserIdOrderByIdDesc(userId, pageable);
    } else {
      // 스크롤을 내려서 과거 내역을 더 요청했을 때
      notifs = notifRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursorId, pageable);
    }

    // 4. DTO 변환 후 반환
    List<NotifResponse> responseList =
        notifs.stream().map(NotifResponse::from).collect(Collectors.toList());

    return NotifListResponse.of(unreadCount, responseList);
  }

  @Override
  @Transactional
  public void readNotif(Long userId, Long notifId) {
    // 공통 예외 처리(Exception)는 프로젝트 룰에 맞춰 적용하시면 됩니다.
    Notif notif =
        notifRepository
            .findById(notifId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));

    if (!notif.getUserId().equals(userId)) {
      throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
    }

    // JPA Dirty Checking을 통한 상태 업데이트
    notif.markAsRead();
  }

  @Override
  @Transactional
  public void readAllNotifs(Long userId) {
    // 벌크 업데이트 쿼리로 일괄 처리 (성능 최적화)
    notifRepository.markAllAsReadByUserId(userId);
  }

  /** 타 도메인에서 이벤트 발행 시 호출될 실시간 알림 푸시 메서드 (Step 4에서 사용) */
  public void sendNotifToClient(Long userId, NotifResponse responseDto) {
    SseEmitter emitter = emitterRepository.get(userId);
    if (emitter != null) {
      try {
        emitter.send(SseEmitter.event().name("notif").data(responseDto));
      } catch (IOException e) {
        log.error("SSE 알림 전송 실패 - userId: {}", userId, e);
        emitterRepository.remove(userId);
      }
    }
  }

  // ================= [ 스케줄러 지원용 비즈니스 로직 ] =================
  /** [하트비트 발송] 연결된 모든 유저에게 ping 데이터를 쏩니다. (발송 실패 시 연결이 끊긴 것으로 판단하고 즉시 Map에서 청소) */
  public void sendHeartbeat() {
    emitterRepository.forEach(
        (userId, emitter) -> {
          try {
            emitter.send(SseEmitter.event().name("ping").data("heartbeat"));
          } catch (Exception e) {
            log.debug("SSE 하트비트 발송 실패 (클라이언트 이탈 감지) - userId: {}", userId);
            emitter.complete();
            emitterRepository.remove(userId); // ConcurrentHashMap이므로 순회 중 삭제 안전함
          }
        });
  }

  /** [오래된 데이터 청소] 기준 일수(days)가 지난 과거의 알림 데이터를 DB에서 물리적 삭제합니다. */
  @Transactional
  public void cleanupOldNotifications(int days) {
    LocalDateTime threshold = LocalDateTime.now().minusDays(days);
    notifRepository.deleteByCreatedAtBefore(threshold);
    log.info("보관 주기({}일) 경과 알림 데이터 청소 완료 - 기준 시간: {}", days, threshold);
  }
}
