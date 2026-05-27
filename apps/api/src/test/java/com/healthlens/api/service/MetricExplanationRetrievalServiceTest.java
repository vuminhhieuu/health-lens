package com.healthlens.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.entity.OnlineRagReviewStatus;
import com.healthlens.api.service.rag.TrustedOnlineRagSourceAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.document.Document;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricExplanationRetrievalServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;
    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private RagCorpusGovernanceService governanceService;
    @Mock
    private TrustedOnlineRagSourceAdapter onlineRagSourceAdapter;
    @Mock
    private AuditEventRecorder auditEventRecorder;

    private MetricExplanationRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        lenient().when(governanceService.activeApprovedVersion()).thenReturn(java.util.Optional.of("v1"));
        retrievalService = new MetricExplanationRetrievalService(
                vectorStoreService,
                referenceDataService,
                governanceService,
                onlineRagSourceAdapter,
                new SimpleMeterRegistry(),
                3
        );
    }

    @Test
    @DisplayName("Retrieve hit: dùng snippet từ Qdrant")
    void retrieve_hit_returnsQdrantSnippet() {
        Document hit = Document.builder()
                .id("metric-expl:ALT:v1")
                .text("ALT")
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "aliases", List.of("GPT"),
                        "whatIsIt", "ALT là men gan.",
                        "relatedTo", "chức năng gan.",
                        "impactWhenOutOfRange", "ALT tăng có thể gợi ý tổn thương gan.",
                        "score", 0.88
                ))
                .build();
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("qdrant");
        assertThat(result.hit()).isTrue();
        assertThat(result.knowledgeSnippet())
                .contains("Curated approved corpus chunk:")
                .contains("Metric identity: ALT là men gan.");
        assertThat(result.trace().fallbackPath()).isEqualTo("none");
    }

    @Test
    @DisplayName("Retrieve miss: fallback sang ReferenceData")
    void retrieve_miss_returnsReferenceDataSnippet() {
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of());
        when(referenceDataService.buildMetricKnowledgeSnippet(eq("ALT"), eq("abnormal"), eq(sampleRange())))
                .thenReturn("Metric identity: reference snippet");

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("reference-data");
        assertThat(result.hit()).isFalse();
        assertThat(result.knowledgeSnippet()).contains("reference snippet");
        assertThat(result.trace().fallbackPath()).isEqualTo("qdrant_miss_to_reference_data");
    }

    @Test
    @DisplayName("Retrieve audit: gắn actor khi caller truyền userId")
    void retrieve_withActor_recordsAttributedAudit() {
        UUID actorId = UUID.randomUUID();
        retrievalService.setAuditEventRecorder(auditEventRecorder);
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of());
        when(referenceDataService.buildMetricKnowledgeSnippet(eq("ALT"), eq("abnormal"), eq(sampleRange())))
                .thenReturn("Metric identity: reference snippet");

        retrievalService.retrieve("ALT", "abnormal", sampleRange(), "vi", MetricExplanationRetrievalService.RetrievalContext.none(), actorId);

        verify(auditEventRecorder).recordEvent(
                eq(actorId),
                eq(AuditActions.RAG_RETRIEVAL),
                eq(AuditResourceTypes.RAG_RETRIEVAL),
                isNull(),
                any(Map.class)
        );
        verify(auditEventRecorder, never()).recordAnonymous(
                eq(AuditActions.RAG_RETRIEVAL),
                eq(AuditResourceTypes.RAG_RETRIEVAL),
                isNull(),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Retrieve bỏ qua kết quả sai metricKey và fallback sang ReferenceData")
    void retrieve_mismatchMetricKey_fallbacksToReferenceData() {
        Document unrelated = Document.builder()
                .id("metric-expl:AST:v1")
                .text("AST")
                .metadata(Map.of(
                        "metricKey", "AST",
                        "aliases", List.of("GOT"),
                        "whatIsIt", "AST là men gan."
                ))
                .build();
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(unrelated));
        when(referenceDataService.buildMetricKnowledgeSnippet(eq("ALT"), eq("abnormal"), eq(sampleRange())))
                .thenReturn("Metric identity: reference snippet");

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("reference-data");
        assertThat(result.hit()).isFalse();
        assertThat(result.knowledgeSnippet()).contains("reference snippet");
    }

    @Test
    @DisplayName("Retrieve hit: alias tiếng Việt có dấu được normalize")
    void retrieve_vietnameseAliasWithDiacritics_hitsQdrantSnippet() {
        Document hit = Document.builder()
                .id("metric-expl:WBC:v1")
                .text("WBC")
                .metadata(Map.of(
                        "metricKey", "WBC",
                        "aliases", List.of("Bạch cầu", "Bach cau"),
                        "whatIsIt", "WBC là số lượng bạch cầu.",
                        "relatedTo", "miễn dịch và nhiễm trùng."
                ))
                .build();
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "Bach cau", "normal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("qdrant");
        assertThat(result.hit()).isTrue();
        assertThat(result.knowledgeSnippet()).contains("WBC là số lượng bạch cầu");
    }

    @Test
    @DisplayName("Retrieve timeout/error: fallback an toàn, không ném exception")
    void retrieve_timeout_returnsSafeFallback() {
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenThrow(new RuntimeException("qdrant timeout"));
        when(referenceDataService.buildMetricKnowledgeSnippet(eq("ALT"), eq("abnormal"), eq(sampleRange())))
                .thenReturn("Metric identity: fallback from reference");

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("reference-data");
        assertThat(result.knowledgeSnippet()).contains("fallback from reference");
        assertThat(result.trace().fallbackPath()).isEqualTo("qdrant_error_to_reference_data");
    }

    @Test
    @DisplayName("Retrieve governance: không dùng Qdrant nếu chưa có active approved corpus")
    void retrieve_withoutActiveApprovedCorpus_fallbacksToReferenceData() {
        RagCorpusGovernanceService emptyGovernance = mock(RagCorpusGovernanceService.class);
        when(emptyGovernance.activeApprovedVersion()).thenReturn(java.util.Optional.empty());
        retrievalService = new MetricExplanationRetrievalService(
                vectorStoreService,
                referenceDataService,
                emptyGovernance,
                onlineRagSourceAdapter,
                new SimpleMeterRegistry(),
                3
        );
        when(referenceDataService.buildMetricKnowledgeSnippet(eq("ALT"), eq("abnormal"), eq(sampleRange())))
                .thenReturn("Metric identity: reference snippet");

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("reference-data");
        assertThat(result.hit()).isFalse();
        assertThat(result.trace().fallbackPath()).isEqualTo("no_active_corpus_to_reference_data");
    }

    @Test
    @DisplayName("Rollback: retrieval dùng previous approved version sau khi rollback")
    void retrieve_afterRollback_filtersToPreviousApprovedVersion() {
        when(governanceService.activeApprovedVersion()).thenReturn(java.util.Optional.of("v1"));

        Document hit = Document.builder()
                .id("metric-expl:ALT:v1")
                .text("ALT")
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "aliases", List.of("GPT"),
                        "whatIsIt", "ALT là men gan.",
                        "relatedTo", "chức năng gan.",
                        "impactWhenOutOfRange", "ALT tăng có thể gợi ý tổn thương gan."
                ))
                .build();
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("qdrant");
        assertThat(result.hit()).isTrue();
    }

    @Test
    @DisplayName("Retrieve context: chỉ đưa profile context khi caller đã kiểm tra access/consent")
    void retrieve_withCheckedProfileContext_includesContextInSnippet() {
        Document hit = Document.builder()
                .id("metric-expl:ALT:v1")
                .text("ALT approved chunk")
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "aliases", List.of("GPT"),
                        "whatIsIt", "ALT là men gan.",
                        "relatedTo", "chức năng gan.",
                        "impactWhenOutOfRange", "ALT tăng có thể gợi ý tổn thương gan.",
                        "score", 0.88
                ))
                .build();
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT",
                "abnormal",
                sampleRange(),
                "vi",
                new MetricExplanationRetrievalService.RetrievalContext(
                        true,
                        "{\"age\":42,\"gender\":\"male\",\"accessScope\":\"owner\"}"
                )
        );

        assertThat(result.knowledgeSnippet())
                .contains("ALT approved chunk")
                .contains("Profile context (access and consent checked): {\"age\":42,\"gender\":\"male\",\"accessScope\":\"owner\"}");
    }

    @Test
    @DisplayName("Curated corpus covers Priority A metric keys")
    void curatedCorpus_coversPriorityAMetrics() throws Exception {
        List<Map<String, Object>> chunks = new ObjectMapper().readValue(
                java.nio.file.Files.readString(resolveMetricExplanationsPath()),
                new TypeReference<>() {}
        );
        assertThat(chunks)
                .extracting(chunk -> chunk.get("metricKey").toString())
                .contains(
                        "GLUCOSE", "HBA1C", "CHOL", "TRIGLYCERIDE", "HDL", "LDL",
                        "HGB", "WBC", "RBC", "HCT", "PLT", "MCV", "MCH", "MCHC", "RDW",
                        "NEUTROPHILS", "LYMPHOCYTES", "MONOCYTES", "EOSINOPHILS", "BASOPHILS",
                        "AST", "ALT", "ALP", "GGT", "BILIRUBIN TOTAL", "BILIRUBIN DIRECT",
                        "ALBUMIN", "TOTAL PROTEIN", "CREATININE", "BUN", "UREA",
                        "SODIUM", "POTASSIUM", "CHLORIDE", "CALCIUM", "CRP"
                );
    }

    @Test
    @DisplayName("Retrieve context: Priority A OCR alias hits curated corpus instead of generic fallback")
    void retrieve_priorityAOcrAlias_hitsCuratedCorpus() {
        Document hit = Document.builder()
                .id("metric-expl:WBC:v1")
                .text("WBC approved chunk")
                .metadata(Map.of(
                        "metricKey", "WBC",
                        "aliases", List.of("WHITE BLOOD CELL", "BẠCH CẦU", "BC"),
                        "whatIsIt", "WBC là số lượng bạch cầu.",
                        "relatedTo", "miễn dịch.",
                        "impactWhenOutOfRange", "WBC lệch ngưỡng có thể gợi ý viêm hoặc nhiễm trùng.",
                        "score", 0.9
                ))
                .build();
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "Bạch cầu",
                "abnormal",
                sampleRange(),
                "vi");

        assertThat(result.trace().source()).isEqualTo("qdrant");
        assertThat(result.trace().hit()).isTrue();
        assertThat(result.knowledgeSnippet()).contains("WBC approved chunk");
        verify(referenceDataService, never()).buildMetricKnowledgeSnippet(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Retrieve context: giới hạn kích thước curated chunk trước khi đưa vào prompt")
    void retrieve_largeCuratedChunk_truncatesSnippet() {
        String longChunk = "A".repeat(1_500);
        Document hit = Document.builder()
                .id("metric-expl:ALT:v1")
                .text(longChunk)
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "whatIsIt", "ALT là men gan.",
                        "relatedTo", "chức năng gan.",
                        "impactWhenOutOfRange", "ALT tăng có thể gợi ý tổn thương gan."
                ))
                .build();
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.knowledgeSnippet()).contains("A".repeat(1_200) + "...");
        assertThat(result.knowledgeSnippet()).doesNotContain("A".repeat(1_300));
    }

    @Test
    @DisplayName("Retrieve online RAG: đính kèm citation metadata nhưng chỉ dùng content khi source đã được duyệt")
    void retrieve_onlineRagCitation_includesMetadataAndOnlyApprovedContent() {
        Document hit = Document.builder()
                .id("metric-expl:ALT:v1")
                .text("ALT curated chunk")
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "sourceUrl", "https://who.int/news/item/alt",
                        "publisher", "WHO",
                        "whatIsIt", "ALT là men gan.",
                        "relatedTo", "chức năng gan.",
                        "impactWhenOutOfRange", "ALT tăng có thể gợi ý tổn thương gan.",
                        "score", 0.88
                ))
                .build();
        UUID snapshotId = UUID.randomUUID();
        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult onlineResult =
                new TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult(
                        Optional.of("Approved online context"),
                        new TrustedOnlineRagSourceAdapter.OnlineRagSourceMetadata(
                                snapshotId,
                                "https://who.int/news/item/alt",
                                "WHO",
                                Instant.parse("2026-05-19T08:00:00Z"),
                                "a".repeat(64),
                                OnlineRagReviewStatus.APPROVED,
                                false,
                                false
                        ),
                        true,
                        false,
                        false,
                        true
                );
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));
        when(onlineRagSourceAdapter.retrieve(URI.create("https://who.int/news/item/alt"), "WHO"))
                .thenReturn(onlineResult);

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.knowledgeSnippet()).contains("Approved online context");
        assertThat(result.onlineCitations()).hasSize(1);
        assertThat(result.onlineCitations().get(0).sourceSnapshotId()).isEqualTo(snapshotId);
        assertThat(result.onlineCitations().get(0).sourceUrl()).isEqualTo("https://who.int/news/item/alt");
        assertThat(result.onlineCitations().get(0).reviewStatus()).isEqualTo(OnlineRagReviewStatus.APPROVED);
        assertThat(result.onlineCitations().get(0).cacheHit()).isTrue();
        verify(onlineRagSourceAdapter).retrieve(URI.create("https://who.int/news/item/alt"), "WHO");
    }

    @Test
    @DisplayName("Retrieve online RAG: source review-required hiện trong audit metadata nhưng không vào prompt")
    void retrieve_onlineRagCitation_reviewRequiredNotUsedAsApprovedEvidence() {
        Document hit = Document.builder()
                .id("metric-expl:ALT:v1")
                .text("ALT curated chunk")
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "sourceUrl", "https://who.int/news/item/alt-review",
                        "publisher", "WHO"
                ))
                .build();
        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult onlineResult =
                new TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult(
                        Optional.of("Unreviewed online context must not be used"),
                        new TrustedOnlineRagSourceAdapter.OnlineRagSourceMetadata(
                                UUID.randomUUID(),
                                "https://who.int/news/item/alt-review",
                                "WHO",
                                Instant.parse("2026-05-19T08:00:00Z"),
                                "b".repeat(64),
                                OnlineRagReviewStatus.REVIEW_REQUIRED,
                                true,
                                false
                        ),
                        false,
                        true,
                        false,
                        false
                );
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));
        when(onlineRagSourceAdapter.retrieve(URI.create("https://who.int/news/item/alt-review"), "WHO"))
                .thenReturn(onlineResult);

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.knowledgeSnippet()).doesNotContain("Unreviewed online context must not be used");
        assertThat(result.onlineCitations()).hasSize(1);
        assertThat(result.onlineCitations().get(0).reviewStatus()).isEqualTo(OnlineRagReviewStatus.REVIEW_REQUIRED);
        assertThat(result.onlineCitations().get(0).usableForAi()).isFalse();
    }

    @Test
    @DisplayName("Retrieve online RAG: lỗi fetch online không làm rơi Qdrant hit hợp lệ")
    void retrieve_onlineRagCitation_fetchFailureKeepsCuratedHit() {
        Document hit = Document.builder()
                .id("metric-expl:ALT:v1")
                .text("ALT curated chunk")
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "sourceUrl", "https://who.int/news/item/alt-timeout",
                        "publisher", "WHO"
                ))
                .build();
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(hit));
        when(onlineRagSourceAdapter.retrieve(URI.create("https://who.int/news/item/alt-timeout"), "WHO"))
                .thenThrow(new IllegalStateException("timeout"));

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("qdrant");
        assertThat(result.hit()).isTrue();
        assertThat(result.knowledgeSnippet()).contains("ALT curated chunk");
        assertThat(result.onlineCitations()).hasSize(1);
        assertThat(result.onlineCitations().get(0).reviewRequired()).isTrue();
        assertThat(result.onlineCitations().get(0).stale()).isFalse();
        assertThat(result.onlineCitations().get(0).usableForAi()).isFalse();
    }

    @Test
    @DisplayName("Retrieve online RAG: chỉ resolve online source của top document để tránh nhiều HTTP call serial")
    void retrieve_onlineRagCitation_resolvesOnlyTopDocument() {
        Document topHit = Document.builder()
                .id("metric-expl:ALT:v1")
                .text("ALT curated chunk")
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "sourceUrl", "https://who.int/news/item/alt-top",
                        "publisher", "WHO"
                ))
                .build();
        Document secondHit = Document.builder()
                .id("metric-expl:ALT:v1-secondary")
                .text("ALT secondary chunk")
                .metadata(Map.of(
                        "metricKey", "ALT",
                        "sourceUrl", "https://who.int/news/item/alt-second",
                        "publisher", "WHO"
                ))
                .build();
        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult onlineResult =
                new TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult(
                        Optional.empty(),
                        new TrustedOnlineRagSourceAdapter.OnlineRagSourceMetadata(
                                UUID.randomUUID(),
                                "https://who.int/news/item/alt-top",
                                "WHO",
                                Instant.parse("2026-05-19T08:00:00Z"),
                                "f".repeat(64),
                                OnlineRagReviewStatus.REVIEW_REQUIRED,
                                true,
                                false
                        ),
                        false,
                        true,
                        false,
                        false
                );
        when(vectorStoreService.semanticSearch(anyString(), eq(3),
                eq("language == 'vi' && sourceVersion == 'v1'")))
                .thenReturn(List.of(topHit, secondHit));
        when(onlineRagSourceAdapter.retrieve(URI.create("https://who.int/news/item/alt-top"), "WHO"))
                .thenReturn(onlineResult);

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.onlineCitations()).hasSize(1);
        assertThat(result.onlineCitations().get(0).sourceUrl()).isEqualTo("https://who.int/news/item/alt-top");
        verify(onlineRagSourceAdapter).retrieve(URI.create("https://who.int/news/item/alt-top"), "WHO");
        verify(onlineRagSourceAdapter, never()).retrieve(URI.create("https://who.int/news/item/alt-second"), "WHO");
    }

    private ReferenceRangeDto sampleRange() {
        return new ReferenceRangeDto(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(40),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(60),
                "U/L"
        );
    }

    private java.nio.file.Path resolveMetricExplanationsPath() {
        List<java.nio.file.Path> candidates = List.of(
                java.nio.file.Path.of("src", "main", "resources", "ai", "metric-explanations.vi.json"),
                java.nio.file.Path.of("apps", "api", "src", "main", "resources", "ai", "metric-explanations.vi.json")
        );
        return candidates.stream()
                .filter(java.nio.file.Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot find metric-explanations.vi.json"));
    }
}
