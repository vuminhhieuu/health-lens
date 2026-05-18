package com.healthlens.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated Use explicit audit calls instead of AOP:
 * <ul>
 *   <li>Unified {@code audit_logs}: {@link com.healthlens.api.audit.UnifiedAuditCoordinator}</li>
 *   <li>Legacy {@code health_record_audit_logs}: {@link com.healthlens.api.audit.HealthRecordLegacyAuditWriter}</li>
 * </ul>
 * The former {@link com.healthlens.api.aspect.AuditableAspect} inferred {@code (UUID userId, UUID recordId)}
 * from join-point args and silently skipped mismatched signatures.
 */
@Deprecated(forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {
    String action();

    @Deprecated(forRemoval = true)
    String unifiedResourceType() default "";
}
