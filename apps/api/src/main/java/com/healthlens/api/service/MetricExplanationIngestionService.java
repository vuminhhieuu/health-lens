package com.healthlens.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricExplanationIngestionService {

    private final ObjectMapper objectMapper;
    private final VectorStoreService vectorStoreService;

    public int ingest(Resource resource, String sourceVersion) {
        List<MetricKnowledgeChunk> chunks = loadChunks(resource);
        List<Document> documents = new ArrayList<>();
        for (MetricKnowledgeChunk chunk : chunks) {
            documents.add(toDocument(chunk, sourceVersion));
        }
        vectorStoreService.upsertDocuments(documents);
        return documents.size();
    }

    private List<MetricKnowledgeChunk> loadChunks(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load metric explanation curated data", ex);
        }
    }

    private Document toDocument(MetricKnowledgeChunk chunk, String sourceVersion) {
        String metricKey = chunk.metricKey() == null ? "unknown" : chunk.metricKey().trim().toUpperCase(Locale.ROOT);
        String stableIdSource = "metric-expl:" + metricKey + ":" + sourceVersion;
        String id = UUID.nameUUIDFromBytes(stableIdSource.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("metricKey", metricKey);
        metadata.put("aliases", chunk.aliases() == null ? List.of() : chunk.aliases());
        metadata.put("language", chunk.language() == null ? "vi" : chunk.language());
        metadata.put("whatIsIt", chunk.whatIsIt());
        metadata.put("relatedTo", chunk.relatedTo());
        metadata.put("impactWhenOutOfRange", chunk.impactWhenOutOfRange());
        metadata.put("sourceVersion", sourceVersion);
        metadata.put("documentKey", stableIdSource);

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

    public record MetricKnowledgeChunk(
            String metricKey,
            List<String> aliases,
            String language,
            String whatIsIt,
            String relatedTo,
            String impactWhenOutOfRange
    ) {
    }
}
