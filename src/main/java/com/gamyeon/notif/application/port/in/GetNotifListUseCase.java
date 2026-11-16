package com.gamyeon.notif.application.port.in;

import com.gamyeon.notif.application.port.in.dto.NotifListResponse;

public interface GetNotifListUseCase {
  NotifListResponse getNotifs(Long userId, Long cursorId, int size);
}
