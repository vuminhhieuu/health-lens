package com.healthlens.api.service;

import com.healthlens.api.entity.RagCorpusVersion;
import com.healthlens.api.repository.RagCorpusVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagCorpusGovernanceServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-19T03:00:00Z");

    private RagCorpusVersionRepository repository;
    private RagCorpusGovernanceService service;

    @BeforeEach
    void setUp() {
        repository = mock(RagCorpusVersionRepository.class);
        service = new RagCorpusGovernanceService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.save(any(RagCorpusVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Approval: pending corpus becomes approved with durable effective date")
    void approveVersion_marksPendingVersionApproved() {
        RagCorpusVersion pending = version("v1", "pending_review", NOW.minusSeconds(60));
        when(repository.findById("v1")).thenReturn(Optional.of(pending));

        RagCorpusGovernanceService.CorpusVersionMetadata metadata =
                service.approveVersion("v1", "clinical-admin", NOW);

        assertThat(metadata.approvalStatus()).isEqualTo("approved");
        assertThat(metadata.reviewer()).isEqualTo("clinical-admin");
        assertThat(metadata.effectiveDate()).isEqualTo(NOW);
        verify(repository).save(pending);
    }

    @Test
    @DisplayName("Active approved version: lấy latest approved version đã tới effective date")
    void activeApprovedVersion_usesRepositoryLatestEffectiveApprovedVersion() {
        when(repository.findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
                "approved",
                NOW
        )).thenReturn(Optional.of(version("v2", "approved", NOW)));

        assertThat(service.activeApprovedVersion()).contains("v2");
    }

    @Test
    @DisplayName("Rollback: validate previous version trước khi mutate current")
    void rollback_withoutPreviousVersion_doesNotMutateCurrent() {
        RagCorpusVersion current = version("v2", "approved", NOW);
        when(repository.findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
                "approved",
                NOW
        )).thenReturn(Optional.of(current));
        when(repository.findById("v2")).thenReturn(Optional.of(current));
        when(repository.findFirstByStatusAndSourceVersionNotAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
                "approved",
                "v2",
                NOW
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rollbackToPreviousApprovedVersion("v2", "clinical-admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No previous approved");

        assertThat(current.getStatus()).isEqualTo("approved");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Rollback: preserves original approval audit and appends rollback metadata")
    void rollback_preservesApprovalMetadataAndAddsRollbackMetadata() {
        RagCorpusVersion current = version("v2", "approved", NOW);
        current.setReviewer("first-reviewer");
        current.setEffectiveDate(NOW.minusSeconds(30));
        RagCorpusVersion previous = version("v1", "approved", NOW.minusSeconds(60));
        when(repository.findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
                "approved",
                NOW
        )).thenReturn(Optional.of(current));
        when(repository.findById("v2")).thenReturn(Optional.of(current));
        when(repository.findFirstByStatusAndSourceVersionNotAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
                "approved",
                "v2",
                NOW
        )).thenReturn(Optional.of(previous));

        RagCorpusGovernanceService.CorpusVersionMetadata metadata =
                service.rollbackToPreviousApprovedVersion("v2", "rollback-admin");

        assertThat(metadata.sourceVersion()).isEqualTo("v1");
        assertThat(current.getStatus()).isEqualTo("rolled_back");
        assertThat(current.getReviewer()).isEqualTo("first-reviewer");
        assertThat(current.getEffectiveDate()).isEqualTo(NOW.minusSeconds(30));
        assertThat(current.getRolledBackBy()).isEqualTo("rollback-admin");
        assertThat(current.getRolledBackAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Audit inspection: returns durable metadata from repository")
    void inspectMetadata_returnsRepositoryMetadata() {
        when(repository.findAll()).thenReturn(List.of(version("v1", "approved", NOW)));

        assertThat(service.inspectMetadata())
                .singleElement()
                .extracting(RagCorpusGovernanceService.CorpusVersionMetadata::sourceVersion)
                .isEqualTo("v1");
    }

    private RagCorpusVersion version(String sourceVersion, String status, Instant effectiveDate) {
        RagCorpusVersion version = new RagCorpusVersion();
        version.setSourceVersion(sourceVersion);
        version.setStatus(status);
        version.setReviewer("clinical-admin");
        version.setEffectiveDate(effectiveDate);
        version.setEmbeddingModel("text-embedding-3-small");
        version.setEmbeddingDimension(1536);
        version.setChunkCount(1);
        version.setCreatedAt(effectiveDate);
        version.setApprovedAt(effectiveDate);
        return version;
    }
}
