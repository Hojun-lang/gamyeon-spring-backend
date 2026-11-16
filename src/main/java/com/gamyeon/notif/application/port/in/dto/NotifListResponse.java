package com.gamyeon.notif.application.port.in.dto;

import java.util.List;

/** 알림 목록 조회 API의 data 객체 규격 DTO입니다. */
public record NotifListResponse(int unreadCount, List<NotifResponse> notifs) {
  public static NotifListResponse of(int unreadCount, List<NotifResponse> notifs) {
    return new NotifListResponse(unreadCount, notifs);
  }
}
