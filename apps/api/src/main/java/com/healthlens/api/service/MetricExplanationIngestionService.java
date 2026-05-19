package com.healthlens.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MetricExplanationIngestionService {

    private final ObjectMapper objectMapper;
    private final VectorStoreService vectorStoreService;
    private final RagCorpusGovernanceService governanceService;
    private final String embeddingModel;
    private final int embeddingDimension;
    private final Clock clock;

    @Autowired
    public MetricExplanationIngestionService(
            ObjectMapper objectMapper,
            VectorStoreService vectorStoreService,
            RagCorpusGovernanceService governanceService,
            @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}") String embeddingModel,
            @Value("${spring.ai.vectorstore.qdrant.vector-dimension:1536}") int embeddingDimension
    ) {
        this(objectMapper, vectorStoreService, governanceService, embeddingModel, embeddingDimension, Clock.systemUTC());
    }

    MetricExplanationIngestionService(
            ObjectMapper objectMapper,
            VectorStoreService vectorStoreService,
            RagCorpusGovernanceService governanceService,
            String embeddingModel,
            int embeddingDimension,
            Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.vectorStoreService = vectorStoreService;
        this.governanceService = governanceService;
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        this.clock = clock;
    }

    public int ingest(Resource resource, String sourceVersion) {
        IngestionReport report = ingestWithReport(resource, sourceVersion, "system-ingestion");
        return report.hasErrors() ? 0 : report.chunkCount();
    }

    public IngestionReport ingestWithReport(Resource resource, String sourceVersion, String reviewer) {
        String normalizedSourceVersion;
        try {
            normalizedSourceVersion = normalizeSourceVersion(sourceVersion);
            governanceService.validateNewSourceVersion(normalizedSourceVersion);
        } catch (Exception ex) {
            return new IngestionReport(sourceVersion, 0, embeddingModel, embeddingDimension, List.of(ex.getMessage()), reviewer, Instant.now(clock));
        }

        Instant effectiveDate = Instant.now(clock);
        List<String> errors = new ArrayList<>();
        List<MetricKnowledgeChunk> chunks;
        try {
            chunks = loadChunks(resource);
        } catch (Exception ex) {
            errors.add(ex.getMessage());
            return new IngestionReport(normalizedSourceVersion, 0, embeddingModel, embeddingDimension, List.copyOf(errors), reviewer, effectiveDate);
        }
        if (chunks.isEmpty()) {
            errors.add("Metric explanation corpus is empty");
            return new IngestionReport(normalizedSourceVersion, 0, embeddingModel, embeddingDimension, List.copyOf(errors), reviewer, effectiveDate);
        }

        List<Document> documents = new ArrayList<>();
        for (MetricKnowledgeChunk chunk : chunks) {
            documents.add(toDocument(chunk, normalizedSourceVersion, reviewer, effectiveDate));
        }
        try {
            vectorStoreService.upsertDocuments(documents);
            IngestionReport report = new IngestionReport(
                    normalizedSourceVersion,
                    documents.size(),
                    embeddingModel,
                    embeddingDimension,
                    List.copyOf(errors),
                    reviewer,
                    effectiveDate
            );
            governanceService.recordPendingIngestion(report);
            return report;
        } catch (Exception ex) {
            errors.add(ex.getMessage());
            log.warn("metric_explanation_ingestion failed sourceVersion={} error={}", normalizedSourceVersion, ex.getMessage());
            return new IngestionReport(normalizedSourceVersion, 0, embeddingModel, embeddingDimension, List.copyOf(errors), reviewer, effectiveDate);
        }
    }

    private List<MetricKnowledgeChunk> loadChunks(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load metric explanation curated data", ex);
        }
    }

    private Document toDocument(MetricKnowledgeChunk chunk, String sourceVersion, String reviewer, Instant effectiveDate) {
        String metricKey = chunk.metricKey() == null ? "unknown" : chunk.metricKey().trim().toUpperCase(Locale.ROOT);
        String stableIdSource = "metric-expl:" + metricKey + ":" + sourceVersion;
        String id = UUID.nameUUIDFromBytes(stableIdSource.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("metricKey", metricKey);
        metadata.put("aliases", chunk.aliases() == null ? List.of() : chunk.aliases());
        metadata.put("language", normalizeLanguage(chunk.language()));
        metadata.put("whatIsIt", chunk.whatIsIt());
        metadata.put("relatedTo", chunk.relatedTo());
        metadata.put("impactWhenOutOfRange", chunk.impactWhenOutOfRange());
        metadata.put("sourceVersion", sourceVersion);
        metadata.put("documentKey", stableIdSource);
        metadata.put("approvalStatus", "pending_review");
        metadata.put("reviewer", reviewer);
        metadata.put("effectiveDate", effectiveDate.toString());
        metadata.put("embeddingModel", embeddingModel);
        metadata.put("embeddingDimension", embeddingDimension);

        String content = """
                Metric key: %s
                Aliases: %s
                What is it: %s
                Related to: %s
                Impact when out of range: %s
                """.formatted(
                metricKey,
                String.join(", ", chunk.aliases() == null ? List.of() : chunk.aliases()),
                chunk.whatIsIt(),
                chunk.relatedTo(),
                chunk.impactWhenOutOfRange()
        );

        return Document.builder()
                .id(id)
                .text(content)
                .metadata(metadata)
                .build();
    }

    private String normalizeSourceVersion(String sourceVersion) {
        if (sourceVersion == null || sourceVersion.isBlank()) {
            throw new IllegalArgumentException("RAG corpus sourceVersion is required");
        }
        return sourceVersion.trim();
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "vi";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    public record MetricKnowledgeChunk(
            String metricKey,
            List<String> aliases,
            String language,
            String whatIsIt,
            String relatedTo,
            String impactWhenOutOfRange
    ) {
    }

    public record IngestionReport(
            String sourceVersion,
            int chunkCount,
            String embeddingModel,
            int embeddingDimension,
            List<String> errors,
            String reviewer,
            Instant effectiveDate
    ) {
        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }
}
