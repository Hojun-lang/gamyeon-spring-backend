package com.gamyeon.answer.adapter.in.web;

import com.gamyeon.answer.application.port.in.IssueAnswerUploadUrlResult;
import com.gamyeon.answer.application.port.in.IssueAnswerUploadUrlUseCase;
import com.gamyeon.answer.application.port.in.RegisterAnswerResult;
import com.gamyeon.answer.application.port.in.RegisterAnswerUseCase;
import com.gamyeon.answer.application.port.in.RequestAnswerAnalysisCommand;
import com.gamyeon.answer.application.port.in.RequestAnswerAnalysisUseCase;
import com.gamyeon.answer.domain.AnswerSuccessCode;
import com.gamyeon.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AnswerController {

  private final IssueAnswerUploadUrlUseCase issueAnswerUploadUrlUseCase;
  private final RegisterAnswerUseCase registerAnswerUseCase;
  private final RequestAnswerAnalysisUseCase requestAnswerAnalysisUseCase;

  @PostMapping("/api/v1/intvs/{questionSetId}/answers/presigned-url")
  public ResponseEntity<ApiResponse<AnswerUploadUrlResponse>> issueUploadUrl(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long questionSetId,
      @Valid @RequestBody IssueAnswerUploadUrlRequest request) {
    log.info(
        "Received issueUploadUrl request. userId={}, questionSetId={}, originalFileName={}, contentType={}, fileSizeBytes={}",
        userId,
        questionSetId,
        request.originalFileName(),
        request.contentType(),
        request.fileSizeBytes());

    IssueAnswerUploadUrlResult result =
        issueAnswerUploadUrlUseCase.issueUploadUrl(request.toCommand(userId, questionSetId));

    return ApiResponse.success(
        AnswerSuccessCode.ANSWER_UPLOAD_URL_ISSUED, AnswerUploadUrlResponse.from(result));
  }

  @PostMapping("/api/v1/intvs/{questionSetId}/answers")
  public ResponseEntity<ApiResponse<RegisterAnswerResponse>> registerAnswer(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long questionSetId,
      @Valid @RequestBody RegisterAnswerRequest request) {
    log.info(
        "Received registerAnswer request. userId={}, intvId={}, questionSetId={}, originalFileName={}, fileKey={}",
        userId,
        request.intvId(),
        questionSetId,
        request.originalFileName(),
        request.fileKey());

    RegisterAnswerResult result =
        registerAnswerUseCase.register(request.toCommand(userId, questionSetId));

    return ApiResponse.success(
        AnswerSuccessCode.ANSWER_REGISTERED, RegisterAnswerResponse.from(result));
  }

  @PostMapping("/api/v1/answers/{answerId}/analysis")
  public ResponseEntity<ApiResponse<Void>> requestAnalysis(
      @AuthenticationPrincipal Long userId, @PathVariable Long answerId) {
    log.info("Received requestAnalysis request. userId={}, answerId={}", userId, answerId);

    requestAnswerAnalysisUseCase.requestAnalysis(
        new RequestAnswerAnalysisCommand(userId, answerId));

    return ApiResponse.success(AnswerSuccessCode.ANSWER_ANALYSIS_REQUESTED);
  }
}
