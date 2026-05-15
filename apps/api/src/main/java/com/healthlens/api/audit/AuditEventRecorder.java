package com.healthlens.api.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Central helper for writing rows to {@code audit_logs} with JSON payloads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventRecorder {

    private final UnifiedAuditLogWriter unifiedAuditLogWriter;
    private final ObjectMapper objectMapper;

    public void record(
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue
    ) {
        unifiedAuditLogWriter.record(
                actorId,
                action,
                resourceType,
                resourceId,
                toJson(oldValue),
                toJson(newValue)
        );
    }

    public void recordEvent(
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, ?> details
    ) {
        record(actorId, action, resourceType, resourceId, null, details);
    }

    /** For pre-auth or failed-auth events where no authenticated actor exists yet. */
    public void recordAnonymous(
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, ?> details
    ) {
        unifiedAuditLogWriter.recordWithoutActor(
                action,
                resourceType,
                resourceId,
                null,
                toJson(details)
        );
    }

    private String toJson(Map<String, ?> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit payload for action={}", value, e);
            return null;
        }
    }
}
