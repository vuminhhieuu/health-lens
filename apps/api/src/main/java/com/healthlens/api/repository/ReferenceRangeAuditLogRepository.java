package com.healthlens.api.repository;

import com.healthlens.api.entity.ReferenceRangeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReferenceRangeAuditLogRepository extends JpaRepository<ReferenceRangeAuditLog, UUID> {
}
