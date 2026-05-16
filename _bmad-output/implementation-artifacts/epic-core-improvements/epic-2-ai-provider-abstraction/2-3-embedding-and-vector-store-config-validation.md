# Story 2.3: Embedding And Vector Store Config Validation

Status: ready-for-dev

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

- [ ] Add embedding dimension config validation.
- [ ] Validate Qdrant host/port format.
- [ ] Add vector store health indicator.
- [ ] Document reindex procedure when embedding model or dimension changes.
- [ ] Add tests for dimension mismatch, host format, and unavailable vector store.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
