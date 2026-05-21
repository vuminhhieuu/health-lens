package com.healthlens.api.repository;

import com.healthlens.api.entity.ProfileShare;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileShareRepository extends JpaRepository<ProfileShare, UUID> {
    boolean existsByProfileIdAndViewerIdAndRevokedAtIsNull(UUID profileId, UUID viewerId);
    boolean existsByProfileIdAndViewerIdAndAccessLevelIgnoreCaseAndRevokedAtIsNull(
            UUID profileId, UUID viewerId, String accessLevel);
    Optional<ProfileShare> findByProfileIdAndViewerIdAndRevokedAtIsNull(UUID profileId, UUID viewerId);

    /** Serialize revoke vs accept for the same active (profile, viewer) share row (Story 3.2). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM ProfileShare s
            WHERE s.profileId = :profileId AND s.viewerId = :viewerId AND s.revokedAt IS NULL
            """)
    Optional<ProfileShare> findByProfileIdAndViewerIdAndRevokedAtIsNullForUpdate(
            @Param("profileId") UUID profileId,
            @Param("viewerId") UUID viewerId);
    List<ProfileShare> findAllByProfileIdAndRevokedAtIsNull(UUID profileId);
    List<ProfileShare> findAllByViewerIdAndRevokedAtIsNull(UUID viewerId);

    @Modifying
    @Query("""
            DELETE FROM ProfileShare s
            WHERE s.ownerId = :userId OR s.viewerId = :userId
            """)
    int deleteAllByUserParticipation(@Param("userId") UUID userId);
}
