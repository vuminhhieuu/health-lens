package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "profile_shares")
public class ProfileShare {

    @Id
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "viewer_id", nullable = false)
    private UUID viewerId;

    @Column(name = "access_level", nullable = false, length = 20)
    private String accessLevel;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (grantedAt == null) {
            grantedAt = Instant.now();
        }
        if (accessLevel == null || accessLevel.isBlank()) {
            accessLevel = "view";
        }
    }
}
