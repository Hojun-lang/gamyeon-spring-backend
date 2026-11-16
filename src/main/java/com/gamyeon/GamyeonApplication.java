package com.gamyeon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync; // <-- [추가 1]
import org.springframework.scheduling.annotation.EnableScheduling; // <-- [추가 2]

@EnableAsync // 비동기 이벤트 리스너 (@Async) 가동 스위치
@EnableScheduling // 30초 핑 및 새벽 4시 청소 스케줄러 가동 스위치
@EnableFeignClients(basePackages = "com.gamyeon")
@SpringBootApplication
@EnableJpaAuditing
public class GamyeonApplication {

  public static void main(String[] args) {
    SpringApplication.run(GamyeonApplication.class, args);
  }
}
