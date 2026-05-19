package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecordInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthRecordInvitationRepository extends JpaRepository<HealthRecordInvitation, UUID> {
    Optional<HealthRecordInvitation> findByToken(String token);
    Optional<HealthRecordInvitation> findByIdAndHealthRecordId(UUID id, UUID healthRecordId);
    List<HealthRecordInvitation> findAllByInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(String inviteeEmail, String status);
    Optional<HealthRecordInvitation> findTopByHealthRecordIdAndInviteeEmailIgnoreCaseOrderByCreatedAtDesc(
            UUID healthRecordId, String inviteeEmail);
    List<HealthRecordInvitation> findAllByHealthRecordIdOrderByCreatedAtDesc(UUID healthRecordId);
    List<HealthRecordInvitation> findAllByHealthRecordIdAndInviteeEmailIgnoreCase(UUID healthRecordId, String inviteeEmail);
    void deleteAllByHealthRecordIdAndInviteeEmailIgnoreCase(UUID healthRecordId, String inviteeEmail);

    @Modifying
    int deleteAllByInviteeEmailIgnoreCase(String inviteeEmail);

    @Modifying
    @Query("""
            DELETE FROM HealthRecordInvitation i
            WHERE i.inviterId = :userId
               OR i.profileId IN (
                   SELECT p.id FROM Profile p WHERE p.user.id = :userId
               )
            """)
    int deleteAllByUserParticipation(@Param("userId") UUID userId);
}
