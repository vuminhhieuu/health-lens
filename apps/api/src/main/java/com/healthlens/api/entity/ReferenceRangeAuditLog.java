package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "reference_range_audit_logs")
public class ReferenceRangeAuditLog {

    @Id
    private UUID id;

    @Column(name = "metric_id", nullable = false)
    private UUID metricId;

    @Column(name = "reference_range_id", nullable = false)
    private UUID referenceRangeId;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (appliedAt == null) {
            appliedAt = Instant.now();
        }
    }
}
