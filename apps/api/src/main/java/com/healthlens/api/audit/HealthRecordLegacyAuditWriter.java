package com.healthlens.api.audit;

import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.HealthRecordAuditLog;
import com.healthlens.api.repository.HealthRecordAuditLogRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Writes rows to the legacy {@code health_record_audit_logs} table. Call explicitly from service
 * methods instead of relying on {@link com.healthlens.api.annotation.Auditable} AOP and inferred
 * argument positions.
 */
@Service
@RequiredArgsConstructor
public class HealthRecordLegacyAuditWriter {

    private final HealthRecordAuditLogRepository healthRecordAuditLogRepository;

    public void record(UUID userId, String action, UUID recordId) {
        HealthRecordAuditLog auditLog = new HealthRecordAuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setRecordId(recordId);
        healthRecordAuditLogRepository.save(auditLog);
    }

    public void recordPdfDownload(UUID actorId, HealthRecord record, String shareScope) {
        HealthRecordAuditLog auditLog = new HealthRecordAuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setUserId(actorId);
        auditLog.setAction(AuditActions.DOWNLOAD_HEALTH_RECORD_PDF);
        auditLog.setRecordId(record.getId());
        auditLog.setProfileId(record.getProfileId());
        auditLog.setViewerId(actorId);
        auditLog.setShareScope(shareScope);
        auditLog.setResourceType(AuditResourceTypes.HEALTH_RECORD);
        healthRecordAuditLogRepository.save(auditLog);
    }
}
