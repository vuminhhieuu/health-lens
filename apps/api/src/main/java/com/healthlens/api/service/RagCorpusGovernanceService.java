package com.healthlens.api.service;

import com.healthlens.api.entity.RagCorpusVersion;
import com.healthlens.api.repository.RagCorpusVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RagCorpusGovernanceService {

    private static final String PENDING_REVIEW = "pending_review";
    private static final String APPROVED = "approved";
    private static final String ROLLED_BACK = "rolled_back";

    private final RagCorpusVersionRepository repository;
    private final Clock clock;

    @Autowired
    public RagCorpusGovernanceService(RagCorpusVersionRepository repository) {
        this(repository, Clock.systemUTC());
    }

    RagCorpusGovernanceService(RagCorpusVersionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void validateNewSourceVersion(String sourceVersion) {
        String normalized = normalizeSourceVersion(sourceVersion);
        if (repository.existsById(normalized)) {
            throw new IllegalArgumentException("RAG corpus sourceVersion already exists: " + normalized);
        }
    }

    @Transactional
    public CorpusVersionMetadata recordPendingIngestion(MetricExplanationIngestionService.IngestionReport report) {
        validateNewSourceVersion(report.sourceVersion());
        RagCorpusVersion version = new RagCorpusVersion();
        version.setSourceVersion(normalizeSourceVersion(report.sourceVersion()));
        version.setStatus(PENDING_REVIEW);
        version.setReviewer(report.reviewer());
        version.setEmbeddingModel(report.embeddingModel());
        version.setEmbeddingDimension(report.embeddingDimension());
        version.setChunkCount(report.chunkCount());
        version.setCreatedAt(report.effectiveDate() == null ? Instant.now(clock) : report.effectiveDate());
        return toMetadata(repository.save(version));
    }

    @Transactional
    public CorpusVersionMetadata approveVersion(String sourceVersion, String reviewer, Instant effectiveDate) {
        RagCorpusVersion version = repository.findById(normalizeSourceVersion(sourceVersion))
                .orElseThrow(() -> new IllegalArgumentException("RAG corpus sourceVersion not found: " + sourceVersion));
        if (ROLLED_BACK.equals(version.getStatus())) {
            throw new IllegalStateException("Cannot approve a rolled back RAG corpus version: " + sourceVersion);
        }
        Instant approvalTime = Instant.now(clock);
        version.setStatus(APPROVED);
        version.setReviewer(normalizeReviewer(reviewer));
        version.setApprovedAt(approvalTime);
        version.setEffectiveDate(effectiveDate == null ? approvalTime : effectiveDate);
        return toMetadata(repository.save(version));
    }

    @Transactional(readOnly = true)
    public Optional<String> activeApprovedVersion() {
        return repository.findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
                        APPROVED,
                        Instant.now(clock)
                )
                .map(RagCorpusVersion::getSourceVersion);
    }

    @Transactional
    public CorpusVersionMetadata rollbackToPreviousApprovedVersion(String problematicVersion, String reviewer) {
        String normalized = normalizeSourceVersion(problematicVersion);
        String active = activeApprovedVersion()
                .orElseThrow(() -> new IllegalStateException("No active approved RAG corpus version is available"));
        if (!active.equals(normalized)) {
            throw new IllegalArgumentException("Can only rollback the active RAG corpus version: " + active);
        }

        RagCorpusVersion current = repository.findById(normalized)
                .orElseThrow(() -> new IllegalArgumentException("RAG corpus sourceVersion not found: " + normalized));
        RagCorpusVersion previous = repository
                .findFirstByStatusAndSourceVersionNotAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
                        APPROVED,
                        normalized,
                        Instant.now(clock)
                )
                .orElseThrow(() -> new IllegalStateException("No previous approved RAG corpus version is available for rollback"));

        current.setStatus(ROLLED_BACK);
        current.setRolledBackAt(Instant.now(clock));
        current.setRolledBackBy(normalizeReviewer(reviewer));
        current.setRollbackReason("rollback_to_previous_approved_version");
        repository.save(current);
        return toMetadata(previous);
    }

    @Transactional(readOnly = true)
    public List<CorpusVersionMetadata> inspectMetadata() {
        return repository.findAll().stream()
                .map(this::toMetadata)
                .toList();
    }

    private CorpusVersionMetadata toMetadata(RagCorpusVersion version) {
        return new CorpusVersionMetadata(
                version.getSourceVersion(),
                version.getStatus(),
                version.getReviewer(),
                version.getEffectiveDate(),
                version.getEmbeddingModel(),
                version.getEmbeddingDimension(),
                version.getChunkCount(),
                version.getCreatedAt(),
                version.getRolledBackAt(),
                version.getRolledBackBy()
        );
    }

    private String normalizeSourceVersion(String sourceVersion) {
        if (sourceVersion == null || sourceVersion.isBlank()) {
            throw new IllegalArgumentException("RAG corpus sourceVersion is required");
        }
        return sourceVersion.trim();
    }

    private String normalizeReviewer(String reviewer) {
        return reviewer == null || reviewer.isBlank() ? "unknown-reviewer" : reviewer.trim();
    }

    public record CorpusVersionMetadata(
            String sourceVersion,
            String approvalStatus,
            String reviewer,
            Instant effectiveDate,
            String embeddingModel,
            int embeddingDimension,
            int chunkCount,
            Instant createdAt,
            Instant rolledBackAt,
            String rolledBackBy
    ) {
    }
}
