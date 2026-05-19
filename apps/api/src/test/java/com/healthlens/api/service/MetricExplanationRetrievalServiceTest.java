package com.healthlens.api.service;

import com.healthlens.api.dto.ReferenceRangeDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.document.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricExplanationRetrievalServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;
    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private RagCorpusGovernanceService governanceService;

    private MetricExplanationRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        lenient().when(governanceService.activeApprovedVersion()).thenReturn(java.util.Optional.of("v1"));
        retrievalService = new MetricExplanationRetrievalService(
                vectorStoreService,
                referenceDataService,
                governanceService,
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

    private ReferenceRangeDto sampleRange() {
        return new ReferenceRangeDto(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(40),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(60),
                "U/L"
        );
    }
}
