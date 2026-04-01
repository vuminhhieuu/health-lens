---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/research/technical-ocr-llms-solution-research-2026-03-29.md
workflowType: 'research'
lastStep: 4
research_type: 'technical'
research_topic: 'local-dev-constraints'
research_goals: 'Analyze RAM requirements and propose solutions for local development on 16GB machines'
user_name: 'BMAD Architect'
date: '2026-04-01'
web_research_enabled: true
source_verification: true
---

# Research Report: Local Development Constraints for HealthLens

**Date:** 2026-04-01
**Author:** BMAD Architect
**Research Type:** Technical
**Status:** Superseded by Option B+ - Decision Made

> ✅ **RESOLUTION:** Option B+ (Fully Cloud) selected on 2026-04-01
> See `technical-option-bplus-analysis-2026-04-01.md` and `technical-option-bplus-feasibility-2026-04-01.md` for final decisions.

---

## Research Overview

### Problem Statement
The previously selected Local-First AI architecture (PaddleOCR + Ollama + Qdrant + nomic-embed-text) requires significant RAM resources that exceed typical developer laptops (16GB). This research analyzes actual memory requirements and proposes viable alternatives.

### Research Goals
1. Quantify RAM requirements for each service
2. Identify solutions for 16GB RAM constraint
3. Propose hybrid/local/cloud options with trade-offs

---

## Memory Requirements Analysis

### Current Architecture Services

| Service | Original Spec | Min RAM | Typical RAM | Notes |
|---------|---------------|---------|-------------|-------|
| Ollama + Qwen 3.5 7B Q4 | Local LLM | 4-6 GB | 6-8 GB | Quantized model |
| Qdrant Vector DB | Self-hosted | 1-2 GB | 2-4 GB | In-memory vectors |
| PostgreSQL | Database | 256 MB | 512 MB-2 GB | Tunable |
| Redis | Cache/Sessions | 64 MB | 128-512 MB | Optional |
| PaddleOCR | OCR Engine | 2-4 GB | 3-5 GB | Heavy model |
| nomic-embed-text | Embeddings | 1-2 GB | 1.5-2.5 GB | Running via Ollama |
| Spring Boot API | Backend | 512 MB | 1-2 GB | JVM heap |
| Next.js Web | Frontend | 256 MB | 512 MB-1 GB | Dev mode |
| OS + Tools | System | 2-4 GB | 3-5 GB | Chrome, IDE, etc. |

**Total Estimated (All Running):** 14-31 GB
**16GB Laptop Reality:** Will swap heavily, extremely slow, possibly crash

---

## Solution Options

### Option A: Lightweight Local Stack ✅ Recommended for Dev

**Changes:**
- Qwen 3.5 7B → **Qwen 2.5 3B Q4** (2-3 GB RAM)
- PaddleOCR → **Tesseract 5 + EasyOCR fallback** (< 500 MB)
- Qdrant → **ChromaDB** or **SQLite + vec0** (< 100 MB)
- Embedding: Use API-based or **miniLM-l6-v2** (< 100 MB)
- Redis: Remove (use PostgreSQL pub/sub or skip)

**Estimated RAM:**
| Service | RAM |
|---------|-----|
| Ollama + Qwen 2.5 3B | 2-3 GB |
| ChromaDB | 100 MB |
| PostgreSQL | 512 MB |
| Tesseract/EasyOCR | 500 MB |
| Spring Boot API | 1 GB |
| Next.js Web | 512 MB |
| OS + Tools | 3 GB |
| **Total** | **~7-9 GB** |

**Pros:**
- All local, no API costs
- Fast iteration
- Full privacy

**Cons:**
- Weaker AI quality
- May need cloud fallback for complex cases

---

### Option B: Hybrid Local-Cloud (Recommended for MVP)

**Local:**
- ~~PostgreSQL~~, Redis, Spring Boot API, Next.js Web

**Cloud (Free Tiers):**
- PostgreSQL: **Neon** (free tier: 512 MB storage) or **Supabase** (free tier: 500 MB)
- LLM: **Groq API** (free tier: 14,400 requests/min)
- Embeddings: **Groq** or **Atlas Cloud** free tier
- Vector DB: **Qdrant Cloud** (free tier: 1 cluster, 1GB)

**Estimated RAM:** ~3-4 GB (fewer local services)

**Cost:** $0-5/month (within free tiers)

---

### Option B+: Fully Cloud (All-in-One Managed)

**All Services Cloud (Free Tiers):**
| Service | Provider | Free Tier |
|---------|----------|-----------|
| PostgreSQL | Neon / Supabase | 512 MB / 500 MB |
| Redis | Redis Cloud / Upstash | 30 MB / 256 requests/min |
| API | Railway / Render | 500 MB RAM |
| LLM | Groq API | 14,400 req/min |
| Embeddings | Groq / OpenAI | Free tier |
| Vector DB | Qdrant Cloud | 1 GB storage |

**Estimated RAM:** ~0-2 GB (only IDE + Web browser)

**Pros:**
- Zero local resource usage
- Can develop on any machine (even Chromebook)
- Team collaboration easier (shared DB)

**Cons:**
- Requires internet
- Free tier limits
- Potential data privacy concerns

---

### Option C: Cloud-Only (Original Decision)

**Pros:**
- Handles complex AI tasks via cloud
- Keeps data services local
- Scales well

**Cons:**
- Requires internet
- Some API dependency

---

### Option C: Cloud-Only (Original Decision)

**All Services:**
- OCR: Google Vision API / AWS Textract
- LLM: OpenAI GPT-4 / Claude
- Embeddings: OpenAI / Cohere
- Vector DB: Pinecone / Qdrant Cloud

**Cost:** $20-200/month (MVP)

**Pros:**
- Best AI quality
- No local constraints
- Easy scaling

**Cons:**
- Highest cost
- API dependency
- Latency

---

### Option D: Tiered Development Setup

| Mode | Services | RAM | Use Case |
|------|----------|-----|----------|
| Minimal | PostgreSQL, API, Web | 2-3 GB | Quick feature dev |
| Standard | + Ollama (3B), ChromaDB | 5-7 GB | AI feature dev |
| Full | + Qwen 7B, Qdrant | 12-16 GB | Performance testing |

**Docker Compose profiles to enable/disable services**

---

## RAM Optimization Techniques

### Docker Resource Limits

```yaml
services:
  ollama:
    deploy:
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 2G
    
  qdrant:
    deploy:
      resources:
        limits:
          memory: 1G
    
  postgres:
    deploy:
      resources:
        limits:
          memory: 512M
```

### PostgreSQL Tuning (development)

```bash
# postgresql.conf
max_connections = 20
shared_buffers = 256MB
effective_cache_size = 512MB
maintenance_work_mem = 128MB
work_mem = 16MB
```

### Redis (if needed)

```yaml
services:
  redis:
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

---

## Cloud LLM Providers Comparison (2026)

### Free Tier Comparison

| Provider | Free Tier | Models | Vietnamese | Notes |
|----------|-----------|--------|------------|-------|
| **Groq** | 14,400 req/min | Llama 3.3, Mixtral, Qwen 2.5 | ⚠️ Limited | Fast inference |
| **Atlas Cloud** | $10 free credits | Many models | ✅ Good | Pay-as-you-go |
| **Together AI** | $5 free | 100+ models | ✅ Good | Good coverage |
| **Perplexity** | API key needed | Llama, Mistral | ⚠️ Limited | |
| **OpenRouter** | From $0 | 100+ models | ✅ Good | Unified API |

### Recommended for HealthLens MVP

**Primary:** Groq (fast, free tier generous)
**Fallback:** Atlas Cloud / OpenRouter (pay-per-use)

---

## Recommended Architecture Decision

### For 16GB RAM Developer Machine

```
┌─────────────────────────────────────────────────────────┐
│                    LOCAL SERVICES                        │
├─────────────────────────────────────────────────────────┤
│  PostgreSQL (512MB)  │  Redis (128MB)                   │
│  Spring Boot API    │  Next.js Web                      │
│  (1GB)              │  (512MB)                          │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                   CLOUD AI SERVICES                     │
├─────────────────────────────────────────────────────────┤
│  Groq API (LLM)     │  Groq/Atlas (Embeddings)         │
│  Qdrant Cloud       │  EasyOCR/Tesseract (fallback)    │
│  (free tier)        │                                  │
└─────────────────────────────────────────────────────────┘
```

### Architecture by Environment

| Component | Development | Production |
|-----------|-------------|------------|
| Database | PostgreSQL local | PostgreSQL (local/cloud) |
| Cache | Redis (optional) | Redis or skip |
| API | Spring Boot local | Cloud VM / Container |
| Web | Next.js local | Vercel / Cloudflare |
| LLM | Groq API | Groq + Claude fallback |
| OCR | EasyOCR + Tesseract | PaddleOCR + AWS Textract |
| Embeddings | Groq API | Groq / OpenAI |
| Vector DB | Qdrant Cloud | Qdrant Cloud / Self-hosted |

---

## Spring AI Framework Evaluation

### What is Spring AI?

Spring AI is a Spring Framework project that provides abstractions for integrating AI models into Java applications. It supports major AI providers (OpenAI, Azure OpenAI, Anthropic, Ollama, etc.) with a unified API.

### Key Features

| Feature | Spring AI Support | Notes |
|---------|-----------------|-------|
| **LLM Integration** | ✅ OpenAI, Claude, Groq, Ollama, Gemini, etc. | Unified ChatClient API |
| **Embeddings** | ✅ OpenAI, Ollama, Azure, local models | Simple embedding API |
| **Vector Stores** | ✅ Pinecone, Qdrant, Chroma, pgvector, Milvus | Pluggable architecture |
| **RAG Support** | ✅ Document loader, chunking, indexing | Built-in retrieval |
| **Multi-modal** | ✅ Images, audio support | Image understanding |
| **Function Calling** | ✅ Structured output, tool use | Agent capabilities |

### Spring AI vs Direct API Calls vs LangChain4j

| Criteria | Spring AI | Direct API | LangChain4j |
|----------|-----------|------------|--------------|
| **Learning Curve** | Low (Spring style) | Medium | High |
| **Vendor Lock-in** | Low (abstraction layer) | High | Medium |
| **Testing** | Easy (mock support) | Medium | Easy |
| **Production Ready** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Community** | Large (Spring ecosystem) | N/A | Growing |
| **Maintenance** | VMware/Broadcom | N/A | Active |

### Spring AI Code Example

```java
// ChatClient (Spring AI 1.0+)
@RestController
public class AIController {
    private final ChatClient chatClient;
    
    public AIController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are a health assistant.")
            .build();
    }
    
    @GetMapping("/explain")
    public String explainMetric(@RequestParam String metric) {
        return chatClient.prompt()
            .user("Explain {metric} in simple Vietnamese")
            .param("metric", metric)
            .call()
            .content();
    }
}

// Configuration for multiple providers
@Configuration
public class AIConfig {
    @Bean
    public ChatClient groqChatClient(ChatClient.Builder builder) {
        return builder
            .defaultEndpoint("https://api.groq.com/openai/v1")
            .defaultHeaders(h -> h.setAuthorization("Bearer " + groqApiKey))
            .build();
    }
}
```

### Pros for HealthLens

1. **Familiar Spring Boot patterns** - Same DI, configuration, testing approaches
2. **Multi-provider support** - Easy to switch between Groq, Claude, Ollama
3. **Built-in Vector Store abstractions** - Qdrant, pgvector support
4. **RAG framework** - Document loading, chunking, embedding, retrieval
5. **Active development** - Spring AI 1.1+ with Spring Boot 4 support

### Cons / Considerations

1. **Learning curve** - Need to learn Spring AI abstractions
2. **Additional dependency** - Extra library to maintain
3. **Vendor abstraction limits** - Not all provider features exposed equally

### Recommendation: ✅ Use Spring AI

**Justification:**
- HealthLens needs to support multiple LLM providers (Groq, Claude, Ollama)
- Spring AI provides unified abstractions that make switching easy
- RAG capabilities built-in (for future reference data features)
- Aligns with team's existing Spring Boot knowledge

### Implementation Notes

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-qdrant-spring-boot-starter</artifactId>
</dependency>
```

---

## Managed PostgreSQL Evaluation

### Options Comparison

| Provider | Free Tier | Storage | Connections | Branching | Best For |
|----------|-----------|---------|-------------|-----------|----------|
| **Neon** | ✅ | 512 MB | 1 compute | ✅ Yes | Serverless, branching |
| **Supabase** | ✅ | 500 MB | Limited | ✅ Limited | BaaS features |
| **Railway** | ✅ | 500 MB | Limited | ❌ | Simple deploys |
| **Render** | ✅ | Limited | Limited | ❌ | Long-running |
| **Koyeb** | ✅ | 10 GB | Good | ✅ Yes | Production-ready |

### Neon (Recommended for HealthLens)

**Pros:**
- True serverless (scales to zero)
- Database branching (dev/staging branches)
- Good free tier
- Native PostgreSQL

**Cons:**
- 512 MB storage limit (tight for MVP)
- Connection limits on free tier

**Free Tier Limits:**
- 512 MB storage
- 1 project
- 1 branch (main)
- Limited compute hours

### Supabase (Alternative)

**Pros:**
- 500 MB database + 1 GB file storage
- Built-in auth, storage
- Real-time subscriptions
- Good documentation

**Cons:**
- Row-level security complexity
- Connection limits

### Recommendation: Neon or Supabase

**Choose Neon if:**
- You want branching workflow
- Prefer pure PostgreSQL

**Choose Supabase if:**
- Might use BaaS features later
- Want built-in auth (though we have our own)

---

## ADR: Local Development Architecture

### ADR-003: Development Environment Strategy

**Status:** Proposed

**Context:**
Developer laptops have 16GB RAM. Running full local AI stack (PaddleOCR + Ollama + Qdrant + services) requires 14-31GB.

**Decision:**
Use cloud-first approach for development:
- **Database:** Neon/Supabase (managed PostgreSQL)
- **Cache:** Skip Redis or use Upstash (serverless)
- **API:** Spring Boot local (IDE development)
- **Web:** Next.js local
- **LLM:** Groq API (free tier)
- **Vector DB:** Qdrant Cloud (free tier)
- **OCR:** Tesseract/EasyOCR local with cloud fallback

**Consequences:**
- Development requires internet
- ~$0-5/month (within free tiers)
- Can develop on any machine

---

### ADR-004: AI Framework Strategy

**Status:** Proposed

**Context:**
Need unified abstraction for multiple LLM providers (Groq, Claude, Ollama) with RAG support for future features.

**Decision:**
Use **Spring AI** as the AI integration layer:
- Spring AI ChatClient for LLM calls
- Spring AI VectorStore abstractions
- Support multiple providers via configuration

**Consequences:**
- Additional learning curve
- Clean abstraction layer
- Easy provider switching
- Built-in RAG support

---

### ADR-005: Database as a Service

**Status:** Proposed

**Context:**
Local PostgreSQL consumes ~512MB-2GB RAM. For minimal local development, consider managed solutions.

**Decision:**
Use **Neon** (recommended) or **Supabase** for PostgreSQL:
- Free tier sufficient for MVP
- Branching workflow (dev/staging)
- Scales to zero when not in use

**Consequences:**
- Requires internet for DB access
- Storage limit (512MB)
- No data residency concerns (health data encryption)

---

## Next Steps

1. [ ] Review this research with team
2. [ ] Select development strategy (Option B/B+/C)
3. [ ] Decide on Spring AI adoption
4. [ ] Choose managed PostgreSQL provider (Neon vs Supabase)
5. [ ] Update architecture.md with selected strategy
6. [ ] Update stories 1.7, 1.8 if switching to cloud
7. [ ] Create ADR-003, ADR-004, ADR-005

---

## References

- [Ollama RAM Requirements](https://localllm.in/blog/ollama-vram-requirements-for-local-llms)
- [Qwen 2.5 7B on 8GB RAM](https://localaimaster.com/models/qwen-2-5-7b)
- [Docker Resource Limits](https://docker.recipes/docs/resource-limits)
- [Groq API Free Tier](https://console.groq.com)
- [PostgreSQL Redis Comparison](https://dev.to/_d7eb1c1703182e3ce1782/postgresql-performance-tuning-checklist-2026-complete-guide-65a)

---

## Research Metadata

| Field | Value |
|-------|-------|
| Last Updated | 2026-04-01 |
| Research Type | Technical |
| Priority | High |
| Impacts | Epic 1, 3, 4 stories |
| Decision Needed | Yes |

