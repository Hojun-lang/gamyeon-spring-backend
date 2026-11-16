package com.gamyeon.common.storage.application.port.out;

import java.util.Collection;

public interface StorageDeletePort {

  void deleteAll(Collection<String> fileKeys);
}
