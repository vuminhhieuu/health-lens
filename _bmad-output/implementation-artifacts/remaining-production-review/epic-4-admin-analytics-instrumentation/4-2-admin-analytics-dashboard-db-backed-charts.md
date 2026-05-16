# Story 4.2: Admin Analytics Dashboard DB-Backed Charts

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / admin analytics  
**Area:** Admin analytics API, admin dashboard charts, date filtering, empty/error states  
**Priority:** P1

## Story

As an admin,  
I want analytics charts backed by stored events,  
so that user growth, WAU/upload volume, and OCR success/failure rates are trustworthy.

## Acceptance Criteria

1. `/admin/analytics` renders DB-backed user growth.
2. `/admin/analytics` renders WAU and upload volume.
3. `/admin/analytics` renders upload/OCR success and failure rate.
4. Charts support date range and empty/error states.
5. Stub/static chart data is removed.

## Tasks / Subtasks

- [ ] Task 1 - Add analytics query endpoints/services over product events (AC: #1, #2, #3)
- [ ] Task 2 - Wire `/admin/analytics` charts to real API data (AC: #1-#3)
- [ ] Task 3 - Implement date range filtering and empty/error states (AC: #4)
- [ ] Task 4 - Remove static/stub chart data (AC: #5)
- [ ] Task 5 - Add backend query tests and frontend rendering verification (AC: #1-#5)

## Dev Notes

### Implementation Guardrails

- Story 4.1 should exist before this story or this story must include a temporary migration path for existing events.
- Admin charts should never silently fall back to fake data.
- Use the existing admin auth/authorization model.

### Likely Files

- `apps/web/src/app/(admin)/admin/analytics/*`
- `apps/web/src/lib/api.ts`
- `apps/api/src/main/java/com/healthlens/api/controller/AdminAnalyticsController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AdminAnalyticsService.java`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 4.2
- `_bmad-output/planning-artifacts/review-source/production-review/epic-8-analytics-spec.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
