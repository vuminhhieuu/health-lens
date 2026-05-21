# Story 9: Profile Settings Stub Cleanup

Status: done

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

- [x] Edit `apps/web/src/app/(dashboard)/settings/profile/page.tsx`.
- [x] Remove unused `Link` import if still present (REVIEW-FULL-v2 §8.6).
- [x] Fix `setTimeout` success message cleanup if still applicable (§8.7).

### Review Findings

- [x] [Review][Patch] 2FA chỉ hiển thị badge "Sắp có", không có link/aria mô tả Phase 2 [apps/web/src/app/(dashboard)/settings/profile/page.tsx:524]

## Dev Notes

- Avatar upload done in `remaining-1-5` — do not regress.
- Nav "Hồ sơ của tôi" → consider `/profiles` vs `/settings/profile` (document decision).

### Likely Files

- `apps/web/src/app/(dashboard)/settings/profile/page.tsx`
- `apps/web/src/app/(dashboard)/layout.tsx` (optional nav fix)

## Dev Agent Record

### Agent Model Used

Composer

### Implementation Plan

- Removed disabled health stub; added CTA to `/profiles` for family profiles and exam history.
- Replaced fake 2FA row with non-interactive "Sắp có" badge (Phase 2 user 2FA).
- Removed hardcoded 85% completion card.
- Kept `SettingsDirectContactCard` (pae-8 support constants) and `/settings/change-password` link.
- Nav decision: keep split — "Hồ sơ của tôi" → account `/settings/profile`; "Hồ sơ gia đình" → `/profiles` (layout unchanged).

### Completion Notes

- Trang hồ sơ cài đặt chỉ còn tính năng hoạt động: không block sức khỏe giả, không % hoàn thiện cứng, 2FA hiển thị "Sắp có".
- CTA quản lý sức khỏe/hồ sơ gia đình trỏ `/profiles`; đổi mật khẩu và liên hệ hỗ trợ giữ nguyên (pae-6, pae-8).
- Kiểm tra thủ công: mở `/settings/profile`, xác nhận không còn UI gây hiểu nhầm.

### File List

- `apps/web/src/app/(dashboard)/settings/profile/page.tsx`

### Change Log

- 2026-05-21: Removed health stub, fake completion %, and fake 2FA; linked health management to `/profiles`.
