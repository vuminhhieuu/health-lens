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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricExplanationRetrievalServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;
    @Mock
    private ReferenceDataService referenceDataService;

    private MetricExplanationRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new MetricExplanationRetrievalService(
                vectorStoreService,
                referenceDataService,
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
        when(vectorStoreService.semanticSearch(anyString(), eq(3), eq("language == 'vi'")))
                .thenReturn(List.of(hit));

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("qdrant");
        assertThat(result.hit()).isTrue();
        assertThat(result.knowledgeSnippet()).contains("Metric identity: ALT là men gan.");
    }

    @Test
    @DisplayName("Retrieve miss: fallback sang ReferenceData")
    void retrieve_miss_returnsReferenceDataSnippet() {
        when(vectorStoreService.semanticSearch(anyString(), eq(3), eq("language == 'vi'")))
                .thenReturn(List.of());
        when(referenceDataService.buildMetricKnowledgeSnippet(eq("ALT"), eq("abnormal"), eq(sampleRange())))
                .thenReturn("Metric identity: reference snippet");

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("reference-data");
        assertThat(result.hit()).isFalse();
        assertThat(result.knowledgeSnippet()).contains("reference snippet");
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
        when(vectorStoreService.semanticSearch(anyString(), eq(3), eq("language == 'vi'")))
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
        when(vectorStoreService.semanticSearch(anyString(), eq(3), eq("language == 'vi'")))
                .thenThrow(new RuntimeException("qdrant timeout"));
        when(referenceDataService.buildMetricKnowledgeSnippet(eq("ALT"), eq("abnormal"), eq(sampleRange())))
                .thenReturn("Metric identity: fallback from reference");

        MetricExplanationRetrievalService.RetrievalResult result = retrievalService.retrieve(
                "ALT", "abnormal", sampleRange(), "vi");

        assertThat(result.source()).isEqualTo("reference-data");
        assertThat(result.knowledgeSnippet()).contains("fallback from reference");
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
