package com.healthlens.api.audit;

import java.util.UUID;

/**
 * Thread-local payload consumed by {@link com.healthlens.api.aspect.AuditableAspect} when
 * {@link com.healthlens.api.annotation.Auditable#unifiedResourceType()} is set.
 */
public final class UnifiedAuditSnapshot {

    public record Payload(UUID resourceId, String oldValueJson, String newValueJson) {}

    private static final ThreadLocal<Payload> HOLDER = new ThreadLocal<>();

    private UnifiedAuditSnapshot() {}

    /** Must be invoked on the thread that runs the annotated method before it returns successfully. */
    public static void set(Payload payload) {
        HOLDER.set(payload);
    }

    /**
     * Called by AuditableAspect; clears the thread-local.
     *
     * @return captured payload or null if none set
     */
    public static Payload take() {
        try {
            return HOLDER.get();
        } finally {
            HOLDER.remove();
        }
    }
}
