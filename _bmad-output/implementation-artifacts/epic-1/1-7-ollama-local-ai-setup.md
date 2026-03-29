# Story 1.7: Thiết lập Ollama Local AI cho LLM và Embeddings

Status: backlog

## Execution scope

**Phase 1 — MVP:** Story này setup local AI infrastructure cho OCR, LLM và embeddings. Phải hoàn thành **trước** khi dev OCR (3.x) và LLM stories (4.x).

## Story

As a nhóm phát triển sản phẩm,
I want thiết lập Ollama với Qwen 3.5 và nomic-embed-text,
so that ứng dụng có thể xử lý OCR và tạo giải thích **không tốn chi phí API** trong giai đoạn MVP.

## Acceptance Criteria

1. **Given** server với GPU (8GB VRAM), **When** cài đặt Ollama, **Then** Ollama service khởi động thành công và API responsive tại `localhost:11434`.
2. **Given** Ollama đã cài, **When** pull Qwen 3.5 model, **Then** model được download và inference hoạt động với response time < 30s cho health explanation prompts.
3. **Given** Ollama đã cài, **When** pull nomic-embed-text model, **Then** embeddings API hoạt động và trả về vectors 768-dimension.
4. **Given** Docker environment, **When** chạy docker-compose, **Then** Ollama container được start cùng PostgreSQL, Redis, và MinIO.
5. **Given** backend service, **When** gọi `/api/v1/ai/generate`, **Then** request được forward đến Ollama và trả về response hợp lệ.

## Tasks / Subtasks

- [ ] Task 1 — Ollama Installation (AC: #1)
  - [ ] Cài Ollama trên server: `curl -fsSL https://ollama.com/install.sh | sh`
  - [ ] Hoặc sử dụng Docker image: `docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama`
  - [ ] Verify: `curl http://localhost:11434/api/tags`
- [ ] Task 2 — Qwen 3.5 Model Setup (AC: #2)
  - [ ] Pull model: `ollama pull qwen3.5:7b`
  - [ ] Test inference: `ollama run qwen3.5:7b "Explain glucose 5.4 mmol/L in simple Vietnamese"`
  - [ ] Benchmark response time
- [ ] Task 3 — Nomic Embed Text Model Setup (AC: #3)
  - [ ] Pull embedding model: `ollama pull nomic-embed-text`
  - [ ] Test embeddings API: `curl -X POST http://localhost:11434/api/embeddings -d '{"model":"nomic-embed-text","prompt":"blood glucose test"}'`
  - [ ] Verify output dimension: 768
- [ ] Task 4 — Docker Compose Integration (AC: #4)
  - [ ] Thêm Ollama service vào `docker-compose.dev.yml`
  - [ ] Configure volume mount cho model persistence
  - [ ] Environment variables: `OLLAMA_HOST`, `OLLAMA_MODELS`
- [ ] Task 5 — Backend OllamaClient (AC: #5)
  - [ ] Tạo `OllamaClient.java` trong `config/`
  - [ ] Implement `generate(prompt)` method với HTTP client
  - [ ] Implement `embed(text)` method cho embeddings
  - [ ] Test integration với backend service
- [ ] Task 6 — Health Check Endpoint
  - [ ] `GET /api/v1/ai/health` → check Ollama connectivity
  - [ ] Response: `{ "ollama": "connected", "models": ["qwen3.5:7b", "nomic-embed-text"] }`

## Dev Notes

### Ollama API Endpoints

```
# Check status
GET http://localhost:11434/api/tags

# Generate text
POST http://localhost:11434/api/generate
Body: { "model": "qwen3.5:7b", "prompt": "...", "stream": false }

# Generate embeddings
POST http://localhost:11434/api/embeddings
Body: { "model": "nomic-embed-text", "prompt": "..." }
```

### Ollama Java Client

```java
// OllamaClient.java
@Service
public class OllamaClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    public String generate(String model, String prompt) {
        Map<String, Object> body = Map.of(
            "model", model,
            "prompt", prompt,
            "stream", false
        );
        // Call Ollama API
    }
    
    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
            "model", "nomic-embed-text",
            "prompt", text
        );
        // Call Ollama embeddings API
    }
}
```

### Docker Compose Service

```yaml
# docker-compose.dev.yml
services:
  ollama:
    image: ollama/ollama:latest
    container_name: healthlens-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]
    # Fallback: CPU-only nếu không có GPU
    # environment:
    #   - OLLAMA_HOST=0.0.0.0

volumes:
  ollama-data:
```

### GPU Requirements

| Model | VRAM | Use Case |
|-------|------|----------|
| Qwen 3.5 7B | 8GB | LLM inference |
| nomic-embed-text | 2GB | Embeddings |

**Note:** Nếu không có GPU, Qwen 3.5 có thể chạy trên CPU nhưng sẽ chậm hơn (30-60s thay vì 5-10s).

### Hardware Recommendations

- **MVP (Local dev):** MacBook M1/M2/M3 với unified memory (khuyến nghị 16GB+)
- **Production:** Server với NVIDIA GPU (RTX 3080+ hoặc A100)
- **Budget option:** CPU-only server cho dev, dùng cloud fallback cho production

### References

- [Source: architecture.md#ADR-001-Local-First-AI]
- [Source: architecture.md#Tích-Hợp-Dịch-Vụ-Bên-Ngoài]
- [Ollama Documentation](https://github.com/ollama/ollama)
- [Qwen 3.5 on Ollama](https://ollama.com/library/qwen3.5)

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
