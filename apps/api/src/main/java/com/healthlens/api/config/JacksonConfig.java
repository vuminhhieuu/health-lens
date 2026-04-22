package com.healthlens.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code spring-boot-starter-webmvc} không tự đăng ký {@link ObjectMapper} như
 * {@code spring-boot-starter-web}/{@code spring-boot-starter-json}.
 * Các service (upload reservation, OCR payload) cần inject {@link ObjectMapper}.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
