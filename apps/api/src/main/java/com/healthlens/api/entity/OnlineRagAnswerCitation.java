package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "online_rag_answer_citations")
public class OnlineRagAnswerCitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "health_record_id", nullable = false)
    private UUID healthRecordId;

    @Column(name = "metric_name", nullable = false, length = 255)
    private String metricName;

    @Column(name = "answer_hash", nullable = false, length = 64)
    private String answerHash;

    @Column(name = "source_snapshot_id")
    private UUID sourceSnapshotId;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "publisher", nullable = false, length = 255)
    private String publisher;

    @Column(name = "retrieved_at")
    private Instant retrievedAt;

    @Column(name = "snapshot_hash", nullable = false, length = 64)
    private String snapshotHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private OnlineRagReviewStatus reviewStatus;

    @Column(name = "excluded", nullable = false)
    private boolean excluded;

    @Column(name = "cache_hit", nullable = false)
    private boolean cacheHit;

    @Column(name = "usable_for_ai", nullable = false)
    private boolean usableForAi;

    @Column(name = "review_required", nullable = false)
    private boolean reviewRequired;

    @Column(name = "rejected", nullable = false)
    private boolean rejected;

    @Column(name = "retrieval_source", nullable = false, length = 80)
    private String retrievalSource;

    @Column(name = "prompt_version", nullable = false, length = 120)
    private String promptVersion;

    @Column(name = "model_version", nullable = false, length = 120)
    private String modelVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (retrievalSource == null || retrievalSource.isBlank()) {
            retrievalSource = "unknown";
        }
        if (promptVersion == null || promptVersion.isBlank()) {
            promptVersion = "unknown";
        }
        if (modelVersion == null || modelVersion.isBlank()) {
            modelVersion = "unknown";
        }
    }
}
