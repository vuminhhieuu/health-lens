# Story 7.8: Shared Frontend API Contract Types For High-Churn Features

Status: proposed

## Execution Scope

**Area:** Frontend type organization, shared contracts, maintainability  
**Priority:** P3

## Story

As a frontend developer, I want high-churn API contract types organized into shared feature locations, so that health-record review, sharing, admin audit, and reference-data changes do not drift across page-local type definitions.

## Acceptance Criteria

1. **Given** targeted high-churn features are reviewed, **When** the story is complete, **Then** page-local API contract types are reduced in favor of shared feature locations.
2. **Given** route behavior and API payload shapes are exercised, **When** tests or type checks run, **Then** behavior remains unchanged.
3. **Given** developers need contract types for targeted features, **When** browsing source, **Then** those types are easier to locate and reuse.

## Tasks / Subtasks

- [ ] Identify the first set of high-churn features to normalize: health-record review, sharing, admin audit, and reference-data.
- [ ] Move frontend-only contract types into `packages/shared` or clearly named web feature type modules.
- [ ] Keep route constants and backend route definitions unchanged.
- [ ] Avoid introducing code generation in this story.
- [ ] Run type-check verification after reorganization.

## Dev Notes

- This is a type organization story, not an API redesign.
- Prefer small, explicit shared modules over broad catch-all type files.
- Keep compatibility with `packages/shared/constants/api.ts` and backend `ApiRoutes.java`.

## Likely Files

- `packages/shared/`
- `apps/web/src/types/`
- `apps/web/src/components/features/`
- `apps/web/src/app/admin/`
- `apps/web/src/app/(dashboard)/`

## References

- `epic-7-issues-and-proposed-stories.md`
- `code-organization-assessment.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
