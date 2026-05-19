# Story 3.2: Structured LLM Output Schema And Validation

Status: done

## Execution Scope

**Area:** Backend LLM output validation, schema, retry/fallback, metrics  
**Priority:** P0/P1

## Story

As a user receiving AI explanations, I want AI output to follow a strict schema, so that malformed or hallucinated responses are rejected or safely degraded.

## Acceptance Criteria

1. **Given** LLM returns invalid JSON, **When** schema validation fails, **Then** system retries according to policy or returns safe fallback.
2. **Given** LLM output invents a reference range not present in structured data, **When** validation runs, **Then** output is rejected or corrected through fallback.
3. **Given** AI generation completes, **When** metrics are recorded, **Then** latency, model, token usage, outcome, and validation status are captured.
4. **Given** schema validation fails repeatedly, **When** user views explanation, **Then** user sees safe generic explanation with disclaimer.

## Tasks / Subtasks

- [x] Define JSON schema for explanation/recommendation output.
- [x] Replace fragile JSON repair with schema validation.
- [x] Add controlled retry and safe fallback.
- [x] Add metrics for invalid schema, retry count, token usage, latency.
- [x] Add tests for invalid JSON and hallucinated reference ranges.

### Review Findings

- [x] [Review][Patch] Recommendation schema is declared but not enforced [apps/api/src/main/java/com/healthlens/api/service/LlmService.java:69]
- [x] [Review][Patch] Explanation text can still contain hallucinated reference ranges [apps/api/src/main/java/com/healthlens/api/service/LlmService.java:546]
- [x] [Review][Patch] Abnormal explanation safety language is prompted but not validated [apps/api/src/main/java/com/healthlens/api/service/LlmService.java:529]
- [x] [Review][Patch] Successful structured explanations validate disclaimer but do not return it to the user [apps/api/src/main/java/com/healthlens/api/service/LlmService.java:546]

## Dev Notes

- The LLM must not be source of truth for reference ranges.
- Avoid showing raw model output when validation fails.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/main/resources/ai/prompts/metric-explanation.v3.txt`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.2
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 9 and 10

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.LlmServiceTest` - pass
- `cd apps/api && ./gradlew test` - pass

### Completion Notes List

- Defined strict structured output contracts for metric explanations and recommendations.
- Metric explanations now require valid JSON with `explanation`, `referenceRange`, and `disclaimer`; invalid JSON/schema or hallucinated reference ranges are rejected and routed through retry/fallback.
- Safe fallback explanations now include the medical disclaimer and never expose raw invalid model output.
- Added AI generation metrics for latency, model, estimated token usage, outcome, validation status, retry count, plus schema validation failure counters.
- Added unit coverage for invalid JSON retries, hallucinated reference range rejection, structured success output, caching, and metrics.
- Review fixes enforce recommendation schema parsing, reject hallucinated ranges inside explanation text, validate abnormal follow-up safety copy, and return the validated disclaimer with successful explanations.

### File List

- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/main/resources/ai/prompts/metric-explanation.v3.txt`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`

## Change Log

- 2026-05-19: Implemented structured LLM output schema validation, retry/fallback handling, observability metrics, and guardrail tests.
