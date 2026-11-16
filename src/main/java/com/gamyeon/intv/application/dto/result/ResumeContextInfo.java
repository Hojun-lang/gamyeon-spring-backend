package com.gamyeon.intv.application.dto.result;

import com.gamyeon.intv.domain.IntvStatus;
import java.util.List;

public record ResumeContextInfo(
    Long intvId,
    IntvStatus status,
    int totalQuestionCount,
    int answeredCount,
    boolean completed,
    Long nextQuestionSetId,
    Integer nextQuestionOrder,
    String nextQuestionContent,
    List<QuestionProgressInfo> questions) {}
