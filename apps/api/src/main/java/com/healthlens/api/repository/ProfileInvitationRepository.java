package com.healthlens.api.repository;

import com.healthlens.api.entity.ProfileInvitation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileInvitationRepository extends JpaRepository<ProfileInvitation, UUID> {
    Optional<ProfileInvitation> findByToken(String token);

    /** Serialize accept / lifecycle updates for the same invitation token (Story 3.2). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ProfileInvitation i WHERE i.token = :token")
    Optional<ProfileInvitation> findByTokenForUpdate(@Param("token") String token);
    List<ProfileInvitation> findAllByProfileIdOrderByCreatedAtDesc(UUID profileId);
    
    List<ProfileInvitation> findAllByInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String inviteeEmail, String status);
    Optional<ProfileInvitation> findByProfileIdAndInviteeEmailIgnoreCaseAndStatus(UUID profileId, String inviteeEmail, String status);
    Optional<ProfileInvitation> findTopByProfileIdAndInviteeEmailIgnoreCaseOrderByCreatedAtDesc(UUID profileId, String inviteeEmail);

    Optional<ProfileInvitation> findByIdAndProfileId(UUID id, UUID profileId);
    void deleteAllByProfileIdAndInviteeEmailIgnoreCase(UUID profileId, String inviteeEmail);

    @Modifying
    int deleteAllByInviteeEmailIgnoreCase(String inviteeEmail);

    @Modifying
    @Query("""
            DELETE FROM ProfileInvitation i
            WHERE i.inviterId = :userId
               OR i.profileId IN (
                   SELECT p.id FROM Profile p WHERE p.user.id = :userId
               )
            """)
    int deleteAllByUserParticipation(@Param("userId") UUID userId);
}
