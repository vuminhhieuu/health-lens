package com.healthlens.api.audit;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRedactorTest {

    @Test
    void removesSensitiveKeysAndQueryParamsRecursively() {
        Map<String, ?> redacted = AuditRedactor.redact(Map.of(
                "email", "user@example.com",
                "token", "raw-token",
                "authorization", "Bearer abc",
                "ocrText", "raw ocr text",
                "downloadUrl", "https://storage.local/file.pdf?token=secret&email=user@example.com&expires=123",
                "nested", Map.of("presignedUrl", "https://storage.local/a?X-Amz-Signature=secret&safe=1")
        ));

        assertThat(redacted).doesNotContainKeys("token", "authorization", "ocrText");
        assertThat(redacted.get("downloadUrl").toString()).doesNotContain("token=", "email=");
        assertThat(redacted.get("downloadUrl").toString()).contains("expires=123");
        assertThat(redacted.get("nested").toString()).doesNotContain("X-Amz-Signature");
    }
}
