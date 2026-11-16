package com.gamyeon.notif.domain;

/** 알림의 종류를 정의하는 통합 도메인 Enum 클래스입니다. */
public enum NotifType {
  NOTICE, // 공지사항 등록 시
  REPORT_PROCESSING, // 면접 분석 진행 중
  REPORT_SUCCESS, // 면접 분석 완료
  REPORT_FAILED // 면접 분석 실패
}
