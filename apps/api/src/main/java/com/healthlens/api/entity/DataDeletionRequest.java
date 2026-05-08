package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user's right-to-delete request per Nghị định 13/2023/NĐ-CP.
 * Implements 72-hour grace period with optional cancellation.
 */
@Getter
@Setter
@Entity
@Table(name = "data_deletion_requests")
public class DataDeletionRequest {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "scheduled_deletion_at", nullable = false)
    private Instant scheduledDeletionAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeletionRequestStatus status;

    @Column(name = "cancellation_token", nullable = false, unique = true, length = 255)
    private String cancellationToken;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = DeletionRequestStatus.PENDING;
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        // Schedule deletion 72 hours from now
        if (requestedAt != null && scheduledDeletionAt == null) {
            scheduledDeletionAt = requestedAt.plusSeconds(72 * 3600);
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isPending() {
        return status == DeletionRequestStatus.PENDING;
    }

    /**
     * Pending request whose scheduled deletion time is at or before now — no longer cancellable.
     * Uses inclusive deadline boundary ({@code now >= scheduledDeletionAt}) so cancellation matches
     * grace-period semantics (cannot cancel once the scheduled deletion instant is reached).
     */
    public boolean isOverdue() {
        if (!isPending()) {
            return false;
        }
        if (scheduledDeletionAt == null) {
            return true;
        }
        return !Instant.now().isBefore(scheduledDeletionAt);
    }

    public boolean canBeCancelled() {
        return isPending()
                && scheduledDeletionAt != null
                && Instant.now().isBefore(scheduledDeletionAt);
    }
}
