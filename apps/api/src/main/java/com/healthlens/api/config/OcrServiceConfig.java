package com.healthlens.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * OCR Service Configuration
 *
 * <p>Cấu hình RestTemplate chuyên dụng cho OCR microservice (EasyOCR).
 * Timeout được set riêng để handle các request OCR chậm (3-8s).
 *
 * <p>Config values:
 * <ul>
 *   <li>{@code app.ocr.service.url} — URL của EasyOCR service (default: http://localhost:8001)</li>
 *   <li>{@code app.ocr.service.timeout-ms} — Read timeout (default: 10000ms)</li>
 *   <li>{@code app.ocr.service.connect-timeout-ms} — Connect timeout (default: 5000ms)</li>
 * </ul>
 *
 * @see com.healthlens.api.service.OcrService
 */
@Configuration
public class OcrServiceConfig {

    @Value("${app.ocr.service.timeout-ms:10000}")
    private int ocrReadTimeoutMs;

    @Value("${app.ocr.service.connect-timeout-ms:5000}")
    private int ocrConnectTimeoutMs;

    /**
     * RestTemplate bean chuyên dụng cho OCR service.
     *
     * <p>Có connect timeout 5s và read timeout 10s, phù hợp với
     * thời gian xử lý OCR (3-8s trên CPU).
     *
     * @param builder Spring Boot auto-configured builder
     * @return RestTemplate với timeout settings cho OCR
     */
    @Bean("ocrRestTemplate")
    public RestTemplate ocrRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(ocrConnectTimeoutMs))
                .readTimeout(Duration.ofMillis(ocrReadTimeoutMs))
                .build();
    }
}
