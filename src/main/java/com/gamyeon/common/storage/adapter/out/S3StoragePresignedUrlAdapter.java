package com.gamyeon.common.storage.adapter.out;

import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlCommand;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlPort;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlResult;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3StoragePresignedUrlAdapter implements StoragePresignedUrlPort {

  private final S3Presigner s3Presigner;
  private final StorageProperties storageProperties;

  @Override
  public StoragePresignedUrlResult createUploadUrl(StoragePresignedUrlCommand command) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(storageProperties.bucket())
            .key(command.fileKey())
            .contentType(command.contentType())
            .build();

    Duration expiration = Duration.ofSeconds(storageProperties.presignedDurationSeconds());
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .putObjectRequest(putObjectRequest)
            .build();

    PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

    return new StoragePresignedUrlResult(
        command.fileKey(),
        presignedRequest.url().toString(),
        createFileUrl(command.fileKey()),
        expiration.getSeconds());
  }

  private String createFileUrl(String fileKey) {
    return "https://%s.s3.%s.amazonaws.com/%s"
        .formatted(storageProperties.bucket(), storageProperties.region(), fileKey);
  }

  // 읽기용 Presigned URL 생성
  @Override
  public String createReadUrl(String fileUrlOrKey) {
    if (fileUrlOrKey == null || fileUrlOrKey.isBlank()) {
      return null;
    }

    // 1. 전체 URL 형식이라면 Key 부분만 추출
    String fileKey = extractFileKey(fileUrlOrKey);

    // 2. 읽기 요청(GetObjectRequest) 객체 생성
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(storageProperties.bucket()).key(fileKey).build();

    // 3. 비즈니스 정책인 7일(일주일)로 만료 기한 설정
    Duration expiration = Duration.ofDays(7);
    // 만약 application.yml에 있는 값을 쓰고 싶다면:
    // Duration.ofSeconds(storageProperties.presignedDurationSeconds())

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(getObjectRequest)
            .build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

    return presignedRequest.url().toString();
  }

  // 전체 S3 도메인 주소를 떼어내고 파일 Key만 추출하는 헬퍼 메서드
  private String extractFileKey(String fileUrl) {
    String s3DomainPrefix =
        "https://%s.s3.%s.amazonaws.com/"
            .formatted(storageProperties.bucket(), storageProperties.region());
    if (fileUrl.startsWith(s3DomainPrefix)) {
      return fileUrl.substring(s3DomainPrefix.length()); // "answers/..." 부분만 반환
    }
    return fileUrl; // 이미 Key 형태라면 그대로 반환
  }
}
