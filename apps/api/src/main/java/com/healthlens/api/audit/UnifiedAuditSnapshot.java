package com.healthlens.api.audit;

import java.util.UUID;

/**
 * Optional thread-local holder for unified audit payloads. Prefer
 * {@link UnifiedAuditCoordinator#persistAndClear} in service methods so cleanup does not depend on
 * AOP-based cleanup (removed); always clear in a {@code finally} block in the calling service.
 */
public final class UnifiedAuditSnapshot {

    public record Payload(UUID resourceId, String oldValueJson, String newValueJson) {}

    private static final ThreadLocal<Payload> HOLDER = new ThreadLocal<>();

    private UnifiedAuditSnapshot() {}

    /**
     * @deprecated Prefer building a {@link Payload} and calling
     *     {@link UnifiedAuditCoordinator#persistAndClear}. If used, pair with {@link #clear()} in
     *     {@code finally}.
     */
    @Deprecated
    public static void set(Payload payload) {
        HOLDER.set(payload);
    }

    /** Removes any payload for the current thread; safe to call when none was set. */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * Returns and clears the payload for the current thread.
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
