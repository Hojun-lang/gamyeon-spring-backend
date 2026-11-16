package com.gamyeon.common.storage.infrastructure;

import com.gamyeon.common.storage.adapter.out.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

  @Bean
  public S3Presigner s3Presigner(StorageProperties storageProperties) {
    Region region = Region.of(storageProperties.region());
    return S3Presigner.builder()
        .region(region)
        .credentialsProvider(credentialsProvider(storageProperties))
        .build();
  }

  @Bean
  public S3Client s3Client(StorageProperties storageProperties) {
    return S3Client.builder()
        .region(Region.of(storageProperties.region()))
        .credentialsProvider(credentialsProvider(storageProperties))
        .build();
  }

  private AwsCredentialsProvider credentialsProvider(StorageProperties storageProperties) {
    if (StringUtils.hasText(storageProperties.accessKey())
        && StringUtils.hasText(storageProperties.secretKey())) {
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(storageProperties.accessKey(), storageProperties.secretKey()));
    }

    return DefaultCredentialsProvider.create();
  }
}
