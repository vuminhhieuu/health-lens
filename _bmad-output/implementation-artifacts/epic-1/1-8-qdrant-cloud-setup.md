# Story 1.8: Thiết Lập Qdrant Cloud cho Vector Database

**Status:** ready-for-dev

**Approved:** Option B+ Architecture (2026-04-01)

> **Thay thế:** Story 1.8 cũ về Qdrant Vector DB Setup (self-hosted)  
> **Lý do:** Option B+ sử dụng Qdrant Cloud (managed) thay vì self-hosted để giảm resource usage

## Execution scope

**Phase 1 — Web MVP:** Story này phục vụ cho Epic 7 (Reference Data RAG) và Epic 4 (LLM context retrieval).

## Story

As a dev team,
I want tích hợp Qdrant Cloud cho vector storage,
so that hệ thống có thể store và search embeddings mà không cần maintain local Qdrant instance.

## Acceptance Criteria

1. **Given** Qdrant Cloud cluster được tạo, **When** ứng dụng khởi động, **Then** kết nối thành công đến Qdrant Cloud qua REST API.
2. **Given** collection `healthlens` chưa tồn tại, **When** ứng dụng khởi động, **Then** tự động tạo collection với 1024 dimensions.
3. **Given** vector data cần được lưu, **When** gọi VectorStore, **Then** vector được upsert vào Qdrant Cloud.
4. **Given** semantic search query, **When** tìm kiếm, **Then** Qdrant Cloud trả về kết quả phù hợp với similarity score.

## Tasks / Subtasks

- [ ] Task 1 — Infrastructure: Tạo Qdrant Cloud cluster
  - [ ] Đăng ký tài khoản Qdrant Cloud (https://cloud.qdrant.io/)
  - [ ] Tạo free tier cluster (1 GB storage)
  - [ ] Lấy cluster URL và API key từ dashboard
  - [ ] Tạo collection `healthlens` với cấu hình phù hợp

- [ ] Task 2 — Dependencies: Thêm Qdrant dependencies
  - [ ] Thêm `spring-ai-starter-vectorstore-qdrant` vào `build.gradle.kts`
  - [ ] Verify dependencies resolve correctly

- [ ] Task 3 — Configuration: Cấu hình Qdrant Cloud
  - [ ] Thêm QDRANT_HOST, QDRANT_API_KEY vào environment
  - [ ] Cấu hình collection name trong application.yml
  - [ ] Cấu hình timeout và retry settings

- [ ] Task 4 — Service: Tạo VectorStoreService
  - [ ] Tạo `VectorStoreService.java` sử dụng Spring AI VectorStore
  - [ ] Implement method `upsertDocument(id, content, metadata)`
  - [ ] Implement method `semanticSearch(query, topK)` trả về documents
  - [ ] Implement method `deleteDocument(id)`

- [ ] Task 5 — Integration: Tích hợp với Reference Data (Epic 7)
  - [ ] Tạo endpoint để index reference data vào vector store
  - [ ] Implement automatic embedding khi reference data được tạo/cập nhật
  - [ ] Test retrieval với sample queries

- [ ] Task 6 — Tests: Viết unit tests
  - [ ] `VectorStoreServiceTest`: test upsert, search, delete
  - [ ] Mock Qdrant client cho offline testing

## Dev Notes

### Architecture: Vector Storage in Option B+

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot API                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │           VectorStoreService                      │   │
│  │  ┌─────────────────────────────────────────┐   │   │
│  │  │     Spring AI VectorStore Abstraction    │   │   │
│  │  └─────────────────┬───────────────────────┘   │   │
│  └───────────────────│───────────────────────────┘   │
│                        │                                │
└────────────────────────┼────────────────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │   Qdrant Cloud     │
              │   (Managed)        │
              │   ┌─────────────┐  │
              │   │ healthlens  │  │
              │   │ collection  │  │
              │   │ 1024 dims   │  │
              │   └─────────────┘  │
              └─────────────────────┘
```

### Qdrant Cloud Setup Steps

**1. Đăng ký Qdrant Cloud:**
```
1. Truy cập https://cloud.qdrant.io/
2. Sign up với GitHub hoặc email
3. Verify email và login
```

**2. Tạo Cluster:**
```
1. Click "Create Cluster"
2. Chọn region gần nhất (Singapore hoặc US)
3. Chọn plan "Sandbox" (free tier)
4. Đặt tên: healthlens
5. Click "Create"
```

**3. Lấy Credentials:**
```
- Host: https://xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.cloud.qdrant.io
- API Key: qdrant_api_key_xxxx
```

**4. Tạo Collection (Dashboard hoặc API):**
```bash
curl -X PUT "https://<host>/collections/healthlens" \
  -H "api-key: <api-key>" \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": {
      "size": 1024,
      "distance": "Cosine"
    },
    "optimizers": {
      "default_segment_number": 2
    }
  }'
```

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Spring AI - Qdrant VectorStore
    implementation("org.springframework.ai:spring-ai-starter-vectorstore-qdrant")
    
    // For embedding integration
    implementation("org.springframework.ai:spring-ai-starter-embedding")
}
```

### Configuration (application.yml)

```yaml
spring:
  application:
    name: healthlens-api

  # Qdrant Cloud Configuration
  ai:
    vectorstore:
      qdrant:
        host: ${QDRANT_HOST}
        api-key: ${QDRANT_API_KEY}
        collection-name: healthlens
        timeout: 30s
        vector-dimension: 1024

# Environment variables
# QDRANT_HOST=https://xxxxx.cloud.qdrant.io
# QDRANT_API_KEY=qdrant_api_key_xxxx
```

### QdrantVectorStoreConfig.java

```java
package com.healthlens.api.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.QdrantVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantVectorStoreConfig {

    @Value("${spring.ai.vectorstore.qdrant.host}")
    private String host;

    @Value("${spring.ai.vectorstore.qdrant.api-key}")
    private String apiKey;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(embeddingModel)
            .host(host)
            .apiKey(apiKey)
            .collectionName(collectionName)
            .build();
    }
}
```

### VectorStoreService.java

```java
package com.healthlens.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.documents.Document;
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
            .query("id:" + id)
            .topK(1)
            .build();
        
        List<Document> results = vectorStore.similaritySearch(request);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
```

### REST Endpoints (ReferenceDataController)

```java
@RestController
@RequestMapping("/api/v1/reference-data")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final VectorStoreService vectorStoreService;
    private final ReferenceDataService referenceDataService;

    @PostMapping("/{id}/index")
    public ResponseEntity<Void> indexToVectorStore(@PathVariable String id) {
        ReferenceData data = referenceDataService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reference data not found"));
        
        String content = formatForEmbedding(data);
        Map<String, Object> metadata = Map.of(
            "type", data.getType(),
            "name", data.getName(),
            "id", id
        );
        
        vectorStoreService.upsertReferenceData(id, content, metadata);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ReferenceDataSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        
        List<Document> docs = vectorStoreService.semanticSearch(query, topK);
        
        List<ReferenceDataSearchResult> results = docs.stream()
            .map(doc -> new ReferenceDataSearchResult(
                doc.getId(),
                doc.getText(),
                doc.getMetadata()
            ))
            .toList();
        
        return ResponseEntity.ok(results);
    }

    private String formatForEmbedding(ReferenceData data) {
        return String.format("""
            Tên chỉ số: %s
            Giá trị bình thường: %s - %s %s
            Ý nghĩa: %s
            """,
            data.getName(),
            data.getMinValue(), data.getMaxValue(), data.getUnit(),
            data.getDescriptionVi()
        );
    }
}
```

### Unit Test Example

```java
package com.healthlens.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.documents.Document;

import java.util.List;
import java.util.Map;

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
        
        when(vectorStore.similaritySearch(any())).thenReturn(List.of(mockDoc));

        List<Document> results = vectorStoreService.semanticSearch("glucose", 5);

        assertEquals(1, results.size());
        assertEquals("ref-1", results.get(0).getId());
    }
}
```

### Collection Schema (Qdrant Cloud)

```json
{
  "vectors": {
    "size": 1024,
    "distance": "Cosine"
  },
  "optimizers": {
    "default_segment_number": 2
  },
  "on_disk_payload": false
}
```

**Vector Settings:**
- **Size:** 1024 (matches Groq embed-multilingual-v3 dimensions)
- **Distance:** Cosine (best for semantic similarity)
- **Segment Number:** 2 (for free tier performance)

### Monitoring Qdrant Cloud

**Dashboard Metrics:**
- Collection size
- Number of vectors
- Search requests per second
- Memory usage

**Alerts:**
- Set up email alerts for >80% storage usage
- Monitor failed requests

### References

- [Source: architecture.md#ADR-004-Qdrant-Cloud]
- [Source: technical-option-bplus-feasibility-2026-04-01.md]
- [Qdrant Cloud Documentation](https://qdrant.tech/documentation/cloud/)
- [Spring AI Qdrant Reference](https://docs.spring.io/spring-ai/reference/api/vectorstore.html)

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

_[To be filled during implementation]_

### Completion Notes List

_[To be filled upon completion]_

### File List

| File | Action |
|------|--------|
| `build.gradle.kts` | Add dependencies |
| `application.yml` | Add Qdrant config |
| `QdrantVectorStoreConfig.java` | Create |
| `VectorStoreService.java` | Create |
| `ReferenceDataController.java` | Update (add endpoints) |
| `VectorStoreServiceTest.java` | Create |

