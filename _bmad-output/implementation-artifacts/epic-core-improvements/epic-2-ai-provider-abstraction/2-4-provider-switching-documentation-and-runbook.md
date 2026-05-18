# Story 2.4: Provider Switching Documentation And Runbook

Status: done

## Execution Scope

**Area:** Operations docs, provider compatibility matrix, rollback/smoke test runbook  
**Priority:** P1

## Story

As an operator, I want a provider switching runbook, so that changing LLM/OCR/embedding/RAG providers is predictable and reversible.

## Acceptance Criteria

1. **Given** a deployer wants to switch LLM provider, **When** they read the runbook, **Then** they can identify env keys, compatibility limits, smoke tests, and rollback steps.
2. **Given** a provider requires native SDK support, **When** the runbook describes it, **Then** it clearly says env-only switching is not enough.
3. **Given** embedding provider/model changes, **When** the runbook is followed, **Then** reindex/dimension risks are addressed.
4. **Given** OCR provider changes, **When** fallback fails, **Then** rollback and manual recovery steps are documented.

## Tasks / Subtasks

- [x] Create provider compatibility matrix.
- [x] Document env-only provider switches vs adapter-required switches.
- [x] Add rollback steps for LLM, OCR, embedding, and vector store.
- [x] Add smoke test checklist.
- [x] Link from deployment/runbook docs.

## Dev Notes

- This is documentation, but it should be treated as operationally binding.
- Include privacy/cost/latency notes for external providers.

## Likely Files

- `docs/provider-switching-runbook.md`
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md`
- `.env.example`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 2.4

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `git diff --check`
- `cd apps/api && ./gradlew test`

### Completion Notes List

- Added `docs/provider-switching-runbook.md` with compatibility matrix for LLM chat, embedding, Qdrant/RAG, and OCR providers.
- Documented env-only switching boundaries versus adapter-required provider changes.
- Added rollback steps for LLM chat, embedding, Qdrant/vector store, and OCR including fallback failure/manual recovery guidance.
- Added smoke test checklist covering API readiness, AI/RAG health, LLM explanation, embedding/RAG retrieval, OCR extraction, OCR fallback, and rollback validation.
- Linked the provider switching runbook from the docs index, operations runbook, deployment guide, and environment reference.
- Full API regression suite passed after documentation changes; existing scheduled `EmailConsumer` test-context warnings were logged but did not fail the build.

### File List

- `docs/provider-switching-runbook.md`
- `docs/index.md`
- `docs/operations-runbook.md`
- `docs/deployment-guide.md`
- `docs/environment-reference.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-2-ai-provider-abstraction/2-4-provider-switching-documentation-and-runbook.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-05-18: Added provider switching documentation and runbook, linked it from operational docs, and marked story ready for review.
