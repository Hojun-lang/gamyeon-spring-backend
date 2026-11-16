package com.gamyeon.common.storage.adapter.out;

import com.gamyeon.common.storage.application.port.out.StorageDeletePort;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Component
@RequiredArgsConstructor
public class S3StorageDeleteAdapter implements StorageDeletePort {

  private final S3Client s3Client;
  private final StorageProperties storageProperties;

  @Override
  public void deleteAll(Collection<String> fileKeys) {
    fileKeys.stream()
        .filter(fileKey -> fileKey != null && !fileKey.isBlank())
        .distinct()
        .forEach(
            fileKey ->
                s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                        .bucket(storageProperties.bucket())
                        .key(fileKey)
                        .build()));
  }
}
