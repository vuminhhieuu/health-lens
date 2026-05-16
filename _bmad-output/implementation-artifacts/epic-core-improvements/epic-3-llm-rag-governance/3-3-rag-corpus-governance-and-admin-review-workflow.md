# Story 3.3: RAG Corpus Governance And Admin Review Workflow

Status: ready-for-dev

## Execution Scope

**Area:** RAG corpus metadata, ingestion governance, approval/versioning  
**Priority:** P1

## Story

As an admin maintaining medical explanation content, I want RAG corpus changes to be versioned, reviewed, and auditable, so that AI explanations use trustworthy Vietnamese medical context.

## Acceptance Criteria

1. **Given** a corpus update is ingested, **When** ingestion completes, **Then** report includes source version, chunk count, embedding model, and errors.
2. **Given** a corpus chunk is not approved, **When** retrieval runs, **Then** the chunk is not used in user-facing explanation.
3. **Given** a corpus version causes issues, **When** admin rolls back, **Then** previous approved version becomes active.
4. **Given** corpus metadata is inspected, **When** audit requires evidence, **Then** reviewer/effective date/source version are available.

## Tasks / Subtasks

- [ ] Add corpus metadata model.
- [ ] Add ingestion report.
- [ ] Add approval/effective version rules.
- [ ] Add rollback process.
- [ ] Add retrieval filter for approved active corpus only.

## Dev Notes

- Do not duplicate admin reference-data approval workflow unless corpus becomes admin-managed through the same UI.
- Keep `metric-explanations.vi.json` reviewable and versioned.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java`
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`
- `apps/api/src/main/resources/ai/metric-explanations.vi.json`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.3
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 11

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
