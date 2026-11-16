package com.gamyeon.user.application.port.outbound;

import java.util.List;

public interface AccountDataDeletionPort {

  List<String> findFileKeys(Long userId);

  void deleteAllByUserId(Long userId);
}
