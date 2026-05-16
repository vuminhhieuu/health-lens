# Story 8.2: AI/OCR Regression Test Corpus In CI

Status: ready-for-dev

## Execution Scope

**Area:** Test fixtures, OCR/parser regression, LLM schema tests, RAG retrieval tests  
**Priority:** P1

## Story

As a quality owner, I want OCR/parser/AI regression tests to run against representative fixtures, so that improvements do not silently break core health metric extraction.

## Acceptance Criteria

1. **Given** parser code changes, **When** CI runs, **Then** fixture-based tests verify metric name/value/unit/reference extraction.
2. **Given** LLM service changes, **When** CI runs, **Then** invalid JSON and hallucinated range cases are tested.
3. **Given** RAG corpus changes, **When** CI runs, **Then** known metric queries return expected approved chunks.
4. **Given** fixtures are stored, **When** repository is inspected, **Then** no real patient data is committed.

## Tasks / Subtasks

- [ ] Add sanitized/synthetic Vietnamese lab fixtures.
- [ ] Add parser regression tests.
- [ ] Add mocked LLM schema validation tests.
- [ ] Add RAG retrieval tests for known metrics/aliases.
- [ ] Wire tests into CI.

## Dev Notes

- Do not depend on live OCR/LLM providers for normal CI.
- Keep fixtures synthetic or fully redacted.

## Likely Files

- `apps/api/src/test/resources/`
- `apps/api/src/test/java/com/healthlens/api/service/`
- `.github/workflows/ci.yml`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 8.2

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
