# Story 9: Profile Settings Stub Cleanup

Status: backlog

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P2 | **Depends on:** `pae-1`, `pae-8` (support constants)

## Story

As a signed-in user,  
I want the profile settings page to show only working features,  
so that I am not misled by disabled or fake UI.

## Acceptance Criteria

1. Remove or relocate "Chưa khả dụng" health info block (chronic diseases, meds, allergies) — link to `/profiles` if appropriate.
2. Remove fake 2FA toggle or replace with "Sắp có" link to future story (user 2FA is Phase 2; admin MFA separate).
3. Remove hardcoded "85%" profile completion unless computed from API.
4. Support block uses same contact constants as `/settings/about` or public `/support`.
5. Change-password link points to `/settings/change-password` (after `pae-6`).

## Tasks / Subtasks

- [ ] Edit `apps/web/src/app/(dashboard)/settings/profile/page.tsx`.
- [ ] Remove unused `Link` import if still present (REVIEW-FULL-v2 §8.6).
- [ ] Fix `setTimeout` success message cleanup if still applicable (§8.7).

## Dev Notes

- Avatar upload done in `remaining-1-5` — do not regress.
- Nav "Hồ sơ của tôi" → consider `/profiles` vs `/settings/profile` (document decision).

### Likely Files

- `apps/web/src/app/(dashboard)/settings/profile/page.tsx`
- `apps/web/src/app/(dashboard)/layout.tsx` (optional nav fix)

## Dev Agent Record

### Agent Model Used

(pending)
