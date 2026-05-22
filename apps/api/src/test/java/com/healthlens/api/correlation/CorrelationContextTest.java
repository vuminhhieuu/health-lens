package com.healthlens.api.correlation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationContextTest {

    @AfterEach
    void tearDown() {
        CorrelationContext.clear();
    }

    @Test
    void resolveCorrelationIdForJob_usesNormalizedInboundContext() {
        CorrelationContext.ensure("  inbound-correlation  ", "req-1", "trace-1");

        assertThat(CorrelationContext.resolveCorrelationIdForJob("job-fallback"))
                .isEqualTo("inbound-correlation");
    }

    @Test
    void resolveCorrelationIdForJob_fallsBackToJobIdWhenContextMissing() {
        assertThat(CorrelationContext.resolveCorrelationIdForJob("job-only"))
                .isEqualTo("job-only");
    }

    @Test
    void resolveCorrelationIdForJob_requiresJobId() {
        assertThatThrownBy(() -> CorrelationContext.resolveCorrelationIdForJob(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveCorrelationIdForJob_rejectsBlankJobIdWhenContextMissing() {
        assertThatThrownBy(() -> CorrelationContext.resolveCorrelationIdForJob("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void resolveCorrelationIdForJob_trimsJobIdFallback() {
        assertThat(CorrelationContext.resolveCorrelationIdForJob("  job-trimmed  "))
                .isEqualTo("job-trimmed");
    }
}
