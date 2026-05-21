package com.healthlens.api.audit;

import java.util.Locale;

/**
 * Masks PII before it is written to audit payloads. Full emails and raw tokens must not appear in logs.
 */
public final class AuditPiiMasker {

    private AuditPiiMasker() {}

    /**
     * Masks an email for audit storage, e.g. {@code viewer@healthlens.vn} → {@code v***@healthlens.vn}.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at + 1);
        if (domain.isBlank()) {
            return "***";
        }
        char first = local.charAt(0);
        return first + "***@" + domain;
    }
}
