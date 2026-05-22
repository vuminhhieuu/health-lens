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
@Table(name = "user_activity_events")
public class UserActivityEvent {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "is_retry", nullable = false)
    private boolean retry;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "file_type", length = 20)
    private String fileType;

    @Column(length = 50)
    private String provider;

    private Double confidence;

    @Column(name = "has_low_confidence_metrics")
    private Boolean hasLowConfidenceMetrics;

    @Column(name = "failure_reason", length = 50)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
