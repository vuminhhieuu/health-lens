package com.healthlens.api.repository;

import com.healthlens.api.entity.ProfileShareAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileShareAuditLogRepository extends JpaRepository<ProfileShareAuditLog, UUID> {
}