# Story 1.1: Dashboard Uses Real Latest Health Data

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / web correctness  
**Area:** Web dashboard, health record queries, empty/loading/error states  
**Priority:** P0

## Story

As a user opening the dashboard,  
I want to see real latest health data instead of hardcoded stats,  
so that the home screen reflects my actual health record state.

## Context

Production review identified dashboard data correctness as a release risk. The dashboard must not present fake health stats, misleading status, or stale latest-record links. This story replaces hardcoded display values with query-backed data and aligns the empty state with the real upload/review workflow.

## Acceptance Criteria

1. Dashboard stat cards use query-backed data, not hardcoded fake values.
2. Empty/new-user state shows a useful next action without fake health metrics.
3. Latest record summary links to the correct record/profile.
4. Loading/error states do not show misleading health status.

## Tasks / Subtasks

- [ ] Task 1 - Identify dashboard data sources (AC: #1, #3)
  - [ ] Map current dashboard fields to backend/API response fields.
  - [ ] Remove static/fake values where real data is available.
- [ ] Task 2 - Implement real dashboard query mapping (AC: #1, #3)
  - [ ] Fetch latest health record/profile state through existing client API patterns.
  - [ ] Validate null/missing dates before rendering.
- [ ] Task 3 - Add production-safe empty/loading/error states (AC: #2, #4)
  - [ ] Render new-user empty state with upload CTA.
  - [ ] Ensure loading does not render health conclusions.
  - [ ] Ensure error state is visible and retryable.
- [ ] Task 4 - Tests (AC: #1-#4)
  - [ ] Add tests or story-level verification for empty user, user with record, API error, and loading state.

## Dev Notes

### Implementation Guardrails

- Do not show fake metrics or fake trends as placeholders in authenticated production UI.
- Keep API calls consistent with existing frontend data-fetching conventions.
- If a backend summary endpoint is missing, prefer a small typed API addition over client-side scraping of unrelated responses.

### Likely Files

- `apps/web/src/app/(dashboard)/home/page.tsx`
- `apps/web/src/lib/api.ts`
- `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 1.1
- `_bmad-output/planning-artifacts/core-review-docs-coverage-audit.md`
- `_bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
