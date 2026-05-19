package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    Optional<HealthRecord> findByIdAndUserId(UUID id, UUID userId);
    Optional<HealthRecord> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    Optional<HealthRecord> findByIdAndDeletedAtIsNull(UUID id);
    @Query(value = """
        SELECT DISTINCT ON (h.profile_id) h.*
        FROM health_records h
        WHERE h.profile_id IN (:profileIds)
          AND h.deleted_at IS NULL
        ORDER BY h.profile_id ASC, h.exam_date DESC NULLS LAST, h.created_at DESC
        """, nativeQuery = true)
    List<HealthRecord> findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
            @Param("profileIds") Collection<UUID> profileIds);
    List<HealthRecord> findAllByProfileIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID profileId, UUID userId);
    Page<HealthRecord> findAllByProfileIdAndUserIdAndDeletedAtIsNull(UUID profileId, UUID userId, Pageable pageable);
    List<HealthRecord> findAllByDeletedAtBefore(Instant threshold);
    Page<HealthRecord> findAllByDeletedAtBefore(Instant threshold, Pageable pageable);
    Page<HealthRecord> findAllByProfileIdAndUserId(UUID profileId, UUID userId, Pageable pageable);

    java.util.List<HealthRecord> findAllByUserId(UUID userId);

    @Query("SELECT h.fileKey FROM HealthRecord h WHERE h.userId = :userId")
    List<String> findFileKeysByUserId(@Param("userId") UUID userId);

    @Query("SELECT MAX(h.createdAt) FROM HealthRecord h WHERE h.profileId = :profileId AND h.deletedAt IS NULL")
    Optional<Instant> findMaxCreatedAtByProfileId(@Param("profileId") UUID profileId);

    @Modifying
    @Query("DELETE FROM HealthRecord h WHERE h.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
