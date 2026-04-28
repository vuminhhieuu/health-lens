package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    Optional<HealthRecord> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    Optional<HealthRecord> findByIdAndDeletedAtIsNull(UUID id);
    java.util.List<HealthRecord> findAllByProfileIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID profileId, UUID userId);
    Page<HealthRecord> findAllByProfileIdAndUserIdAndDeletedAtIsNull(UUID profileId, UUID userId, Pageable pageable);
    java.util.List<HealthRecord> findAllByDeletedAtBefore(Instant threshold);
    Page<HealthRecord> findAllByDeletedAtBefore(Instant threshold, Pageable pageable);
}
