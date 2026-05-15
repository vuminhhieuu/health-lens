package com.healthlens.api.aspect;

import com.healthlens.api.annotation.Auditable;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.UnifiedAuditLogWriter;
import com.healthlens.api.audit.UnifiedAuditSnapshot;
import com.healthlens.api.entity.HealthRecordAuditLog;
import com.healthlens.api.repository.HealthRecordAuditLogRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditableAspect {

    private final HealthRecordAuditLogRepository healthRecordAuditLogRepository;
    private final UnifiedAuditLogWriter unifiedAuditLogWriter;

    @AfterReturning(value = "@annotation(auditable)")
    public void writeAuditLog(JoinPoint joinPoint, Auditable auditable) {
        String unifiedResourceType = auditable.unifiedResourceType();
        if (unifiedResourceType != null && !unifiedResourceType.isBlank()) {
            persistUnified(joinPoint.getSignature().toShortString(), auditable, unifiedResourceType);
            if (AuditActions.DELETE_HEALTH_RECORD.equals(auditable.action())) {
                persistHealthRecordAudit(joinPoint, auditable);
            }
            return;
        }

        persistHealthRecordAudit(joinPoint, auditable);
    }

    private void persistUnified(String shortSig, Auditable auditable, String resourceType) {
        UnifiedAuditSnapshot.Payload snapshot = UnifiedAuditSnapshot.take();
        if (snapshot == null) {
            log.warn(
                    "Skip unified audit {} for {}: no UnifiedAuditSnapshot provided",
                    resourceType,
                    shortSig
            );
            return;
        }

        unifiedAuditLogWriter.record(
                null,
                auditable.action(),
                resourceType,
                snapshot.resourceId(),
                snapshot.oldValueJson(),
                snapshot.newValueJson()
        );
    }

    private void persistHealthRecordAudit(JoinPoint joinPoint, Auditable auditable) {
        Object[] args = joinPoint.getArgs();
        if (args.length < 2 || !(args[0] instanceof UUID userId) || !(args[1] instanceof UUID recordId)) {
            log.warn(
                    "Skip health audit log for {} due to unexpected method signature",
                    joinPoint.getSignature().toShortString()
            );
            return;
        }

        HealthRecordAuditLog auditLog = new HealthRecordAuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setUserId(userId);
        auditLog.setAction(auditable.action());
        auditLog.setRecordId(recordId);
        healthRecordAuditLogRepository.save(auditLog);
    }
}
