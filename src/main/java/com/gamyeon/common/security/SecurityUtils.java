package com.gamyeon.common.security;

import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.exception.CommonException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 서비스 레이어에서 현재 로그인된 사용자의 userId를 꺼낼 때 사용하는 유틸.
 *
 * <p>컨트롤러에서는 {@code @AuthenticationPrincipal Long userId}를 사용하고, 서비스 레이어에서 직접 userId가 필요한 경우 이 클래스를
 * 사용합니다.
 *
 * <p>사용 예:
 *
 * <pre>
 *   Long userId = SecurityUtils.getCurrentUserId();
 * </pre>
 */
public final class SecurityUtils {

  private SecurityUtils() {}

  public static Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new CommonException(CommonErrorCode.UNAUTHORIZED);
    }

    Object principal = authentication.getPrincipal();

    if (!(principal instanceof Long)) {
      throw new CommonException(CommonErrorCode.UNAUTHORIZED);
    }

    return (Long) principal;
  }
}
