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
        String oldValueJson,
        String newValueJson,
        String ipAddress,
        Instant createdAt
) {}
