package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    Optional<HealthRecord> findByIdAndUserId(UUID id, UUID userId);

    List<HealthRecord> findAllByProfileIdAndUserIdOrderByCreatedAtDesc(UUID profileId, UUID userId);

    List<HealthRecord> findAllByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM HealthRecord h WHERE h.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
