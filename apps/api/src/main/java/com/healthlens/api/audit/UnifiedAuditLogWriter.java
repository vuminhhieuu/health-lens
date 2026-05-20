package com.healthlens.api.audit;

import com.healthlens.api.entity.AuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.correlation.CorrelationContext;
import com.healthlens.api.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedAuditLogWriter {

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    /**
     * Persist an audit row for an authenticated actor. Prefer passing a non-null {@code actorId} from
     * the service layer; null falls back to {@link SecurityContextHolder} for legacy call sites only.
     */
    public void record(
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            String oldValueJson,
            String newValueJson
    ) {
        record(actorId, action, resourceType, resourceId, oldValueJson, newValueJson, null);
    }

    public void record(
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            String oldValueJson,
            String newValueJson,
            String metadataJson
    ) {
        persist(actorId, action, resourceType, resourceId, oldValueJson, newValueJson, metadataJson);
    }

    /**
     * Persist an audit row in a separate transaction so failure audits survive caller rollback
     * (e.g. access denied followed by {@code AccessDeniedException} in the same {@code @Transactional} method).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequiresNew(
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            String oldValueJson,
            String newValueJson,
            String metadataJson
    ) {
        persist(actorId, action, resourceType, resourceId, oldValueJson, newValueJson, metadataJson);
    }

    private void persist(
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            String oldValueJson,
            String newValueJson,
            String metadataJson
    ) {
        UUID resolvedActorId = actorId != null ? actorId : resolveCurrentActorId();
        if (resolvedActorId == null) {
            log.warn(
                    "Skip unified audit action={} resourceType={}: pass actorId explicitly or use recordWithoutActor",
                    action,
                    resourceType
            );
            return;
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(entityManager.getReference(User.class, resolvedActorId));
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setOldValueJson(oldValueJson);
        auditLog.setNewValueJson(newValueJson);
        applyCanonicalFields(auditLog, action, metadataJson);

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            auditLog.setIpAddress(attrs.getRequest().getRemoteAddr());
            auditLog.setUserAgent(attrs.getRequest().getHeader("User-Agent"));
        }

        auditLogRepository.save(auditLog);
    }

    /**
     * Persist an audit row with no actor (e.g. failed login before authentication).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordWithoutActor(
            String action,
            String resourceType,
            UUID resourceId,
            String oldValueJson,
            String newValueJson
    ) {
        recordWithoutActor(action, resourceType, resourceId, oldValueJson, newValueJson, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordWithoutActor(
            String action,
            String resourceType,
            UUID resourceId,
            String oldValueJson,
            String newValueJson,
            String metadataJson
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setOldValueJson(oldValueJson);
        auditLog.setNewValueJson(newValueJson);
        applyCanonicalFields(auditLog, action, metadataJson);

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            auditLog.setIpAddress(attrs.getRequest().getRemoteAddr());
            auditLog.setUserAgent(attrs.getRequest().getHeader("User-Agent"));
        }

        auditLogRepository.save(auditLog);
    }

    private void applyCanonicalFields(AuditLog auditLog, String action, String metadataJson) {
        auditLog.setOutcome(AuditOutcome.fromAction(action));
        auditLog.setMetadataJson(metadataJson);
        auditLog.setCorrelationId(CorrelationContext.getCorrelationId());
        auditLog.setRequestId(CorrelationContext.getRequestId());
        auditLog.setTraceId(CorrelationContext.getTraceId());
    }

    private UUID resolveCurrentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || !auth.isAuthenticated()) {
            return null;
        }
        try {
            return UUID.fromString(auth.getPrincipal().toString());
        } catch (Exception e) {
            return null;
        }
    }
}
