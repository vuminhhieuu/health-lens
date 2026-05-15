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
@Table(name = "health_record_audit_logs")
public class HealthRecordAuditLog {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "viewer_id")
    private UUID viewerId;

    @Column(name = "share_scope", length = 20)
    private String shareScope;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
