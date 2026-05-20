package com.healthlens.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryDto(
        UUID id,
        String actorEmail,
        String action,
        String resourceType,
        UUID resourceId,
        String entityLabel,
        String detailSummary,
        String outcome,
        String oldValueJson,
        String newValueJson,
        String metadataJson,
        String correlationId,
        String requestId,
        String traceId,
        String ipAddress,
        Instant createdAt
) {}
