package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ocr_dead_letters")
public class OcrDeadLetter {

    @Id
    private UUID id;

    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "job_id", length = 120)
    private String jobId;

    @Column(name = "idempotency_key", length = 800)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sanitized_payload", columnDefinition = "jsonb", nullable = false)
    private String sanitizedPayload;

    @Column(name = "failure_category", nullable = false, length = 120)
    private String failureCategory;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
