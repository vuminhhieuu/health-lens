package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "rag_corpus_versions")
public class RagCorpusVersion {

    @Id
    @Column(name = "source_version", nullable = false, length = 100)
    private String sourceVersion;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "reviewer", length = 100)
    private String reviewer;

    @Column(name = "effective_date")
    private Instant effectiveDate;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @Column(name = "embedding_dimension", nullable = false)
    private int embeddingDimension;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rolled_back_at")
    private Instant rolledBackAt;

    @Column(name = "rolled_back_by", length = 100)
    private String rolledBackBy;

    @Column(name = "rollback_reason", columnDefinition = "text")
    private String rollbackReason;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
