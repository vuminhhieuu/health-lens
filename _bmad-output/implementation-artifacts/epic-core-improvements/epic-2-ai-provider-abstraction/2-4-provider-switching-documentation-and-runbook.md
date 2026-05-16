# Story 2.4: Provider Switching Documentation And Runbook

Status: ready-for-dev

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

- [ ] Create provider compatibility matrix.
- [ ] Document env-only provider switches vs adapter-required switches.
- [ ] Add rollback steps for LLM, OCR, embedding, and vector store.
- [ ] Add smoke test checklist.
- [ ] Link from deployment/runbook docs.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
