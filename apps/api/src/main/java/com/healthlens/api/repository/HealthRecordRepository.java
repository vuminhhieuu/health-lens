package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    Optional<HealthRecord> findByIdAndUserId(UUID id, UUID userId);
    java.util.List<HealthRecord> findAllByProfileIdAndUserIdOrderByCreatedAtDesc(UUID profileId, UUID userId);
    Page<HealthRecord> findAllByProfileIdAndUserId(UUID profileId, UUID userId, Pageable pageable);
}
