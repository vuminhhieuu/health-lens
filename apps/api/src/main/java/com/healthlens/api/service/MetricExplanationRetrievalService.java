package com.healthlens.api.service;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.entity.OnlineRagReviewStatus;
import com.healthlens.api.service.rag.TrustedOnlineRagSourceAdapter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetricExplanationRetrievalService {

    private static final String SOURCE_QDRANT = "qdrant";
    private static final String SOURCE_REFERENCE_DATA = "reference-data";
    private static final String SOURCE_GENERIC = "generic";
    private static final String UNKNOWN = "unknown";
    private static final String FALLBACK_NONE = "none";
    private static final String FALLBACK_NO_ACTIVE_CORPUS = "no_active_corpus_to_reference_data";
    private static final String FALLBACK_REFERENCE_DATA = "qdrant_miss_to_reference_data";
    private static final String FALLBACK_QDRANT_ERROR_TO_REFERENCE_DATA = "qdrant_error_to_reference_data";
    private static final String FALLBACK_GENERIC = "reference_data_miss_to_generic";
    private static final int MAX_CURATED_CHUNK_CHARS = 1_200;
    private static final int MAX_ONLINE_CITATION_RESOLUTION_DOCS = 1;

    private final VectorStoreService vectorStoreService;
    private final ReferenceDataService referenceDataService;
    private final RagCorpusGovernanceService governanceService;
    private final TrustedOnlineRagSourceAdapter onlineRagSourceAdapter;
    private final MeterRegistry meterRegistry;
    private final int topK;
    private AuditEventRecorder auditEventRecorder;

    public MetricExplanationRetrievalService(
            VectorStoreService vectorStoreService,
            ReferenceDataService referenceDataService,
            RagCorpusGovernanceService governanceService,
            TrustedOnlineRagSourceAdapter onlineRagSourceAdapter,
            MeterRegistry meterRegistry,
            @Value("${app.ai.explanation.retrieval.top-k:3}") int topK
    ) {
        this.vectorStoreService = vectorStoreService;
        this.referenceDataService = referenceDataService;
        this.governanceService = governanceService;
        this.onlineRagSourceAdapter = onlineRagSourceAdapter;
        this.meterRegistry = meterRegistry;
        this.topK = topK;
    }

    @Autowired(required = false)
    void setAuditEventRecorder(AuditEventRecorder auditEventRecorder) {
        this.auditEventRecorder = auditEventRecorder;
    }

    public RetrievalResult retrieve(String metricName, String status, ReferenceRangeDto referenceRange, String language) {
        return retrieve(metricName, status, referenceRange, language, RetrievalContext.none());
    }

    public RetrievalResult retrieve(
            String metricName,
            String status,
            ReferenceRangeDto referenceRange,
            String language,
            RetrievalContext retrievalContext
    ) {
        return retrieve(metricName, status, referenceRange, language, retrievalContext, null);
    }

    public RetrievalResult retrieve(
            String metricName,
            String status,
            ReferenceRangeDto referenceRange,
            String language,
            RetrievalContext retrievalContext,
            UUID actorId
    ) {
        String safeMetric = metricName == null || metricName.isBlank() ? "metric" : metricName.trim();
        String normalizedLang = language == null || language.isBlank() ? "vi" : language.trim().toLowerCase(Locale.ROOT);
        RetrievalContext safeContext = retrievalContext == null ? RetrievalContext.none() : retrievalContext;
        long startNanos = System.nanoTime();
        String fallbackPath = FALLBACK_NO_ACTIVE_CORPUS;

        try {
            Optional<String> activeVersion = governanceService.activeApprovedVersion();
            if (activeVersion.isPresent()) {
                fallbackPath = FALLBACK_REFERENCE_DATA;
                String query = buildQuery(safeMetric, status, referenceRange);
                String filter = buildGovernedFilter(normalizedLang, activeVersion.get());
                List<Document> documents = vectorStoreService.semanticSearch(query, topK, filter);
                List<Document> metricMatchedDocuments = documents.stream()
                        .filter(document -> matchesMetricOrAlias(document, safeMetric))
                        .collect(Collectors.toList());
                if (!metricMatchedDocuments.isEmpty()) {
                    OnlineCitationBundle onlineCitationBundle = resolveOnlineCitations(metricMatchedDocuments);
                    String snippet = composeSnippet(
                            metricMatchedDocuments,
                            referenceRange,
                            safeContext,
                            onlineCitationBundle.approvedContent()
                    );
                    double topScore = resolveTopScore(metricMatchedDocuments.get(0));
                    RetrievalTrace trace = new RetrievalTrace(SOURCE_QDRANT, true, topScore, FALLBACK_NONE);
                    recordMetrics(trace, startNanos);
                    recordRetrievalAudit(actorId, safeMetric, trace, normalizedLang, onlineCitationBundle.citations().size());
                    log.info(
                            "metric_explanation_retrieval source={} hit={} metric={} topScore={} fallbackPath={} latencyMs={}",
                            SOURCE_QDRANT, true, safeMetric, topScore, FALLBACK_NONE, elapsedMs(startNanos)
                    );
                    return new RetrievalResult(snippet, trace, onlineCitationBundle.citations());
                }
            }
        } catch (Exception ex) {
            fallbackPath = FALLBACK_QDRANT_ERROR_TO_REFERENCE_DATA;
            log.warn("metric_explanation_retrieval source={} hit=false metric={} error={}",
                    SOURCE_QDRANT, safeMetric, ex.getMessage());
        }

        String referenceDataSnippet = referenceDataService.buildMetricKnowledgeSnippet(safeMetric, status, referenceRange);
        if (referenceDataSnippet != null && !referenceDataSnippet.isBlank()) {
            String fallbackSnippet = withStructuredContext(referenceDataSnippet, referenceRange, safeContext);
            RetrievalTrace trace = new RetrievalTrace(SOURCE_REFERENCE_DATA, false, 0.0d, fallbackPath);
            recordMetrics(trace, startNanos);
            recordRetrievalAudit(actorId, safeMetric, trace, normalizedLang, 0);
            log.info("metric_explanation_retrieval source={} hit=false metric={} topScore=0 fallbackPath={} latencyMs={}",
                    SOURCE_REFERENCE_DATA, safeMetric, fallbackPath, elapsedMs(startNanos));
            return new RetrievalResult(fallbackSnippet, trace);
        }

        RetrievalTrace trace = new RetrievalTrace(SOURCE_GENERIC, false, 0.0d, FALLBACK_GENERIC);
        recordMetrics(trace, startNanos);
        recordRetrievalAudit(actorId, safeMetric, trace, normalizedLang, 0);
        log.info("metric_explanation_retrieval source={} hit=false metric={} topScore=0 fallbackPath={} latencyMs={}",
                SOURCE_GENERIC, safeMetric, FALLBACK_GENERIC, elapsedMs(startNanos));
        return new RetrievalResult(
                withStructuredContext(buildGenericSnippet(safeMetric, status), referenceRange, safeContext),
                trace
        );
    }

    private void recordRetrievalAudit(UUID actorId, String metricName, RetrievalTrace trace, String language, int citationCount) {
        if (auditEventRecorder == null) {
            return;
        }
        Map<String, ?> details = Map.of(
                "metricName", metricName,
                "source", trace.source(),
                "hit", trace.hit(),
                "topScore", trace.topScore(),
                "fallbackPath", trace.fallbackPath(),
                "language", language,
                "citationCount", citationCount
        );
        if (actorId != null) {
            auditEventRecorder.recordEvent(actorId, AuditActions.RAG_RETRIEVAL, AuditResourceTypes.RAG_RETRIEVAL, null, details);
            return;
        }
        auditEventRecorder.recordAnonymous(AuditActions.RAG_RETRIEVAL, AuditResourceTypes.RAG_RETRIEVAL, null, details);
    }

    private String buildQuery(String metricName, String status, ReferenceRangeDto referenceRange) {
        String statusText = status == null || status.isBlank() ? UNKNOWN : status;
        String rangeText = "N/A";
        if (referenceRange != null && referenceRange.min() != null && referenceRange.max() != null) {
            rangeText = referenceRange.min().toPlainString() + " - " + referenceRange.max().toPlainString();
        }
        return "metric: " + metricName + ", status: " + statusText + ", reference range: " + rangeText;
    }

    private String buildGovernedFilter(String language, String activeVersion) {
        return "language == '" + escapeFilterStringLiteral(language) + "'"
                + " && sourceVersion == '" + escapeFilterStringLiteral(activeVersion) + "'";
    }

    private String composeSnippet(
            List<Document> documents,
            ReferenceRangeDto referenceRange,
            RetrievalContext retrievalContext,
            List<String> approvedOnlineContent
    ) {
        Document first = documents.get(0);
        String curatedChunk = first.getText() == null || first.getText().isBlank()
                ? "N/A"
                : truncate(first.getText().trim(), MAX_CURATED_CHUNK_CHARS);
        String metricIdentity = metadata(first, "whatIsIt", "Đây là chỉ số xét nghiệm máu.");
        String relatedTo = metadata(first, "relatedTo", "chuyển hóa, miễn dịch hoặc chức năng cơ quan.");
        String impact = metadata(first, "impactWhenOutOfRange", "Khi lệch ngưỡng, nguy cơ bất thường sức khỏe có thể tăng.");
        String snippet = """
                Curated approved corpus chunk:
                %s
                Metric identity: %s
                Clinical relation: %s
                Out-of-range impact: %s
                """.formatted(curatedChunk, metricIdentity, relatedTo, impact);
        if (approvedOnlineContent != null && !approvedOnlineContent.isEmpty()) {
            snippet += "\nApproved online source context:\n" + String.join("\n---\n", approvedOnlineContent);
        }
        return withStructuredContext(snippet, referenceRange, retrievalContext);
    }

    private OnlineCitationBundle resolveOnlineCitations(List<Document> documents) {
        if (onlineRagSourceAdapter == null || documents == null || documents.isEmpty()) {
            return OnlineCitationBundle.empty();
        }

        List<OnlineCitationMetadata> citations = new ArrayList<>();
        List<String> approvedContent = new ArrayList<>();
        for (Document document : documents.stream().limit(MAX_ONLINE_CITATION_RESOLUTION_DOCS).toList()) {
            Optional<URI> sourceUri = onlineSourceUri(document);
            if (sourceUri.isEmpty()) {
                continue;
            }
            String publisher = metadata(document, "publisher", metadata(document, "sourcePublisher", ""));
            TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result;
            try {
                result = onlineRagSourceAdapter.retrieve(sourceUri.get(), publisher);
            } catch (RuntimeException ex) {
                log.warn("online_rag_citation_retrieval_failed sourceUrl={} error={}", sourceUri.get(), ex.getMessage());
                citations.add(new OnlineCitationMetadata(
                        null,
                        sourceUri.get().toString(),
                        publisher,
                        null,
                        "",
                        OnlineRagReviewStatus.REVIEW_REQUIRED,
                        true,
                        false,
                        false,
                        true,
                        false,
                        false
                ));
                continue;
            }
            TrustedOnlineRagSourceAdapter.OnlineRagSourceMetadata metadata = result.metadata();
            citations.add(new OnlineCitationMetadata(
                    metadata.sourceSnapshotId(),
                    metadata.sourceUrl(),
                    metadata.publisher(),
                    metadata.retrievedAt(),
                    metadata.snapshotHash(),
                    metadata.reviewStatus(),
                    metadata.excluded(),
                    result.cacheHit(),
                    result.usableForAi(),
                    result.reviewRequired(),
                    result.rejected(),
                    metadata.stale()
            ));
            if (result.usableForAi()) {
                result.content()
                        .map(String::trim)
                        .filter(content -> !content.isBlank())
                        .map(content -> truncate(content, MAX_CURATED_CHUNK_CHARS))
                        .ifPresent(approvedContent::add);
            }
        }
        return new OnlineCitationBundle(citations, approvedContent);
    }

    private Optional<URI> onlineSourceUri(Document document) {
        if (document == null || document.getMetadata() == null) {
            return Optional.empty();
        }
        String raw = metadata(document, "sourceUrl", "");
        if (raw.isBlank()) {
            raw = metadata(document, "onlineSourceUrl", "");
        }
        if (raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(URI.create(raw));
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping invalid online RAG source URL metadata: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String withStructuredContext(
            String snippet,
            ReferenceRangeDto referenceRange,
            RetrievalContext retrievalContext
    ) {
        String safeSnippet = snippet == null ? "" : snippet.trim();
        String profileContext = retrievalContext != null && retrievalContext.profileContextAllowed()
                && retrievalContext.profileContextSnippet() != null
                && !retrievalContext.profileContextSnippet().isBlank()
                ? "\nProfile context (access and consent checked): " + retrievalContext.profileContextSnippet().trim()
                : "";
        return safeSnippet + profileContext;
    }

    private String truncate(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
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
        String normalized = Normalizer.normalize(token.toString(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replace("Đ", "D");
        return normalized
                .replaceAll("[^A-Za-z0-9%]", "")
                .toUpperCase(Locale.ROOT);
    }

    private String escapeFilterStringLiteral(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }

    private void recordMetrics(RetrievalTrace trace, long startNanos) {
        meterRegistry.counter("metric.explanation.retrieval.count",
                "source", trace.source(),
                "hit", Boolean.toString(trace.hit()),
                "fallback_path", trace.fallbackPath()).increment();
        DistributionSummary.builder("metric.explanation.retrieval.top_score")
                .tag("source", trace.source())
                .register(meterRegistry)
                .record(trace.topScore());
        Timer.builder("metric.explanation.retrieval.latency")
                .tag("source", trace.source())
                .tag("fallback_path", trace.fallbackPath())
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

    public record RetrievalContext(boolean profileContextAllowed, String profileContextSnippet) {
        public static RetrievalContext none() {
            return new RetrievalContext(false, null);
        }
    }

    public record RetrievalTrace(String source, boolean hit, double topScore, String fallbackPath) {
    }

    public record OnlineCitationMetadata(
            UUID sourceSnapshotId,
            String sourceUrl,
            String publisher,
            Instant retrievedAt,
            String snapshotHash,
            OnlineRagReviewStatus reviewStatus,
            boolean excluded,
            boolean cacheHit,
            boolean usableForAi,
            boolean reviewRequired,
            boolean rejected,
            boolean stale
    ) {
    }

    private record OnlineCitationBundle(
            List<OnlineCitationMetadata> citations,
            List<String> approvedContent
    ) {
        private static OnlineCitationBundle empty() {
            return new OnlineCitationBundle(List.of(), List.of());
        }
    }

    public record RetrievalResult(
            String knowledgeSnippet,
            String source,
            boolean hit,
            double topScore,
            RetrievalTrace trace,
            List<OnlineCitationMetadata> onlineCitations
    ) {
        public RetrievalResult {
            onlineCitations = onlineCitations == null ? List.of() : List.copyOf(onlineCitations);
        }

        public RetrievalResult(String knowledgeSnippet, RetrievalTrace trace) {
            this(knowledgeSnippet, trace, List.of());
        }

        public RetrievalResult(
                String knowledgeSnippet,
                RetrievalTrace trace,
                List<OnlineCitationMetadata> onlineCitations
        ) {
            this(
                    knowledgeSnippet,
                    requireTrace(trace).source(),
                    requireTrace(trace).hit(),
                    requireTrace(trace).topScore(),
                    requireTrace(trace),
                    onlineCitations
            );
        }

        public RetrievalResult(String knowledgeSnippet, String source, boolean hit, double topScore) {
            this(knowledgeSnippet, source, hit, topScore, new RetrievalTrace(source, hit, topScore, FALLBACK_NONE), List.of());
        }

        private static RetrievalTrace requireTrace(RetrievalTrace trace) {
            if (trace == null) {
                throw new IllegalArgumentException("retrieval trace is required");
            }
            return trace;
        }
    }
}
