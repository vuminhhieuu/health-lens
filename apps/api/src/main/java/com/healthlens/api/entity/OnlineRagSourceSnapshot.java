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
@Table(name = "online_rag_source_snapshots")
public class OnlineRagSourceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "canonical_host", nullable = false, length = 255)
    private String canonicalHost;

    @Column(name = "publisher", nullable = false, length = 255)
    private String publisher;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;

    @Column(name = "snapshot_hash", nullable = false, length = 64)
    private String snapshotHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private OnlineRagReviewStatus reviewStatus;

    @Column(name = "excluded", nullable = false)
    private boolean excluded;

    @Column(name = "content_length", nullable = false)
    private int contentLength;

    @Column(name = "content_snapshot", nullable = false, columnDefinition = "text")
    private String contentSnapshot;

    @PrePersist
    public void prePersist() {
        if (retrievedAt == null) {
            retrievedAt = Instant.now();
        }
        if (reviewStatus == null) {
            reviewStatus = OnlineRagReviewStatus.REVIEW_REQUIRED;
        }
        excluded = reviewStatus != OnlineRagReviewStatus.APPROVED;
    }
}
