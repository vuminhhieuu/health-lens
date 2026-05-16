# Story 3.5: Trusted Online RAG Source Adapter With Citation And Cache

Status: ready-for-dev

## Execution Scope

**Area:** Trusted source retrieval, online RAG governance, citation/cache/audit  
**Priority:** P1, P0 if online evidence becomes user-facing

## Story

As a product owner, I want online RAG limited to trusted and auditable sources, so that HealthLens can use updated knowledge without exposing users to unvetted web content.

## Acceptance Criteria

1. **Given** a source is not allowlisted, **When** online retrieval is attempted, **Then** the source is rejected.
2. **Given** a trusted source is retrieved, **When** content is used, **Then** snapshot metadata is persisted for audit.
3. **Given** retrieved online evidence is low quality or unreviewed, **When** AI output is generated, **Then** system excludes it or marks review required.
4. **Given** citation metadata exists, **When** admin/audit view inspects an answer, **Then** source URL, publisher, retrievedAt, snapshotHash, and reviewStatus are available.

## Tasks / Subtasks

- [ ] Add trusted source allowlist.
- [ ] Add source metadata model.
- [ ] Add retrieval cache/snapshot hash.
- [ ] Add review status and exclusion rules.
- [ ] Add tests for rejected, cached, approved, and unreviewed sources.

## Dev Notes

- Do not implement unrestricted web search.
- Treat online content as hostile until allowlisted, cached, and reviewed.

## Likely Files

- New RAG online adapter package under `apps/api/src/main/java/com/healthlens/api/`
- RAG retrieval service
- DB migration if metadata is persisted

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.5
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 24

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
