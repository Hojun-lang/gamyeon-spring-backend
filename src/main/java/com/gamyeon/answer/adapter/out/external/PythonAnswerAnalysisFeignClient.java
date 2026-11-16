package com.gamyeon.answer.adapter.out.external;

import com.gamyeon.common.config.InternalApiKeyFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "pythonAnswerAnalysisClient",
    url = "${ai.server.url}",
    configuration = InternalApiKeyFeignConfig.class)
public interface PythonAnswerAnalysisFeignClient {

  @PostMapping("/internal/v1/answers/analyze")
  void requestAnalysis(@RequestBody PythonAnswerAnalysisRequest request);
}
