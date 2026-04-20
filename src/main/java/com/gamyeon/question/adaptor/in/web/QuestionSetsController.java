package com.gamyeon.question.adaptor.in.web;

import com.gamyeon.common.response.ApiResponse;
import com.gamyeon.question.adaptor.in.dto.response.QuestionSetResponse;
import com.gamyeon.question.application.port.in.GetQuestionSetResult;
import com.gamyeon.question.application.port.in.GetQuestionSetUseCase;
import com.gamyeon.question.application.port.in.RequestCustomQuestionUseCase;
import com.gamyeon.question.domain.QuestionSuccessCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class QuestionSetsController {

  private final RequestCustomQuestionUseCase requestCustomQuestionUseCase;
  private final GetQuestionSetUseCase getQuestionSetUseCase;

  @PostMapping("/intvs/{intvId}/questions")
  public ResponseEntity<ApiResponse<Void>> createQuestionSet(
      @AuthenticationPrincipal Long userId, @PathVariable Long intvId) {
    log.info("Received create question set request. userId={}, intvId={}", userId, intvId);

    requestCustomQuestionUseCase.generate(userId, intvId);

    return ApiResponse.success(QuestionSuccessCode.REQUEST_SUCCESS);
  }

  @GetMapping("/intvs/{intvId}/questions")
  public ResponseEntity<ApiResponse<QuestionSetResponse>> getQuestionSet(
      @AuthenticationPrincipal Long userId, @PathVariable Long intvId) {
    log.info("Received get question set request. userId={}, intvId={}", userId, intvId);
    GetQuestionSetResult result = getQuestionSetUseCase.getQuestionSets(userId, intvId);

    QuestionSetResponse response = QuestionSetResponse.from(result);

    return ApiResponse.success(QuestionSuccessCode.GET_SUCCESS, response);
  }
}
