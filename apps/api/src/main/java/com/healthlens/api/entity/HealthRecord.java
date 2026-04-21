package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "health_records")
public class HealthRecord {

    @Id
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ocr_result", columnDefinition = "jsonb")
    private String rawOcrResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metrics;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (sourceType == null) {
            sourceType = "ocr";
        }
        if (status == null) {
            status = "processing";
        }
        if (metrics == null) {
            metrics = "[]";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
