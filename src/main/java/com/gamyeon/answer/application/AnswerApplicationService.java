package com.gamyeon.answer.application;

import com.gamyeon.answer.application.port.in.HandleAnswerSttCallbackCommand;
import com.gamyeon.answer.application.port.in.HandleAnswerSttCallbackUseCase;
import com.gamyeon.answer.application.port.in.IssueAnswerUploadUrlCommand;
import com.gamyeon.answer.application.port.in.IssueAnswerUploadUrlResult;
import com.gamyeon.answer.application.port.in.IssueAnswerUploadUrlUseCase;
import com.gamyeon.answer.application.port.in.RegisterAnswerCommand;
import com.gamyeon.answer.application.port.in.RegisterAnswerResult;
import com.gamyeon.answer.application.port.in.RegisterAnswerUseCase;
import com.gamyeon.answer.application.port.in.RequestAnswerAnalysisCommand;
import com.gamyeon.answer.application.port.in.RequestAnswerAnalysisUseCase;
import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerAnalysisJob;
import com.gamyeon.answer.domain.AnswerAnalysisJobRepository;
import com.gamyeon.answer.domain.AnswerErrorCode;
import com.gamyeon.answer.domain.AnswerException;
import com.gamyeon.answer.domain.AnswerRepository;
import com.gamyeon.answer.domain.AnswerStatus;
import com.gamyeon.common.storage.application.StorageFileKeyGenerator;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlCommand;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlPort;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlResult;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AnswerApplicationService
    implements IssueAnswerUploadUrlUseCase,
        RegisterAnswerUseCase,
        RequestAnswerAnalysisUseCase,
        HandleAnswerSttCallbackUseCase {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".mp4", ".webm");
  private static final Set<String> ALLOWED_CONTENT_TYPE_PREFIXES =
      Set.of("video/mp4", "video/webm");

  private final AnswerRepository answerRepository;
  private final AnswerAnalysisJobRepository answerAnalysisJobRepository;
  private final StorageFileKeyGenerator storageFileKeyGenerator;
  private final StoragePresignedUrlPort storagePresignedUrlPort;
  private final AnswerAnalysisProperties answerAnalysisProperties;
  private final LoadQuestionSetPort loadQuestionSetPort;

  public AnswerApplicationService(
      AnswerRepository answerRepository,
      AnswerAnalysisJobRepository answerAnalysisJobRepository,
      StorageFileKeyGenerator storageFileKeyGenerator,
      StoragePresignedUrlPort storagePresignedUrlPort,
      AnswerAnalysisProperties answerAnalysisProperties,
      LoadQuestionSetPort loadQuestionSetPort) {
    this.answerRepository = answerRepository;
    this.answerAnalysisJobRepository = answerAnalysisJobRepository;
    this.storageFileKeyGenerator = storageFileKeyGenerator;
    this.storagePresignedUrlPort = storagePresignedUrlPort;
    this.answerAnalysisProperties = answerAnalysisProperties;
    this.loadQuestionSetPort = loadQuestionSetPort;
  }

  @Override
  @Transactional(readOnly = true)
  public IssueAnswerUploadUrlResult issueUploadUrl(IssueAnswerUploadUrlCommand command) {
    log.info(
        "Issuing answer upload URL. userId={}, questionSetId={}, originalFileName={}, contentType={}, fileSizeBytes={}",
        command.userId(),
        command.questionSetId(),
        command.originalFileName(),
        command.contentType(),
        command.fileSizeBytes());
    validateVideoFile(command.originalFileName(), command.contentType());
    validateFileSize(command.fileSizeBytes());

    String fileKey =
        storageFileKeyGenerator.generate(
            "answers",
            List.of(String.valueOf(command.questionSetId()), "video"),
            command.originalFileName());
    StoragePresignedUrlResult result =
        storagePresignedUrlPort.createUploadUrl(
            new StoragePresignedUrlCommand(fileKey, command.contentType()));

    return new IssueAnswerUploadUrlResult(
        command.questionSetId(),
        command.originalFileName(),
        result.fileKey(),
        result.presignedUrl(),
        result.fileUrl(),
        result.expiresInSeconds());
  }

  @Override
  public RegisterAnswerResult register(RegisterAnswerCommand command) {
    log.info(
        "Registering answer metadata. userId={}, intvId={}, questionSetId={}, originalFileName={}, fileKey={}",
        command.userId(),
        command.intvId(),
        command.questionSetId(),
        command.originalFileName(),
        command.fileKey());
    answerRepository
        .findByQuestionSetId(command.questionSetId())
        .ifPresent(
            answer -> {
              throw new AnswerException(AnswerErrorCode.ANSWER_ALREADY_EXISTS);
            });

    validateVideoFile(command.originalFileName(), command.contentType());
    validateFileSize(command.fileSizeBytes());

    Answer answer =
        Answer.create(
            command.intvId(),
            command.questionSetId(),
            command.originalFileName(),
            command.fileKey(),
            command.fileUrl(),
            command.contentType(),
            command.fileSizeBytes());

    Answer saved = answerRepository.save(answer);
    return new RegisterAnswerResult(saved.getId(), saved.getQuestionSetId());
  }

  @Override
  public void requestAnalysis(RequestAnswerAnalysisCommand command) {
    log.info(
        "Requesting STT analysis. userId={}, answerId={}", command.userId(), command.answerId());
    Answer answer =
        answerRepository
            .findById(command.answerId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    if (answer.getStatus() == AnswerStatus.STT_PROCESSING
        || answer.getStatus() == AnswerStatus.STT_PENDING
        || answerAnalysisJobRepository.existsActiveJobByAnswerId(answer.getId())) {
      throw new AnswerException(AnswerErrorCode.ANALYSIS_ALREADY_IN_PROGRESS);
    }
    if (answer.getStatus() == AnswerStatus.STT_COMPLETED) {
      throw new AnswerException(AnswerErrorCode.ANALYSIS_ALREADY_COMPLETED);
    }

    loadQuestionSetPort.getQuestionContent(answer.getQuestionSetId());

    String requestId = UUID.randomUUID().toString();
    AnswerAnalysisJob job =
        AnswerAnalysisJob.create(
            answer.getId(), requestId, answerAnalysisProperties.getMaxRetryCount());

    answer.markSttPending();
    answerRepository.save(answer);
    answerAnalysisJobRepository.save(job);
    log.info(
        "STT analysis request accepted. answerId={}, questionSetId={}, requestId={}",
        answer.getId(),
        answer.getQuestionSetId(),
        requestId);
  }

  @Override
  public void handle(HandleAnswerSttCallbackCommand command) {
    log.info(
        "Handling STT callback. requestId={}, intvId={}, questionSetId={}, hasError={}, hasPayload={}",
        command.requestId(),
        command.intvId(),
        command.questionSetId(),
        command.errorMessage() != null && !command.errorMessage().isBlank(),
        command.callbackPayload() != null);
    AnswerAnalysisJob job = loadCallbackTargetJob(command);
    if (job.isTerminal()) {
      log.info(
          "Ignoring duplicate STT callback for terminal job. requestId={}, answerId={}, jobStatus={}",
          job.getRequestId(),
          job.getAnswerId(),
          job.getStatus());
      return;
    }

    Answer answer =
        answerRepository
            .findById(job.getAnswerId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    if (command.errorMessage() != null && !command.errorMessage().isBlank()) {
      job.markFailed(command.errorMessage());
      answer.failStt(command.errorMessage(), command.callbackPayload());
      answerAnalysisJobRepository.save(job);
      answerRepository.save(answer);
      log.warn(
          "STT callback reported failure. requestId={}, answerId={}, questionSetId={}, errorMessage={}",
          job.getRequestId(),
          answer.getId(),
          answer.getQuestionSetId(),
          command.errorMessage());
      return;
    }

    job.complete();
    answer.completeStt(command.callbackPayload());
    answerAnalysisJobRepository.save(job);
    answerRepository.save(answer);
    log.info(
        "STT callback completed. requestId={}, answerId={}, questionSetId={}",
        job.getRequestId(),
        answer.getId(),
        answer.getQuestionSetId());
  }

  private AnswerAnalysisJob loadCallbackTargetJob(HandleAnswerSttCallbackCommand command) {
    if (command.requestId() != null && !command.requestId().isBlank()) {
      return answerAnalysisJobRepository
          .findByRequestId(command.requestId())
          .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));
    }

    Answer answer =
        answerRepository
            .findByQuestionSetId(command.questionSetId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    return answerAnalysisJobRepository
        .findLatestByAnswerId(answer.getId())
        .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));
  }

  private void validateVideoFile(String originalFileName, String contentType) {
    if (originalFileName == null || !hasAllowedExtension(originalFileName)) {
      throw new AnswerException(AnswerErrorCode.INVALID_FILE_EXTENSION);
    }

    if (!hasAllowedContentType(contentType)) {
      throw new AnswerException(AnswerErrorCode.INVALID_CONTENT_TYPE);
    }
  }

  private boolean hasAllowedExtension(String originalFileName) {
    String normalized = originalFileName.toLowerCase();
    return ALLOWED_EXTENSIONS.stream().anyMatch(normalized::endsWith);
  }

  private boolean hasAllowedContentType(String contentType) {
    if (contentType == null) {
      return false;
    }

    String normalized = contentType.toLowerCase();
    return ALLOWED_CONTENT_TYPE_PREFIXES.stream().anyMatch(normalized::startsWith);
  }

  private void validateFileSize(Long fileSizeBytes) {
    if (fileSizeBytes == null || fileSizeBytes < 1) {
      throw new AnswerException(AnswerErrorCode.INVALID_FILE_SIZE);
    }
  }
}
