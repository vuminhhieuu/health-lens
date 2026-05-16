# Story 3.2: Structured LLM Output Schema And Validation

Status: ready-for-dev

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

- [ ] Define JSON schema for explanation/recommendation output.
- [ ] Replace fragile JSON repair with schema validation.
- [ ] Add controlled retry and safe fallback.
- [ ] Add metrics for invalid schema, retry count, token usage, latency.
- [ ] Add tests for invalid JSON and hallucinated reference ranges.

## Dev Notes

- The LLM must not be source of truth for reference ranges.
- Avoid showing raw model output when validation fails.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.2
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 9 and 10

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
