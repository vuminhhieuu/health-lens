package com.healthlens.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;

    public void upsertReferenceData(String id, String content, 
                                    Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = new HashMap<>();
        if (metadata != null) {
            safeMetadata.putAll(metadata);
        }
        safeMetadata.putIfAbsent("id", id);

        Document document = Document.builder()
            .id(id)
            .text(content)
            .metadata(safeMetadata)
            .build();
        
        vectorStore.add(List.of(document));
        log.info("Upserted document {} to vector store", id);
    }

    public void upsertDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        vectorStore.add(documents);
        log.info("Upserted {} documents to vector store", documents.size());
    }

    public List<Document> semanticSearch(String query, int topK) {
        return semanticSearch(query, topK, null);
    }

    public List<Document> semanticSearch(String query, int topK, String filterExpression) {
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .filterExpression(filterExpression)
            .build();
        
        List<Document> results = vectorStore.similaritySearch(request);
        log.info("Semantic search for '{}' returned {} results", query, results.size());
        
        return results;
    }

    public void deleteDocument(String id) {
        vectorStore.delete(List.of(id));
        log.info("Deleted document {} from vector store", id);
    }

    public Optional<Document> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        // Qdrant doesn't support direct ID lookup
        // Search with exact ID in metadata (safe escaped filter expression)
        SearchRequest request = SearchRequest.builder()
            .query("")
            .filterExpression(buildMetadataIdFilterExpression(id))
            .topK(1)
            .build();
        
        List<Document> results = vectorStore.similaritySearch(request);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private String buildMetadataIdFilterExpression(String id) {
        return "id == '" + escapeFilterStringLiteral(id) + "'";
    }

    private String escapeFilterStringLiteral(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'");
    }
}
