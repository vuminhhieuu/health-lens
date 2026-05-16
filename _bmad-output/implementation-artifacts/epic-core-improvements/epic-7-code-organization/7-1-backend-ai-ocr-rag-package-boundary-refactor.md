# Story 7.1: Backend AI/OCR/RAG Package Boundary Refactor

Status: ready-for-dev

## Execution Scope

**Area:** Backend package organization, bounded contexts, maintainability  
**Priority:** P1/P2

## Story

As a backend developer, I want AI/OCR/RAG code grouped by bounded context, so that provider abstraction and future AI changes do not make the generic service package harder to maintain.

## Acceptance Criteria

1. **Given** package refactor is complete, **When** backend tests run, **Then** behavior remains unchanged.
2. **Given** a developer needs OCR provider code, **When** browsing source, **Then** provider/router/contract classes are grouped under OCR/provider packages.
3. **Given** LLM/RAG code changes later, **When** developer navigates code, **Then** chat, embedding, RAG, and OCR boundaries are clear.

## Tasks / Subtasks

- [ ] Introduce packages for `ai.chat`, `ai.embedding`, `ai.rag`, `ocr`, and `provider`.
- [ ] Move classes incrementally.
- [ ] Update imports/tests.
- [ ] Avoid behavior changes beyond package boundaries.
- [ ] Document package map.

## Dev Notes

- This supersedes structural refactor scope, not old dead-code cleanup story.
- Do not combine with route constants cleanup.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/*`
- `apps/api/src/test/java/com/healthlens/api/service/*`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 7.1

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
