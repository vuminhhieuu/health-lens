package com.healthlens.api.activity;

import java.util.Locale;

/**
 * Shared terminal OCR failure reason codes for health records and product analytics events.
 */
public final class FailureReasonNormalizer {

    private FailureReasonNormalizer() {
    }

    public static String normalizeForStorage(String reason) {
        if (reason == null || reason.isBlank()) {
            return "api_error";
        }
        String normalized = reason.trim().toLowerCase(Locale.ROOT);
        if ("processing_error".equals(normalized)) {
            return "api_error";
        }
        if ("timeout".equals(normalized)
                || "low_confidence".equals(normalized)
                || "api_error".equals(normalized)
                || "invalid_file".equals(normalized)) {
            return normalized;
        }
        return "api_error";
    }
}
