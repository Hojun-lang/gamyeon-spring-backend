package com.gamyeon.feedback.application.port.out;

import com.gamyeon.feedback.domain.Feedback;

public interface SaveFeedbackPort {

  boolean existsByQuestionSetId(Long questionSetId);

  boolean saveIfAbsent(Feedback feedback);
}
