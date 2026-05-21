package com.healthlens.api.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditPiiMaskerTest {

    @Test
    void maskEmail_obscuresLocalPartAndKeepsDomain() {
        assertThat(AuditPiiMasker.maskEmail("Viewer@HealthLens.vn")).isEqualTo("v***@healthlens.vn");
    }

    @Test
    void maskEmail_blankReturnsNull() {
        assertThat(AuditPiiMasker.maskEmail("  ")).isNull();
    }
}
