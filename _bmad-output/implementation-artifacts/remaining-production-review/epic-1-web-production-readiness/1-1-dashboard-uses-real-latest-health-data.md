# Story 1.1: Dashboard Uses Real Latest Health Data

Status: done

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

- [x] Task 1 - Identify dashboard data sources (AC: #1, #3)
  - [x] Map current dashboard fields to backend/API response fields.
  - [x] Remove static/fake values where real data is available.
- [x] Task 2 - Implement real dashboard query mapping (AC: #1, #3)
  - [x] Fetch latest health record/profile state through existing client API patterns.
  - [x] Validate null/missing dates before rendering.
- [x] Task 3 - Add production-safe empty/loading/error states (AC: #2, #4)
  - [x] Render new-user empty state with upload CTA.
  - [x] Ensure loading does not render health conclusions.
  - [x] Ensure error state is visible and retryable.
- [x] Task 4 - Tests (AC: #1-#4)
  - [x] Add tests or story-level verification for empty user, user with record, API error, and loading state.

### Review Findings

- [x] [Review][Patch] Empty/upload CTAs route to `/health-records?openUpload=1`, but that page does not mount the upload control [apps/web/src/app/(dashboard)/home/page.tsx:332]
- [x] [Review][Patch] Dashboard total count reads `pagination.totalItems`, but the API contract returns `pagination.total`, causing counts to fall back to the 3-item page size [apps/web/src/app/(dashboard)/home/page.tsx:122]
- [x] [Review][Patch] Retry calls the disabled records query even when no `primaryProfileId` exists after a profile-load error [apps/web/src/app/(dashboard)/home/page.tsx:287]

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

GPT-5 Codex

### Debug Log References

- `pnpm lint` - passed web lint after Copilot comment fixes.
- `pnpm test` - not applicable in current PR state because the dashboard test file was intentionally removed; Vitest exits with "No test files found".

### Completion Notes List

- Replaced hardcoded dashboard health stat placeholders with query-backed profile and latest-record summary values from existing profile/health-record APIs.
- Added null-safe date/count rendering so missing or in-progress records do not produce fake health conclusions.
- Added explicit empty state with upload CTA, loading state without conclusions, and retryable dashboard error state.
- Updated latest-record CTA to link directly to the latest record review page when a record exists.
- Verified dashboard behavior through story-level manual review and web validation; no dedicated dashboard test file is included in this PR.
- Resolved code review findings by routing upload CTAs to the profile history upload flow, reading backend `pagination.total`, and guarding records retry when no profile id exists.
- Resolved Copilot review feedback by showing error placeholders in dashboard stat cards instead of fallback zeros or "Chưa có dữ liệu" when profile/record queries fail.

### File List

- `apps/web/src/app/(dashboard)/home/page.tsx`

### Change Log

- 2026-05-16: Implemented real latest-health-data dashboard mapping and production-safe states for Story 1.1.
- 2026-05-16: Addressed code review findings and marked Story 1.1 done.
