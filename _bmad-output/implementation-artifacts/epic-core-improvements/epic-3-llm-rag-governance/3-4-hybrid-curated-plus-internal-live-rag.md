# Story 3.4: Hybrid Curated + Internal Live RAG

Status: done

## Execution Scope

**Area:** RAG retrieval composition, reference-data grounding, consent-aware context  
**Priority:** P1

## Story

As a user receiving explanations, I want AI explanations grounded in curated corpus plus current internal reference data, so that outputs stay relevant without relying on uncontrolled web search.

## Acceptance Criteria

1. **Given** Qdrant returns a relevant approved chunk, **When** explanation is generated, **Then** prompt includes that chunk and structured reference range.
2. **Given** vector retrieval misses, **When** fallback runs, **Then** system uses internal reference data snippet, not open web.
3. **Given** user profile context is included, **When** retrieval/prompt construction runs, **Then** access control and consent are checked.
4. **Given** retrieval completes, **When** logs/metadata are inspected, **Then** source, hit/miss, score, and fallback path are recorded.

## Tasks / Subtasks

- [x] Compose retrieval from curated corpus and reference-data DB.
- [x] Add approved corpus filter.
- [x] Add optional profile/history context with access/consent checks.
- [x] Add retrieval trace metadata.
- [x] Add tests for hit, miss, and consent-blocked context.

### Review Findings

- [x] [Review][Patch] Approved corpus filter cannot match currently ingested Qdrant metadata [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java:125]
- [x] [Review][Patch] Record-level share can include profile context without profile-level access [apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java:488]
- [x] [Review][Patch] Raw profile fields are inserted into LLM context without escaping or bounds [apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java:498]
- [x] [Review][Patch] Structured reference range is appended as unescaped pseudo-JSON prompt text [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java:149]
- [x] [Review][Patch] Curated document text is included in prompt without a size bound [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java:133]
- [x] [Review][Patch] No-active-corpus path is traced as a Qdrant miss [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java:67]
- [x] [Review][Patch] API response DTO exposes service-layer nested retrieval trace type [apps/api/src/main/java/com/healthlens/api/dto/response/MetricExplanationResponse.java:5]

## Dev Notes

- Do not add open web RAG in this story.
- Reference ranges remain structured data, not model-generated content.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`
- `apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java`
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.4
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 24

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-05-19: `./gradlew test --tests com.healthlens.api.service.MetricExplanationRetrievalServiceTest --tests com.healthlens.api.service.HealthRecordServiceTest --tests com.healthlens.api.service.LlmServiceTest` passed.
- 2026-05-19: `./gradlew test` passed. Test shutdown emitted existing `EmailConsumer` Redis stream warnings, but Gradle completed successfully.
- 2026-05-19: Review patch pass: `./gradlew test --tests com.healthlens.api.service.MetricExplanationRetrievalServiceTest --tests com.healthlens.api.service.HealthRecordServiceTest --tests com.healthlens.api.service.LlmServiceTest` passed.
- 2026-05-19: Review patch pass: `./gradlew test` passed.

### Completion Notes List

- Retrieval now composes approved Qdrant corpus chunks with structured reference range data and falls back only to internal reference data or generic internal snippet.
- Added approved corpus filtering via active approved source version and language.
- Metric explanation flow checks record/profile access first, then active consent before adding profile context to retrieval/prompt construction.
- Retrieval traces now expose source, hit/miss, score, and fallback path through response metadata and logs/metrics.
- Added tests for Qdrant hit, vector miss fallback, Qdrant error fallback, checked profile context, and consent-blocked explanation.
- Review patches remove service-type DTO coupling, omit profile context for record-level shares, sanitize profile context as bounded JSON, avoid pseudo-JSON range text in snippets, bound curated chunk size, and distinguish no-active-corpus fallback traces.

### File List

- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/MetricExplanationResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/RetrievalTraceResponse.java`
- `apps/api/src/main/resources/ai/prompts/metric-explanation.v3.txt`
- `apps/api/src/test/java/com/healthlens/api/service/MetricExplanationRetrievalServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-05-19: Implemented hybrid curated plus internal live RAG with approved corpus filtering, consent-aware profile context, retrieval trace metadata, and regression coverage.
- 2026-05-19: Resolved code review patch findings and moved story to done.
