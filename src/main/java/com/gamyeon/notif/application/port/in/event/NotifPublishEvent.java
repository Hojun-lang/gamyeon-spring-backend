package com.gamyeon.notif.application.port.in.event;

import com.gamyeon.notif.domain.NotifType;

/** 타 도메인(Report, Notice)에서 알림 발행을 위해 사용하는 표준 이벤트 규격입니다. */
public record NotifPublishEvent(
    Long userId,
    NotifType type,
    String title,
    String content,
    Long targetId // 공지사항일 땐 noticeId, 리포트일 땐 intvId로 매핑됨
    ) {}
