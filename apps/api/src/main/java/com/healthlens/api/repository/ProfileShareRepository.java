package com.healthlens.api.repository;

import com.healthlens.api.entity.ProfileShare;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileShareRepository extends JpaRepository<ProfileShare, UUID> {
    boolean existsByProfileIdAndViewerIdAndRevokedAtIsNull(UUID profileId, UUID viewerId);
}
