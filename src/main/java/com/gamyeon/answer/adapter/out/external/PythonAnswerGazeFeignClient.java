package com.gamyeon.answer.adapter.out.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamyeon.common.config.InternalApiKeyFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "pythonAnswerGazeClient",
    url = "${ai.server.url}",
    configuration = InternalApiKeyFeignConfig.class)
public interface PythonAnswerGazeFeignClient {

  @PostMapping("/internal/v1/gaze-batches")
  void send(@RequestBody JsonNode request);
}
