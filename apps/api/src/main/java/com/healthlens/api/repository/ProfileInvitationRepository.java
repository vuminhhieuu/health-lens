package com.healthlens.api.repository;

import com.healthlens.api.entity.ProfileInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileInvitationRepository extends JpaRepository<ProfileInvitation, UUID> {
    Optional<ProfileInvitation> findByToken(String token);
    List<ProfileInvitation> findAllByProfileIdOrderByCreatedAtDesc(UUID profileId);
    
    List<ProfileInvitation> findAllByInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String inviteeEmail, String status);
    Optional<ProfileInvitation> findByProfileIdAndInviteeEmailIgnoreCaseAndStatus(UUID profileId, String inviteeEmail, String status);
    Optional<ProfileInvitation> findTopByProfileIdAndInviteeEmailIgnoreCaseOrderByCreatedAtDesc(UUID profileId, String inviteeEmail);

    Optional<ProfileInvitation> findByIdAndProfileId(UUID id, UUID profileId);
    void deleteAllByProfileIdAndInviteeEmailIgnoreCase(UUID profileId, String inviteeEmail);
}
