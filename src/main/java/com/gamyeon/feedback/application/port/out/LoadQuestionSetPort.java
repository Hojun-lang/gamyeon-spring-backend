package com.gamyeon.feedback.application.port.out;

import java.util.Optional;

public interface LoadQuestionSetPort {

  Optional<Long> findIntvIdById(Long id);
}
