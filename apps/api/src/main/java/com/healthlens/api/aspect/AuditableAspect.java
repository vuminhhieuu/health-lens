package com.healthlens.api.aspect;

import com.healthlens.api.annotation.Auditable;
import com.healthlens.api.entity.HealthRecordAuditLog;
import com.healthlens.api.repository.HealthRecordAuditLogRepository;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuditableAspect {

    private final HealthRecordAuditLogRepository healthRecordAuditLogRepository;

    public AuditableAspect(HealthRecordAuditLogRepository healthRecordAuditLogRepository) {
        this.healthRecordAuditLogRepository = healthRecordAuditLogRepository;
    }

    @AfterReturning(value = "@annotation(auditable)")
    public void writeAuditLog(JoinPoint joinPoint, Auditable auditable) {
        Object[] args = joinPoint.getArgs();
        if (args.length < 2 || !(args[0] instanceof UUID userId) || !(args[1] instanceof UUID recordId)) {
            log.warn("Skip audit log for {} due to unexpected method signature", joinPoint.getSignature().toShortString());
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
