package com.gamyeon.notif.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gamyeon.notif.domain.Notif;
import com.gamyeon.notif.domain.NotifType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record NotifResponse(
    @JsonProperty("notifId") Long notifId,
    @JsonProperty("notifType") String notifType,
    @JsonProperty("title") String title,
    @JsonProperty("content") String content,
    @JsonProperty("targetId") Long targetId,

    // Jackson이 멋대로 'read'로 이름을 바꾸는 것을 차단
    @JsonProperty("isRead") boolean isRead,

    //  배열([2026,8,20])이 아닌 프론트가 원하는 문자열(String) 포맷으로 강제 변환
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("createdAt")
        LocalDateTime createdAt) {
  public static NotifResponse from(Notif notif) {
    Long targetId = (notif.getType() == NotifType.NOTICE) ? notif.getNoticeId() : notif.getIntvId();

    return NotifResponse.builder()
        .notifId(notif.getId())
        .notifType(notif.getType().name())
        .title(notif.getTitle())
        .content(notif.getContent())
        .targetId(targetId)
        .isRead(notif.isRead())
        .createdAt(notif.getCreatedAt())
        .build();
  }
}
