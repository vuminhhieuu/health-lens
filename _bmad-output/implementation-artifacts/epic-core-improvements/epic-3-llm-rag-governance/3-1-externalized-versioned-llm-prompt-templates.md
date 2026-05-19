# Story 3.1: Externalized Versioned LLM Prompt Templates

Status: done

## Execution Scope

**Area:** Backend LLM prompt resources, prompt versioning, tests  
**Priority:** P1

## Story

As an AI feature maintainer, I want prompts stored as versioned templates outside large Java strings, so that prompt behavior can be reviewed, tested, and rolled back.

## Acceptance Criteria

1. **Given** a recommendation is generated, **When** AI metadata is persisted/logged, **Then** prompt version and model version are available for audit.
2. **Given** a prompt template is edited, **When** tests run, **Then** required medical disclaimer and no-invent-reference-ranges instruction are verified.
3. **Given** prompt rendering fails, **When** API handles request, **Then** system returns safe fallback and records a failure metric.

## Tasks / Subtasks

- [x] Move prompts from Java inline strings to resource templates.
- [x] Add prompt version metadata.
- [x] Add prompt rendering tests.
- [x] Record prompt/model version in generated output metadata.
- [x] Document prompt update/review process.

### Review Findings

- [x] [Review][Patch] Guard malformed 3-part explanation cache values [apps/api/src/main/java/com/healthlens/api/service/LlmService.java:415]
- [x] [Review][Patch] Add recommendation prompt/model metadata for audit [apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java:515]
- [x] [Review][Patch] Prevent recommendation prompt version drift from configurable template path [apps/api/src/main/java/com/healthlens/api/service/LlmService.java:79]
- [x] [Review][Patch] Add recommendation prompt render-failure test coverage [apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java:278]

## Dev Notes

- Do not change user-facing AI behavior without tests capturing current required guardrails.
- Keep templates readable by reviewers who are not Java developers.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/main/resources/ai/`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.1
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 9

## Dev Agent Record

### Agent Model Used

Codex GPT-5

### Debug Log References

- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.LlmServiceTest` — pass
- `cd apps/api && ./gradlew test` — pass
- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.LlmServiceTest --tests com.healthlens.api.service.HealthRecordServiceTest` — pass after review fixes
- `cd apps/api && ./gradlew test` — pass after review fixes

### Completion Notes List

- Externalized metric explanation and recommendation prompts into versioned classpath resources under `apps/api/src/main/resources/ai/prompts/`.
- Added prompt rendering failure fallback path with Micrometer counter `healthlens.ai.prompt.render.failures`.
- Added `promptVersion` and `modelVersion` to LLM explanation metadata and API response while preserving older constructors/cache parsing.
- Added tests for prompt guardrails, metadata, cache serialization, and prompt-rendering fallback behavior.
- Documented prompt update/review and rollback process in `docs/llm-prompt-templates.md`.
- Addressed code review findings: hardened malformed cache parsing, added recommendation metadata to API response, tied recommendation cache/audit versioning to configurable prompt templates, and covered recommendation render-failure fallback metrics.

### File List

- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-3-llm-rag-governance/3-1-externalized-versioned-llm-prompt-templates.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `apps/api/src/main/java/com/healthlens/api/dto/response/MetricExplanationResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/RecommendationsResponse.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/main/java/com/healthlens/api/service/PromptTemplateRenderer.java`
- `apps/api/src/main/resources/ai/prompts/metric-explanation.v3.txt`
- `apps/api/src/main/resources/ai/prompts/recommendations.v8-medical-disclaimer-vi.txt`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`
- `docs/index.md`
- `docs/llm-prompt-templates.md`

### Change Log

- 2026-05-19: Implemented externalized versioned LLM prompt templates, audit metadata, render-failure fallback metrics, tests, and prompt update documentation.
- 2026-05-19: Addressed code review findings and marked story done after full backend regression passed.
