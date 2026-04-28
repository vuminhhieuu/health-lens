package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecordAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRecordAuditLogRepository extends JpaRepository<HealthRecordAuditLog, UUID> {
}
