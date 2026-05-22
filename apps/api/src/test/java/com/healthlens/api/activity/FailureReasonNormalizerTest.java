package com.healthlens.api.activity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FailureReasonNormalizerTest {

    @Test
    void normalizeForStorage_mapsProcessingErrorToApiError() {
        assertThat(FailureReasonNormalizer.normalizeForStorage("processing_error")).isEqualTo("api_error");
    }

    @Test
    void normalizeForStorage_preservesKnownCodes() {
        assertThat(FailureReasonNormalizer.normalizeForStorage("timeout")).isEqualTo("timeout");
        assertThat(FailureReasonNormalizer.normalizeForStorage("low_confidence")).isEqualTo("low_confidence");
    }
}
