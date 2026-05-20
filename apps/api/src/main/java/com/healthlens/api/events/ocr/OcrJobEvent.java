package com.healthlens.api.events.ocr;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record OcrJobEvent(
        String jobId,
        String correlationId,
        UUID recordId,
        String fileKey,
        String fileVersion,
        String mimeType,
        UUID profileId,
        Integer attempt
) {

    public static final String DEFAULT_FILE_VERSION = "unversioned";

    public Map<String, String> toStreamMap() {
        Map<String, String> payload = new LinkedHashMap<>();
        put(payload, "jobId", jobId);
        put(payload, "correlationId", correlationId);
        put(payload, "recordId", recordId == null ? null : recordId.toString());
        put(payload, "fileKey", fileKey);
        put(payload, "fileVersion", fileVersion == null || fileVersion.isBlank() ? DEFAULT_FILE_VERSION : fileVersion);
        put(payload, "mimeType", mimeType);
        put(payload, "profileId", profileId == null ? null : profileId.toString());
        if (attempt != null) {
            put(payload, "attempt", Integer.toString(attempt));
        }
        return payload;
    }

    private static void put(Map<String, String> payload, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        payload.put(key, value);
    }
}
