package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecordShare;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRecordShareRepository extends JpaRepository<HealthRecordShare, UUID> {
    boolean existsByHealthRecordIdAndViewerIdAndRevokedAtIsNull(UUID healthRecordId, UUID viewerId);
    Optional<HealthRecordShare> findByHealthRecordIdAndViewerIdAndRevokedAtIsNull(UUID healthRecordId, UUID viewerId);
    List<HealthRecordShare> findAllByHealthRecordIdAndRevokedAtIsNull(UUID healthRecordId);
    List<HealthRecordShare> findAllByViewerIdAndRevokedAtIsNull(UUID viewerId);
    List<HealthRecordShare> findAllByViewerIdAndProfileIdAndRevokedAtIsNull(UUID viewerId, UUID profileId);
}
