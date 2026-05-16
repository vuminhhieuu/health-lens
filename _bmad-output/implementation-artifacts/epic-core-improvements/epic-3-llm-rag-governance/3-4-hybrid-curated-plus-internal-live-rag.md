# Story 3.4: Hybrid Curated + Internal Live RAG

Status: ready-for-dev

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

- [ ] Compose retrieval from curated corpus and reference-data DB.
- [ ] Add approved corpus filter.
- [ ] Add optional profile/history context with access/consent checks.
- [ ] Add retrieval trace metadata.
- [ ] Add tests for hit, miss, and consent-blocked context.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
