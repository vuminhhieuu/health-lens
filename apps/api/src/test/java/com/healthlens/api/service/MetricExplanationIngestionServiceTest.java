package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.entity.RagCorpusVersion;
import com.healthlens.api.repository.RagCorpusVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricExplanationIngestionServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-19T03:00:00Z");

    private VectorStoreService vectorStoreService;
    private RagCorpusVersionRepository repository;
    private RagCorpusGovernanceService governanceService;
    private MetricExplanationIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        vectorStoreService = mock(VectorStoreService.class);
        repository = mock(RagCorpusVersionRepository.class);
        governanceService = new RagCorpusGovernanceService(repository, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        ingestionService = new MetricExplanationIngestionService(
                new ObjectMapper(),
                vectorStoreService,
                governanceService,
                "text-embedding-3-small",
                1536,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        when(repository.save(any(RagCorpusVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Ingestion report: trả về sourceVersion, chunk count, embedding config và pending audit metadata")
    void ingestWithReport_returnsReportAndWritesPendingGovernedMetadata() {
        MetricExplanationIngestionService.IngestionReport report = ingestionService.ingestWithReport(
                resource("""
                        [
                          {
                            "metricKey": "ALT",
                            "aliases": ["GPT"],
                            "language": "vi",
                            "whatIsIt": "ALT là men gan.",
                            "relatedTo": "chức năng gan.",
                            "impactWhenOutOfRange": "ALT tăng có thể gợi ý tổn thương gan."
                          }
                        ]
                        """),
                "v2026-05-19",
                "clinical-admin"
        );

        assertThat(report.sourceVersion()).isEqualTo("v2026-05-19");
        assertThat(report.chunkCount()).isEqualTo(1);
        assertThat(report.embeddingModel()).isEqualTo("text-embedding-3-small");
        assertThat(report.embeddingDimension()).isEqualTo(1536);
        assertThat(report.errors()).isEmpty();
        assertThat(report.reviewer()).isEqualTo("clinical-admin");
        assertThat(report.effectiveDate()).isEqualTo(FIXED_NOW);

        @SuppressWarnings("unchecked")
        var documentsCaptor = forClass((Class<List<Document>>) (Class<?>) List.class);
        verify(vectorStoreService).upsertDocuments(documentsCaptor.capture());
        Document stored = documentsCaptor.getValue().get(0);
        assertThat(stored.getMetadata())
                .containsEntry("sourceVersion", "v2026-05-19")
                .containsEntry("approvalStatus", "pending_review")
                .containsEntry("reviewer", "clinical-admin")
                .containsEntry("effectiveDate", FIXED_NOW.toString())
                .containsEntry("embeddingModel", "text-embedding-3-small")
                .containsEntry("embeddingDimension", 1536);

        verify(repository).save(any(RagCorpusVersion.class));
    }

    @Test
    @DisplayName("Ingestion report: lỗi parse được đưa vào errors")
    void ingestWithReport_invalidCorpus_returnsErrorReport() {
        MetricExplanationIngestionService.IngestionReport report = ingestionService.ingestWithReport(
                resource("{not-json"),
                "bad-version",
                "clinical-admin"
        );

        assertThat(report.sourceVersion()).isEqualTo("bad-version");
        assertThat(report.chunkCount()).isZero();
        assertThat(report.errors()).isNotEmpty();
        verify(vectorStoreService, never()).upsertDocuments(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Ingestion guard: corpus rỗng không được active hoặc upsert")
    void ingestWithReport_emptyCorpus_returnsErrorWithoutUpsert() {
        MetricExplanationIngestionService.IngestionReport report = ingestionService.ingestWithReport(
                resource("[]"),
                "empty-version",
                "clinical-admin"
        );

        assertThat(report.chunkCount()).isZero();
        assertThat(report.errors()).contains("Metric explanation corpus is empty");
        verify(vectorStoreService, never()).upsertDocuments(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Ingestion guard: sourceVersion blank bị reject trước khi upsert")
    void ingestWithReport_blankSourceVersion_returnsErrorWithoutUpsert() {
        MetricExplanationIngestionService.IngestionReport report = ingestionService.ingestWithReport(
                resource("[]"),
                " ",
                "clinical-admin"
        );

        assertThat(report.errors()).contains("RAG corpus sourceVersion is required");
        verify(vectorStoreService, never()).upsertDocuments(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Ingestion legacy count: trả 0 khi report có lỗi")
    void ingest_returnsZeroWhenReportHasErrors() {
        int count = ingestionService.ingest(resource("{not-json"), "bad-version");

        assertThat(count).isZero();
        verify(vectorStoreService, never()).upsertDocuments(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Ingestion failure: upsert lỗi không được báo chunk count thành công")
    void ingestWithReport_upsertFailure_returnsZeroChunkCount() {
        doThrow(new IllegalStateException("qdrant unavailable"))
                .when(vectorStoreService).upsertDocuments(any());

        MetricExplanationIngestionService.IngestionReport report = ingestionService.ingestWithReport(
                resource("""
                        [
                          {
                            "metricKey": "ALT",
                            "aliases": ["GPT"],
                            "language": "vi",
                            "whatIsIt": "ALT là men gan.",
                            "relatedTo": "chức năng gan.",
                            "impactWhenOutOfRange": "ALT tăng có thể gợi ý tổn thương gan."
                          }
                        ]
                        """),
                "v2026-05-20",
                "clinical-admin"
        );

        assertThat(report.chunkCount()).isZero();
        assertThat(report.hasErrors()).isTrue();
        assertThat(report.errors()).contains("qdrant unavailable");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Ingestion failure: governance lỗi sau upsert vẫn là report thất bại")
    void ingestWithReport_governanceFailureAfterUpsert_returnsZeroChunkCount() {
        doThrow(new IllegalStateException("governance write failed"))
                .when(repository).save(any(RagCorpusVersion.class));

        MetricExplanationIngestionService.IngestionReport report = ingestionService.ingestWithReport(
                resource("""
                        [
                          {
                            "metricKey": "ALT",
                            "aliases": ["GPT"],
                            "language": "vi",
                            "whatIsIt": "ALT là men gan.",
                            "relatedTo": "chức năng gan.",
                            "impactWhenOutOfRange": "ALT tăng có thể gợi ý tổn thương gan."
                          }
                        ]
                        """),
                "v2026-05-21",
                "clinical-admin"
        );

        assertThat(report.chunkCount()).isZero();
        assertThat(report.hasErrors()).isTrue();
        assertThat(report.errors()).contains("governance write failed");
        verify(vectorStoreService).upsertDocuments(any());
    }

    private ByteArrayResource resource(String text) {
        return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
    }
}
