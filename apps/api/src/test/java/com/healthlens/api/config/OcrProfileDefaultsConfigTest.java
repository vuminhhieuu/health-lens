package com.healthlens.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OcrProfileDefaultsConfigTest {

    @Test
    @DisplayName("application.yml giữ default OCR routing cho dev/staging/production")
    void applicationYml_containsExpectedOcrProviderDefaultsPerProfile() throws Exception {
        String content = new ClassPathResource("application.yml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(content).contains("primary: ${OCR_PROVIDER_PRIMARY:easyocr}");
        assertThat(content).contains("on-profile: staging");
        assertThat(content).contains("primary: ${OCR_PROVIDER_PRIMARY:gcv}");
        assertThat(content).contains("on-profile: production");
        assertThat(content).contains("fallback-order: ${OCR_PROVIDER_FALLBACK_ORDER:}");
    }
}
