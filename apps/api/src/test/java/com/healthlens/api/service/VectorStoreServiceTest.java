package com.healthlens.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorStoreServiceTest {

    @Mock
    private VectorStore vectorStore;

    private VectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        vectorStoreService = new VectorStoreService(vectorStore);
    }

    @Test
    void shouldUpsertDocument() {
        String id = "ref-1";
        String content = "Glucose: 3.9-5.6 mmol/L";
        Map<String, Object> metadata = Map.of("type", "metric");

        vectorStoreService.upsertReferenceData(id, content, metadata);

        verify(vectorStore).add(argThat((List<Document> docs) -> 
            docs.size() == 1 && 
            docs.get(0).getId().equals(id) &&
            docs.get(0).getText().equals(content)
        ));
    }

    @Test
    void shouldSearchAndReturnResults() {
        Document mockDoc = Document.builder()
            .id("ref-1")
            .text("Glucose reference data")
            .metadata(Map.of("type", "metric"))
            .build();
        
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockDoc));

        List<Document> results = vectorStoreService.semanticSearch("glucose", 5);

        assertEquals(1, results.size());
        assertEquals("ref-1", results.get(0).getId());
    }

    @Test
    void shouldDeleteDocument() {
        vectorStoreService.deleteDocument("ref-1");

        verify(vectorStore).delete(List.of("ref-1"));
    }

    @Test
    void shouldFindById() {
        Document mockDoc = Document.builder()
            .id("ref-1")
            .text("Glucose reference data")
            .metadata(Map.of("type", "metric"))
            .build();
        
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockDoc));

        Optional<Document> result = vectorStoreService.findById("ref-1");

        assertTrue(result.isPresent());
        assertEquals("ref-1", result.get().getId());
    }
}
