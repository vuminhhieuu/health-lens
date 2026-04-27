package com.healthlens.api.service;

import com.healthlens.api.dto.ReferenceRangeDto;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetricExplanationRetrievalService {

    private static final String SOURCE_QDRANT = "qdrant";
    private static final String SOURCE_REFERENCE_DATA = "reference-data";
    private static final String SOURCE_GENERIC = "generic";
    private static final String UNKNOWN = "unknown";

    private final VectorStoreService vectorStoreService;
    private final ReferenceDataService referenceDataService;
    private final MeterRegistry meterRegistry;
    private final int topK;

    public MetricExplanationRetrievalService(
            VectorStoreService vectorStoreService,
            ReferenceDataService referenceDataService,
            MeterRegistry meterRegistry,
            @Value("${app.ai.explanation.retrieval.top-k:3}") int topK
    ) {
        this.vectorStoreService = vectorStoreService;
        this.referenceDataService = referenceDataService;
        this.meterRegistry = meterRegistry;
        this.topK = topK;
    }

    public RetrievalResult retrieve(String metricName, String status, ReferenceRangeDto referenceRange, String language) {
        String safeMetric = metricName == null || metricName.isBlank() ? "metric" : metricName.trim();
        String normalizedLang = language == null || language.isBlank() ? "vi" : language.trim().toLowerCase(Locale.ROOT);
        long startNanos = System.nanoTime();

        try {
            String query = buildQuery(safeMetric, status, referenceRange);
            String filter = "language == '" + normalizedLang + "'";
            List<Document> documents = vectorStoreService.semanticSearch(query, topK, filter);
            List<Document> metricMatchedDocuments = documents.stream()
                    .filter(document -> matchesMetricOrAlias(document, safeMetric))
                    .collect(Collectors.toList());
            if (!metricMatchedDocuments.isEmpty()) {
                String snippet = composeSnippet(metricMatchedDocuments);
                double topScore = resolveTopScore(metricMatchedDocuments.get(0));
                recordMetrics(SOURCE_QDRANT, true, topScore, startNanos);
                log.info(
                        "metric_explanation_retrieval source={} hit={} metric={} topScore={} latencyMs={}",
                        SOURCE_QDRANT, true, safeMetric, topScore, elapsedMs(startNanos)
                );
                return new RetrievalResult(snippet, SOURCE_QDRANT, true, topScore);
            }
        } catch (Exception ex) {
            log.warn("metric_explanation_retrieval source={} hit=false metric={} error={}",
                    SOURCE_QDRANT, safeMetric, ex.getMessage());
        }

        String fallbackSnippet = referenceDataService.buildMetricKnowledgeSnippet(safeMetric, status, referenceRange);
        if (fallbackSnippet != null && !fallbackSnippet.isBlank()) {
            recordMetrics(SOURCE_REFERENCE_DATA, false, 0.0d, startNanos);
            log.info("metric_explanation_retrieval source={} hit=false metric={} topScore=0 latencyMs={}",
                    SOURCE_REFERENCE_DATA, safeMetric, elapsedMs(startNanos));
            return new RetrievalResult(fallbackSnippet, SOURCE_REFERENCE_DATA, false, 0.0d);
        }

        recordMetrics(SOURCE_GENERIC, false, 0.0d, startNanos);
        log.info("metric_explanation_retrieval source={} hit=false metric={} topScore=0 latencyMs={}",
                SOURCE_GENERIC, safeMetric, elapsedMs(startNanos));
        return new RetrievalResult(buildGenericSnippet(safeMetric, status), SOURCE_GENERIC, false, 0.0d);
    }

    private String buildQuery(String metricName, String status, ReferenceRangeDto referenceRange) {
        String statusText = status == null || status.isBlank() ? UNKNOWN : status;
        String rangeText = "N/A";
        if (referenceRange != null && referenceRange.min() != null && referenceRange.max() != null) {
            rangeText = referenceRange.min().toPlainString() + " - " + referenceRange.max().toPlainString();
        }
        return "metric: " + metricName + ", status: " + statusText + ", reference range: " + rangeText;
    }

    private String composeSnippet(List<Document> documents) {
        Document first = documents.get(0);
        String metricIdentity = metadata(first, "whatIsIt", "Đây là chỉ số xét nghiệm máu.");
        String relatedTo = metadata(first, "relatedTo", "chuyển hóa, miễn dịch hoặc chức năng cơ quan.");
        String impact = metadata(first, "impactWhenOutOfRange", "Khi lệch ngưỡng, nguy cơ bất thường sức khỏe có thể tăng.");
        return """
                Metric identity: %s
                Clinical relation: %s
                Out-of-range impact: %s
                """.formatted(metricIdentity, relatedTo, impact);
    }

    private String metadata(Document document, String key, String fallback) {
        if (document == null || document.getMetadata() == null) {
            return fallback;
        }
        Object value = document.getMetadata().get(key);
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isBlank() ? fallback : text;
    }

    private double resolveTopScore(Document document) {
        if (document == null || document.getMetadata() == null) {
            return 0.0d;
        }
        Map<String, Object> metadata = document.getMetadata();
        return parseDouble(metadata.get("score"))
                .or(() -> parseDouble(metadata.get("distance")))
                .orElse(0.0d);
    }

    private Optional<Double> parseDouble(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value.toString()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private boolean matchesMetricOrAlias(Document document, String metricName) {
        if (document == null || document.getMetadata() == null) {
            return false;
        }
        String normalizedTarget = normalizeMetricToken(metricName);
        if (normalizedTarget.isBlank()) {
            return false;
        }

        String normalizedMetricKey = normalizeMetricToken(document.getMetadata().get("metricKey"));
        if (normalizedTarget.equals(normalizedMetricKey)) {
            return true;
        }

        Object aliasesRaw = document.getMetadata().get("aliases");
        Set<String> normalizedAliases = extractAliases(aliasesRaw);
        return normalizedAliases.contains(normalizedTarget);
    }

    private Set<String> extractAliases(Object aliasesRaw) {
        if (aliasesRaw == null) {
            return Set.of();
        }
        if (aliasesRaw instanceof List<?> list) {
            return list.stream()
                    .map(this::normalizeMetricToken)
                    .filter(token -> !token.isBlank())
                    .collect(Collectors.toSet());
        }
        String raw = aliasesRaw.toString();
        if (raw.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(this::normalizeMetricToken)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizeMetricToken(Object token) {
        if (token == null) {
            return "";
        }
        return token.toString()
                .replaceAll("[^A-Za-z0-9%]", "")
                .toUpperCase(Locale.ROOT);
    }

    private void recordMetrics(String source, boolean hit, double topScore, long startNanos) {
        meterRegistry.counter("metric.explanation.retrieval.count",
                "source", source,
                "hit", Boolean.toString(hit)).increment();
        DistributionSummary.builder("metric.explanation.retrieval.top_score")
                .tag("source", source)
                .register(meterRegistry)
                .record(topScore);
        Timer.builder("metric.explanation.retrieval.latency")
                .tag("source", source)
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String buildGenericSnippet(String metricName, String status) {
        String safeStatus = status == null || status.isBlank() ? "unknown" : status;
        return """
                Metric identity: %s là một chỉ số xét nghiệm sức khỏe.
                Clinical relation: chỉ số này nên được đọc cùng các chỉ số liên quan và bối cảnh lâm sàng.
                Out-of-range impact: khi trạng thái là %s, bạn nên theo dõi thêm và tham khảo chuyên gia y tế nếu cần.
                """.formatted(metricName, safeStatus);
    }

    public record RetrievalResult(String knowledgeSnippet, String source, boolean hit, double topScore) {
    }
}
