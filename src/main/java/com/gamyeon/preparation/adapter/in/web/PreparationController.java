package com.gamyeon.preparation.adapter.in.web;

import com.gamyeon.common.response.ApiResponse;
import com.gamyeon.preparation.application.port.in.PreparationFileUseCase;
import com.gamyeon.preparation.application.port.in.PreparationFilesRegisterResult;
import com.gamyeon.preparation.application.port.in.UploadPreparationFileUrlResult;
import com.gamyeon.preparation.application.port.in.UploadPreparationFileUrlUseCase;
import com.gamyeon.preparation.domain.PreparationSuccessCode;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preparations")
@Slf4j
public class PreparationController {

  private final PreparationFileUseCase preparationFileUseCase;
  private final UploadPreparationFileUrlUseCase uploadPreparationFileUrlUseCase;

  public PreparationController(
      PreparationFileUseCase preparationFileUseCase,
      UploadPreparationFileUrlUseCase uploadPreparationFileUrlUseCase) {
    this.preparationFileUseCase = preparationFileUseCase;
    this.uploadPreparationFileUrlUseCase = uploadPreparationFileUrlUseCase;
  }

  @PostMapping("/{intvId}/files")
  public ResponseEntity<ApiResponse<PreparationFileRegisterResponse>> registerFile(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long intvId,
      @Valid @RequestBody PreparationFileRegisterRequest request) {
    log.info(
        "Received register preparation files request. userId={}, intvId={}, fileCount={}",
        userId,
        intvId,
        request.files() == null ? 0 : request.files().size());
    Long finalUserId = userId;
    PreparationFilesRegisterResult result =
        preparationFileUseCase.registerFiles(
            request.files().stream().map(file -> file.toCommand(finalUserId, intvId)).toList());

    return ApiResponse.success(
        PreparationSuccessCode.PREPARATION_FILE_REGISTERED,
        PreparationFileRegisterResponse.from(result));
  }

  @PostMapping("/{intvId}/files/presigned-url")
  public ResponseEntity<ApiResponse<PreparationResponse>> issueUploadUrl(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long intvId,
      @Valid @RequestBody PreparationRequest request) {
    log.info(
        "Received preparation upload URL request. userId={}, intvId={}, fileType={}, originalFileName={}, fileSizeBytes={}",
        userId,
        intvId,
        request.fileType(),
        request.originalFileName(),
        request.fileSizeBytes());
    UploadPreparationFileUrlResult result =
        uploadPreparationFileUrlUseCase.issueUploadUrl(request.toCommand(userId, intvId));

    return ApiResponse.success(
        PreparationSuccessCode.PREPARATION_UPLOAD_URL_ISSUED, PreparationResponse.from(result));
  }
}
