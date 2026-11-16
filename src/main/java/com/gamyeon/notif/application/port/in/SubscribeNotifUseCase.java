package com.gamyeon.notif.application.port.in;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SubscribeNotifUseCase {
  SseEmitter subscribe(Long userId);
}
