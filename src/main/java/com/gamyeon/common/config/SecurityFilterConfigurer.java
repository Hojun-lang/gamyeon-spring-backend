package com.gamyeon.common.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface SecurityFilterConfigurer {

  void configure(HttpSecurity http) throws Exception;
}
