package com.gamyeon.report.infrastructure.external;

import com.gamyeon.common.config.InternalApiKeyFeignConfig;
import com.gamyeon.report.infrastructure.external.dto.AiReportRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "ai-report-client",
    url = "${ai.server.url}",
    configuration = InternalApiKeyFeignConfig.class)
public interface AiReportClient {

  @PostMapping("/internal/v1/reports/generate")
  void requestGenerate(@RequestBody AiReportRequest request);
}
