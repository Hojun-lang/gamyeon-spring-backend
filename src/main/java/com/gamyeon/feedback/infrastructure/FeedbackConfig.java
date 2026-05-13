package com.gamyeon.feedback.infrastructure;

import com.gamyeon.feedback.application.FeedbackCallbackProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FeedbackCallbackProperties.class)
public class FeedbackConfig {}
