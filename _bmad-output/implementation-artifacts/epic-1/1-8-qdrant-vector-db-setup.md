# Story 1.8: Thiết lập Qdrant Vector Database cho Semantic Search

Status: backlog

## Execution scope

**Phase 1 — MVP:** Story này setup Qdrant vector database cho RAG pipeline. Cần hoàn thành **trước** khi dev RAG features.

## Story

As a nhóm phát triển sản phẩm,
I want thiết lập Qdrant vector database,
so that hệ thống có thể lưu trữ embeddings và thực hiện semantic search cho RAG pipeline.

## Acceptance Criteria

1. **Given** Docker environment, **When** chạy Qdrant container, **Then** Qdrant service khởi động thành công và API responsive tại port 6333.
2. **Given** Qdrant đã chạy, **When** tạo collection "health_metrics", **Then** collection được tạo với vector size 768 và HNSW index.
3. **Given** collection đã tạo, **When** upsert embeddings, **Then** vectors được lưu và search hoạt động với recall > 0.9.
4. **Given** backend, **When** gọi VectorService, **Then** embeddings được upserted và retrieved từ Qdrant.
5. **Given** Docker Compose, **When** start services, **Then** Qdrant, Ollama, PostgreSQL, Redis đều healthy.

## Tasks / Subtasks

- [ ] Task 1 — Qdrant Docker Setup (AC: #1)
  - [ ] Thêm Qdrant service vào `docker-compose.dev.yml`
  - [ ] Configure persistent storage
  - [ ] Map ports: 6333 (API), 6334 (gRPC)
- [ ] Task 2 — Qdrant Collection Setup (AC: #2)
  - [ ] Tạo collection "health_metrics" với:
    - Vector size: 768 (nomic-embed-text output)
    - Distance: Cosine
    - Index: HNSW (m=16, ef_construct=200)
  - [ ] Create payload indexes cho filtering
- [ ] Task 3 — VectorService Implementation (AC: #4)
  - [ ] Tạo `VectorService.java` trong service layer
  - [ ] Implement `upsert(collection, id, vector, payload)` method
  - [ ] Implement `search(collection, queryVector, limit, filter)` method
  - [ ] Implement `delete(collection, id)` method
- [ ] Task 4 — Health Check & Admin Endpoints (AC: #1)
  - [ ] `GET /api/v1/vector/health` → check Qdrant connectivity
  - [ ] `GET /api/v1/vector/collections` → list collections
  - [ ] `POST /api/v1/vector/collections` → create collection (admin)

## Dev Notes

### Qdrant Docker Configuration

```yaml
# docker-compose.dev.yml
services:
  qdrant:
    image: qdrant/qdrant:latest
    container_name: healthlens-qdrant
    ports:
      - "6333:6333"  # REST API
      - "6334:6334"  # gRPC
    volumes:
      - qdrant-data:/qdrant/storage
    environment:
      - QDRANT__SERVICE__GRPC_PORT=6334

volumes:
  qdrant-data:
```

### Qdrant REST API

```bash
# Check health
curl http://localhost:6333/collections

# Create collection
curl -X PUT http://localhost:6333/collections/health_metrics \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": {
      "size": 768,
      "distance": "Cosine"
    },
    "hnsw_config": {
      "m": 16,
      "ef_construct": 200
    }
  }'

# Upsert point
curl -X PUT http://localhost:6333/collections/health_metrics/points \
  -H "Content-Type: application/json" \
  -d '{
    "points": [
      {
        "id": 1,
        "vector": [0.1, 0.2, ...],
        "payload": {
          "metric_name": "Glucose",
          "description": "Blood sugar level",
          "reference_range": "3.9-5.6 mmol/L"
        }
      }
    ]
  }'

# Search
curl -X POST http://localhost:6333/collections/health_metrics/points/search \
  -H "Content-Type: application/json" \
  -d '{
    "vector": [0.1, 0.2, ...],
    "limit": 5,
    "with_payload": true
  }'
```

### VectorService Java Implementation

```java
@Service
public class VectorService {
    private final QdrantClient qdrantClient;
    private final OllamaClient ollamaClient;
    
    public void indexHealthMetric(HealthMetric metric, String text) {
        // Generate embedding
        float[] embedding = ollamaClient.embed(text);
        
        // Upsert to Qdrant
        PointStruct point = PointStruct.of(
            metric.getId().toString(),
            embedding,
            Map.of(
                "metric_name", metric.getName(),
                "description", metric.getDescription(),
                "reference_range", metric.getReferenceRange()
            )
        );
        qdrantClient.upsert(
            ScrollPoints.builder()
                .collectionName("health_metrics")
                .point(Collections.singletonList(point))
                .build()
        );
    }
    
    public List<HealthMetric> semanticSearch(String query, int limit) {
        // Generate query embedding
        float[] queryVector = ollamaClient.embed(query);
        
        // Search Qdrant
        SearchPoints searchResult = SearchPoints.builder()
            .collectionName("health_metrics")
            .vector(queryVector)
            .limit(limit)
            .withPayload(true)
            .build();
        
        // Return matched metrics
    }
}
```

### Collection Schema

```json
{
  "name": "health_metrics",
  "vectors": {
    "size": 768,
    "distance": "Cosine"
  },
  "hnsw_config": {
    "m": 16,
    "ef_construct": 200
  },
  "payload_schema": {
    "metric_name": { "type": "keyword" },
    "description": { "type": "text" },
    "reference_range": { "type": "text" },
    "created_at": { "type": "datetime" }
  }
}
```

### Use Cases for Vector Search

1. **Medical term search** — Search "đường huyết" → find "Glucose" metric
2. **Symptom-based search** — Search "mệt mỏi" → find related metrics
3. **RAG for explanations** — Retrieve relevant context for LLM

### References

- [Source: architecture.md#ADR-002-Qdrant-for-Vector-Storage]
- [Source: architecture.md#Tích-Hợp-Dịch-Vụ-Bên-Ngoài]
- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [Qdrant Java Client](https://github.com/qdrant/qdrant-java)

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
