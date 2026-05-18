package com.healthlens.api.audit;

import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Writes unified {@code audit_logs} rows from service code with guaranteed {@link UnifiedAuditSnapshot}
 * cleanup. Prefer this over {@link UnifiedAuditSnapshot#set} on async or self-invoked code paths.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedAuditCoordinator {

    private final UnifiedAuditLogWriter unifiedAuditLogWriter;

    /**
     * Persists a unified audit row, then always clears the thread-local holder in {@code finally}.
     */
    public void persistAndClear(
            UUID actorId,
            String action,
            String resourceType,
            UnifiedAuditSnapshot.Payload payload
    ) {
        try {
            persist(actorId, action, resourceType, payload);
        } finally {
            UnifiedAuditSnapshot.clear();
        }
    }

    /**
     * @param actorId authenticated user/admin performing the action (required for attributable audit rows)
     */
    public void persist(
            UUID actorId,
            String action,
            String resourceType,
            UnifiedAuditSnapshot.Payload payload
    ) {
        Objects.requireNonNull(actorId, "actorId is required for unified audit");
        if (payload == null) {
            log.warn("Skip unified audit action={} resourceType={}: no payload", action, resourceType);
            return;
        }
        unifiedAuditLogWriter.record(
                actorId,
                action,
                resourceType,
                payload.resourceId(),
                payload.oldValueJson(),
                payload.newValueJson()
        );
    }
}
