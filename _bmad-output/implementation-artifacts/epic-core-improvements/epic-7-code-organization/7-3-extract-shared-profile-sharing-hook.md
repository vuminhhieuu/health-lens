# Story 7.3: Extract Shared Profile Sharing Hook

Status: ready-for-dev

## Execution Scope

**Area:** Frontend profile sharing logic, duplicated mutations, notifications  
**Priority:** P2

## Story

As a frontend developer, I want profile sharing mutations reused across dashboard pages, so that invite/update/revoke behavior stays consistent.

## Acceptance Criteria

1. **Given** user updates access level from home page, **When** mutation succeeds, **Then** shared hook invalidates relevant queries and shows standardized toast.
2. **Given** same action happens from health-records page, **When** mutation fails, **Then** error behavior is identical.
3. **Given** invite/revoke logic changes, **When** code is updated, **Then** only shared hook needs core mutation changes.

## Tasks / Subtasks

- [ ] Extract `useProfileSharing`.
- [ ] Reuse in home page.
- [ ] Reuse in health-records page.
- [ ] Integrate global notification from Story 6.1.
- [ ] Add tests or focused component/hook coverage.

## Dev Notes

- Depends on notification foundation for final UX cleanup.
- Keep query keys compatible with existing React Query usage.

## Likely Files

- `apps/web/src/app/(dashboard)/home/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/page.tsx`
- `apps/web/src/hooks/useProfileSharing.ts`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 7.3

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
