# Story 2.3: Embedding And Vector Store Config Validation

Status: done

## Execution Scope

**Area:** Backend embedding config, Qdrant/vector store startup validation, RAG health  
**Priority:** P1

## Story

As a deployer, I want embedding model dimension and vector store configuration validated at startup, so that RAG does not silently fail or produce low-quality retrieval.

## Acceptance Criteria

1. **Given** embedding dimension is 1536 but collection is 1024, **When** app starts in production, **Then** startup fails with a clear reindex/dimension mismatch message.
2. **Given** `QDRANT_HOST` includes a protocol when unsupported, **When** config is loaded, **Then** app fails fast or normalizes according to documented rules.
3. **Given** vector store is unavailable, **When** health check runs, **Then** AI/RAG health status reports degraded/unavailable.
4. **Given** embedding model changes, **When** app starts, **Then** docs and validation indicate whether reindex is required.

## Tasks / Subtasks

- [x] Add embedding dimension config validation.
- [x] Validate Qdrant host/port format.
- [x] Add vector store health indicator.
- [x] Document reindex procedure when embedding model or dimension changes.
- [x] Add tests for dimension mismatch, host format, and unavailable vector store.

### Review Findings

- [x] [Review][Patch] AI/RAG health check can hang indefinitely when Qdrant health future does not complete [apps/api/src/main/java/com/healthlens/api/config/AiRagHealthIndicator.java:19]
- [x] [Review][Patch] AI/RAG health check swallows thread interruption while converting failures to DOWN [apps/api/src/main/java/com/healthlens/api/config/AiRagHealthIndicator.java:25]
- [x] [Review][Patch] Qdrant host validation accepts unsupported schemes, host:port, paths, query, or user-info as hostname-only values [apps/api/src/main/java/com/healthlens/api/config/QdrantVectorStoreEnvironmentPostProcessor.java:52]
- [x] [Review][Patch] URL-style local/dev host normalization can ignore an explicit URI port because application defaults already provide `spring.ai.vectorstore.qdrant.port` [apps/api/src/main/java/com/healthlens/api/config/QdrantVectorStoreEnvironmentPostProcessor.java:69]
- [x] [Review][Patch] Strict startup validation passes when existing collection dimension cannot be extracted [apps/api/src/main/java/com/healthlens/api/config/EmbeddingVectorStoreStartupValidator.java:68]
- [x] [Review][Patch] Named-vector Qdrant collections validate only the first vector size instead of validating the app-used vector or all configured vectors [apps/api/src/main/java/com/healthlens/api/config/EmbeddingVectorStoreStartupValidator.java:104]
- [x] [Review][Patch] Local/dev Qdrant URL normalization behavior is implemented but not documented as part of the host rules [docs/environment-reference.md:69]

## Dev Notes

- Do not silently recreate or reindex production collections during startup.
- Fail-fast behavior should be production-profile strict and local-dev friendly where appropriate.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/EmbeddingService.java`
- `apps/api/src/main/java/com/healthlens/api/service/VectorStoreService.java`
- `apps/api/src/main/resources/application.yml`
- `apps/api/src/main/resources/application-docker.yml`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 2.3
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 13

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests '*QdrantVectorStoreEnvironmentPostProcessorTest' --tests '*EmbeddingVectorStoreStartupValidatorTest' --tests '*AiRagHealthIndicatorTest'`
- `cd apps/api && ./gradlew test`
- `cd apps/api && ./gradlew test --tests '*QdrantVectorStoreEnvironmentPostProcessorTest' --tests '*EmbeddingVectorStoreStartupValidatorTest' --tests '*AiRagHealthIndicatorTest'`
- `cd apps/api && ./gradlew test`

### Completion Notes List

- Added production/staging startup validation for embedding dimension vs configured Qdrant dimension and existing collection dimension. Local/default startup skips remote validation unless explicitly enabled with `app.ai.rag.validation.local-enabled=true`.
- Added Qdrant host/port environment validation: production/staging rejects protocol-bearing `QDRANT_HOST`; local/dev normalizes URL-style hosts to hostname/port.
- Added `aiRag` health indicator that reports Qdrant-backed RAG as available or unavailable via Actuator health details.
- Updated Qdrant defaults/docs to use hostname-only host, gRPC port `6334`, vector dimension `1536`, and explicit reindex procedure when embedding settings change.
- Tests added for dimension mismatch, host format/port validation, and unavailable vector store health behavior. Full API test suite passed.
- Code review patches resolved: bounded AI/RAG health timeout, interrupt restoration, stricter Qdrant host validation, URI port precedence, strict failure when collection dimension cannot be verified, named-vector dimension handling, and local/dev normalization docs.
- Re-ran targeted config/health tests and full API regression suite after review fixes; both passed.

### File List

- `.env.example`
- `apps/api/src/main/java/com/healthlens/api/config/AiRagHealthIndicator.java`
- `apps/api/src/main/java/com/healthlens/api/config/EmbeddingVectorStoreStartupValidator.java`
- `apps/api/src/main/java/com/healthlens/api/config/QdrantVectorStoreEnvironmentPostProcessor.java`
- `apps/api/src/main/resources/META-INF/spring.factories`
- `apps/api/src/main/resources/application-docker.yml`
- `apps/api/src/main/resources/application.yml`
- `apps/api/src/test/java/com/healthlens/api/config/AiRagHealthIndicatorTest.java`
- `apps/api/src/test/java/com/healthlens/api/config/EmbeddingVectorStoreStartupValidatorTest.java`
- `apps/api/src/test/java/com/healthlens/api/config/QdrantVectorStoreEnvironmentPostProcessorTest.java`
- `docs/STAGING_DEPLOYMENT.md`
- `docs/environment-reference.md`

### Change Log

- 2026-05-18: Implemented embedding/vector store startup validation, Qdrant config validation, RAG health indicator, docs, and tests.
- 2026-05-18: Resolved code review findings and marked story done.
