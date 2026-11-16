package com.gamyeon.notif.application.port.in;

import com.gamyeon.notif.application.port.in.event.NotifPublishEvent;

public interface CreateNotifUseCase {
  void createAndPush(NotifPublishEvent event);
}
