package com.healthlens.api.audit;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AuditRedactor {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization",
            "authheader",
            "auth_header",
            "token",
            "invitationtoken",
            "invitationlink",
            "inviteeemail",
            "vieweremail",
            "owneremail",
            "rawtoken",
            "accesstoken",
            "refreshtoken",
            "password",
            "secret",
            "presignedurl",
            "presigned_url",
            "rawocrtext",
            "ocrtext",
            "rawtext",
            "text"
    );
    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "token",
            "email",
            "x-amz-signature",
            "x-amz-credential",
            "x-amz-security-token",
            "signature"
    );

    private AuditRedactor() {
    }

    public static Map<String, ?> redact(Map<String, ?> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> redacted = new LinkedHashMap<>();
        value.forEach((key, nestedValue) -> {
            if (key == null || nestedValue == null || isSensitiveKey(key)) {
                return;
            }
            redacted.put(key, redactValue(key, nestedValue));
        });
        return redacted;
    }

    @SuppressWarnings("unchecked")
    private static Object redactValue(String key, Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> typed = new LinkedHashMap<>();
            mapValue.forEach((nestedKey, nestedValue) -> {
                if (nestedKey != null && nestedValue != null) {
                    typed.put(nestedKey.toString(), nestedValue);
                }
            });
            return redact(typed);
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(item -> item instanceof Map<?, ?> ? redact((Map<String, ?>) item) : redactScalar(key, item))
                    .toList();
        }
        return redactScalar(key, value);
    }

    private static Object redactScalar(String key, Object value) {
        if (value == null) {
            return null;
        }
        String stringValue = value.toString();
        if (looksLikeUrl(key, stringValue)) {
            return stripSensitiveQueryParams(stringValue);
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = normalizeKey(key);
        return SENSITIVE_KEYS.contains(normalized);
    }

    private static boolean looksLikeUrl(String key, String value) {
        String normalized = normalizeKey(key);
        return normalized.endsWith("url") || value.startsWith("http://") || value.startsWith("https://");
    }

    private static String stripSensitiveQueryParams(String value) {
        try {
            URI uri = URI.create(value);
            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) {
                return value;
            }
            List<String> safeParams = List.of(query.split("&")).stream()
                    .filter(param -> {
                        int eq = param.indexOf('=');
                        String name = eq >= 0 ? param.substring(0, eq) : param;
                        return !SENSITIVE_QUERY_KEYS.contains(name.toLowerCase(Locale.ROOT));
                    })
                    .toList();
            String safeQuery = safeParams.isEmpty() ? null : String.join("&", safeParams);
            return new URI(uri.getScheme(), uri.getRawAuthority(), uri.getRawPath(), safeQuery, uri.getRawFragment())
                    .toString();
        } catch (RuntimeException | java.net.URISyntaxException ex) {
            return "[redacted-url]";
        }
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
