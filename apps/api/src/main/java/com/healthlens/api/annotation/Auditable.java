package com.healthlens.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {
    String action();

    /**
     * When non-blank, a row is also written to {@code audit_logs} after the method succeeds.
     * Callers populate JSON snapshots via {@link com.healthlens.api.audit.UnifiedAuditSnapshot}.
     */
    String unifiedResourceType() default "";
}
