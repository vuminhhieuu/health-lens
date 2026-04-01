# Story 1.7: Thiết Lập Groq API cho LLM và Embeddings

**Status:** ready-for-dev

**Approved:** Option B+ Architecture (2026-04-01)

> **Thay thế:** Story 1.7 cũ về Ollama Local AI Setup  
> **Lý do:** Option B+ sử dụng Groq API thay vì Ollama local để phù hợp với constraint 16GB RAM

## Execution scope

**Phase 1 — Web MVP:** Story này là infrastructure story, phục vụ cho tất cả Epic 3 (OCR), 4 (LLM), 7 (RAG).

## Story

As a dev team,
I want tích hợp Groq API cho LLM và embeddings,
so that hệ thống có thể generate health explanations mà không cần chạy local LLM.

## Acceptance Criteria

1. **Given** Groq API key được cấu hình, **When** gọi LLM cho health explanation, **Then** hệ thống trả về response từ Groq API với model `qwen-2.5-72b-versatile`.
2. **Given** ứng dụng cần embeddings vector, **When** gọi embedding API, **Then** hệ thống sử dụng `groq/embed-multilingual-v3` model.
3. **Given** Groq API không khả dụng, **When** gọi LLM, **Then** hệ thống fallback sang OpenRouter API hoặc trả về default explanation.
4. **Given** Spring Boot application khởi động, **When** kiểm tra AI configuration, **Then** các beans (ChatModel, EmbeddingModel) được inject đúng cách.

## Tasks / Subtasks

- [ ] Task 1 — Dependencies: Thêm Spring AI Groq dependencies
  - [ ] Thêm `spring-ai-starter-groq` vào `build.gradle.kts`
  - [ ] Thêm `spring-ai-starter-openai` (cho fallback OpenRouter tương thích)
  - [ ] Verify dependencies resolve correctly

- [ ] Task 2 — Configuration: Cấu hình Groq API trong application.yml
  - [ ] Thêm Groq API key vào environment variables
  - [ ] Cấu hình ChatModel với model `qwen-2.5-72b-versatile`
  - [ ] Cấu hình EmbeddingModel với `groq/embed-multilingual-v3`
  - [ ] Cấu hình timeout và retry settings

- [ ] Task 3 — Service: Tạo GroqAiConfig class
  - [ ] Tạo `GroqAiConfig.java` với `@Configuration`
  - [ ] Khai báo `ChatModel` bean cho Groq
  - [ ] Khai báo `EmbeddingModel` bean cho Groq embeddings
  - [ ] Implement multi-provider pattern (Groq primary, OpenRouter fallback)

- [ ] Task 4 — Service: Tạo LlmService với Spring AI ChatClient
  - [ ] Tạo `LlmService.java` sử dụng `ChatClient`
  - [ ] Implement method `generateExplanation(metric, value, status, lang="vi")`
  - [ ] Implement caching strategy (Redis cache key)
  - [ ] Implement retry logic (3 attempts, exponential backoff)

- [ ] Task 5 — Service: Tạo EmbeddingService với Groq
  - [ ] Tạo `EmbeddingService.java`
  - [ ] Implement method `embed(text)` trả về float array
  - [ ] Verify vector dimensions (1024) phù hợp với Qdrant Cloud

- [ ] Task 6 — Tests: Viết unit tests cho Groq integration
  - [ ] `LlmServiceTest`: test generation, caching, fallback
  - [ ] `EmbeddingServiceTest`: test embedding generation
  - [ ] Mock Groq API responses cho offline testing

## Dev Notes

### Architecture: Option B+ AI Layer

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot API                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Spring AI Framework                   │   │
│  │  ┌───────────────┐  ┌────────────────────────┐ │   │
│  │  │  ChatClient   │  │   EmbeddingModel       │ │   │
│  │  └───────┬───────┘  └───────────┬────────────┘ │   │
│  └──────────│──────────────────────│───────────────┘   │
│             │                      │                      │
└─────────────┼──────────────────────┼────────────────────┘
              │                      │
              ▼                      ▼
    ┌─────────────────┐    ┌─────────────────────┐
    │    Groq API     │    │   Groq Embeddings   │
    │  qwen-2.5-72b   │    │ embed-multilingual  │
    └────────┬────────┘    └─────────────────────┘
             │
             │ Fallback
             ▼
    ┌─────────────────┐
    │   OpenRouter    │  (or Claude API)
    │   (backup)      │
    └─────────────────┘
```

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Spring AI - Groq
    implementation("org.springframework.ai:spring-ai-starter-groq")
    
    // Spring AI - OpenAI (for OpenRouter compatibility)
    implementation("org.springframework.ai:spring-ai-starter-openai")
    
    // For VectorStore integration
    implementation("org.springframework.ai:spring-ai-starter-vectorstore-qdrant")
    
    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

### Configuration (application.yml)

```yaml
spring:
  application:
    name: healthlens-api

  # Groq API Configuration
  ai:
    groq:
      api-key: ${GROQ_API_KEY}
      chat:
        options:
          model: qwen-2.5-72b-versatile
          temperature: 0.7
          max-tokens: 500
        endpoint: https://api.groq.com/openai/v1
    
    # Embedding Configuration
    embedding:
      groq:
        options:
          model: groq/embed-multilingual-v3
          dimensions: 1024

# Fallback Configuration
app:
  ai:
    primary: groq
    fallback:
      enabled: true
      provider: openrouter  # or: claude, openai
    retry:
      max-attempts: 3
      initial-delay-ms: 1000
      multiplier: 2.0
```

### Environment Variables (.env)

```bash
# Groq API (Primary LLM)
GROQ_API_KEY=sk-xxxxx

# OpenRouter (Fallback LLM) - optional
OPENROUTER_API_KEY=sk-or-xxxxx

# For VectorStore (Qdrant Cloud)
QDRANT_HOST=cloud.qdrant.io
QDRANT_API_KEY=xxxxx
```

### GroqAiConfig.java

```java
package com.healthlens.api.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.groq.GroqChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.embedding.GroqEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroqAiConfig {

    @Value("${spring.ai.groq.api-key}")
    private String groqApiKey;

    @Bean
    public GroqChatModel groqChatModel() {
        return GroqChatModel.builder()
            .apiKey(groqApiKey)
            .defaultOptions(
                GroqChatOptions.builder()
                    .model("qwen-2.5-72b-versatile")
                    .temperature(0.7f)
                    .maxTokens(500)
                    .build()
            )
            .build();
    }

    @Bean
    public ChatClient groqChatClient(GroqChatModel groqChatModel) {
        return ChatClient.builder(groqChatModel)
            .defaultSystem("You are a helpful health assistant. Respond in Vietnamese.")
            .build();
    }

    @Bean
    public EmbeddingModel groqEmbeddingModel() {
        return GroqEmbeddingModel.builder()
            .apiKey(groqApiKey)
            .dimensions(1024)
            .build();
    }
}
```

### LlmService.java

```java
package com.healthlens.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final ChatClient groqChatClient;
    
    private static final Map<String, String> FALLBACK_EXPLANATIONS = Map.of(
        "Glucose", "Đây là chỉ số đường huyết. Hãy tham khảo bác sĩ.",
        "HbA1c", "Chỉ số này phản ánh đường huyết trung bình 3 tháng.",
        "default", "Kết quả cần được bác sĩ chuyên khoa giải thích thêm."
    );

    public String generateExplanation(String metricName, String value, 
                                     String unit, String status) {
        String prompt = buildPrompt(metricName, value, unit, status);
        
        try {
            return groqChatClient.prompt()
                .user(prompt)
                .call()
                .content();
        } catch (Exception e) {
            log.warn("Groq API failed: {}, returning fallback", e.getMessage());
            return getFallbackExplanation(metricName);
        }
    }

    private String buildPrompt(String metricName, String value, 
                               String unit, String status) {
        return String.format("""
            Bạn là trợ lý sức khỏe. Giải thích kết quả xét nghiệm sau bằng tiếng Việt đơn giản.
            Tối đa 3 câu. Không dùng thuật ngữ y khoa phức tạp.

            Chỉ số: %s
            Giá trị: %s %s
            Trạng thái: %s
            """, metricName, value, unit, status);
    }

    private String getFallbackExplanation(String metricName) {
        return FALLBACK_EXPLANATIONS.getOrDefault(metricName, 
            FALLBACK_EXPLANATIONS.get("default"));
    }
}
```

### EmbeddingService.java

```java
package com.healthlens.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public float[] embed(String text) {
        try {
            EmbeddingResponse response = embeddingModel.embed(
                List.of(text)
            );
            return response.getResult().getEmbedding();
        } catch (Exception e) {
            log.error("Embedding failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
}
```

### Groq Available Models

| Model | Context | Vietnamese | Use Case |
|-------|---------|------------|----------|
| `qwen-2.5-72b-versatile` | 128K | ✅ Excellent | Health explanations |
| `llama-3.3-70b-versatile` | 128K | ⚠️ Good | Complex reasoning |
| `mixtral-8x7b-32768` | 32K | ⚠️ Limited | Fast responses |
| `gemma2-9b-it` | 8K | ⚠️ Limited | Simple tasks |

### Unit Test Example

```java
package com.healthlens.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LlmServiceTest {

    @Autowired
    private LlmService llmService;

    @Test
    void shouldGenerateExplanation() {
        String result = llmService.generateExplanation(
            "Glucose", "5.4", "mmol/L", "normal"
        );
        
        assertNotNull(result);
        assertTrue(result.length() > 10);
        assertTrue(result.contains("đường") || result.contains("huyết"));
    }

    @Test
    void shouldReturnFallbackOnError() {
        // Simulate API failure - should return fallback
        String result = llmService.generateExplanation(
            "UnknownMetric", "999", "unit", "unknown"
        );
        
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }
}
```

### References

- [Source: architecture.md#ADR-001-Cloud-First-AI]
- [Source: technical-option-bplus-feasibility-2026-04-01.md]
- [Spring AI Groq Reference](https://docs.spring.io/spring-ai/reference/api/chat/groq-chat.html)
- [Groq API Documentation](https://console.groq.com/docs)

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
| `application.yml` | Add Groq config |
| `GroqAiConfig.java` | Create |
| `LlmService.java` | Create |
| `EmbeddingService.java` | Create |
| `LlmServiceTest.java` | Create |
| `EmbeddingServiceTest.java` | Create |

