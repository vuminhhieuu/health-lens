# Story 6.2: Replace Browser Alerts And Local Toasts

Status: done

## Execution Scope

**Area:** Frontend notification cleanup, dashboard/review/admin/consent flows  
**Priority:** P1

## Story

As a user, I want all app notifications to use the same UI pattern, so that dashboard, review, admin, and consent flows feel coherent.

## Acceptance Criteria

1. **Given** codebase is searched for `alert(`, **When** replacement is complete, **Then** no production UI path uses browser alert for normal success/failure notifications.
2. **Given** consent is accepted or rejected, **When** feedback is shown, **Then** global toast system is used.
3. **Given** sharing/invite/revoke actions complete, **When** feedback appears, **Then** it uses standardized notification copy.
4. **Given** save result succeeds/fails, **When** review page responds, **Then** it uses toast/inline errors according to notification policy.

## Tasks / Subtasks

- [ ] Replace browser alerts in home and health-records sharing flows.
- [ ] Replace review page save success alert.
- [ ] Replace admin backup-code copy alert.
- [ ] Refactor `ConsentModal` local toast to global notification.
- [ ] Add grep/test check for remaining `alert(` where feasible.

## Dev Notes

- Depends on Story 6.1.
- Do not convert field-level validation errors into only global toasts.

## Likely Files

- `apps/web/src/app/(dashboard)/home/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `apps/web/src/app/admin/login/page.tsx`
- `apps/web/src/components/features/consent/ConsentModal.tsx`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 6.2

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
