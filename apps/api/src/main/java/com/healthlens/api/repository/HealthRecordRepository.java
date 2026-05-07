package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    Optional<HealthRecord> findByIdAndUserId(UUID id, UUID userId);
    Optional<HealthRecord> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    Optional<HealthRecord> findByIdAndDeletedAtIsNull(UUID id);
    java.util.List<HealthRecord> findAllByProfileIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID profileId, UUID userId);
    Page<HealthRecord> findAllByProfileIdAndUserIdAndDeletedAtIsNull(UUID profileId, UUID userId, Pageable pageable);
    java.util.List<HealthRecord> findAllByDeletedAtBefore(Instant threshold);
    Page<HealthRecord> findAllByDeletedAtBefore(Instant threshold, Pageable pageable);
    Page<HealthRecord> findAllByProfileIdAndUserId(UUID profileId, UUID userId, Pageable pageable);

    java.util.List<HealthRecord> findAllByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM HealthRecord h WHERE h.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
