package com.healthlens.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

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
        Document document = Document.builder()
            .id(id)
            .text(content)
            .metadata(metadata)
            .build();
        
        vectorStore.add(List.of(document));
        log.info("Upserted document {} to vector store", id);
    }

    public List<Document> semanticSearch(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
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
        // Qdrant doesn't support direct ID lookup
        // Search with exact ID in metadata
        SearchRequest request = SearchRequest.builder()
            .query("")
            .filterExpression("id == '" + id + "'")
            .topK(1)
            .build();
        
        List<Document> results = vectorStore.similaritySearch(request);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
