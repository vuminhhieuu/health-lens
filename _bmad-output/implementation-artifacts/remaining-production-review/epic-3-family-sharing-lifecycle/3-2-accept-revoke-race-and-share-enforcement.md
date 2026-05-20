# Story 3.2: Accept/Revoke Race And Share Enforcement

Status: done

## Execution Scope

**Phase:** Remaining production review / family sharing correctness  
**Area:** Invitation accept/revoke concurrency, profile share uniqueness, access enforcement  
**Priority:** P0

## Story

As a profile owner,  
I want revoke and accept operations to be race-safe,  
so that revoked access cannot become active again.

## Acceptance Criteria

1. Concurrent accept and revoke result in one valid final state.
2. Duplicate `ProfileShare` rows cannot be created by concurrent accepts.
3. Revoked users lose access immediately across API and UI.
4. Wrong revoke fallback ID behavior is fixed and tested.
5. Tests cover owner/viewer invite, accept, revoke, concurrent accept, concurrent revoke, and post-revoke access.

## Tasks / Subtasks

- [x] Task 1 - Audit share/invitation data model and constraints (AC: #1, #2)
- [x] Task 2 - Add database/service-level concurrency protection (AC: #1, #2)
- [x] Task 3 - Enforce revoke immediately in API authorization and UI state (AC: #3) — API đã dùng `revoked_at`; client có xử lý `PROFILE_ACCESS_REVOKED` (apiClient)
- [x] Task 4 - Fix revoke fallback ID behavior (AC: #4)
- [x] Task 5 - Add concurrency and lifecycle tests (AC: #1-#5)

## Dev Notes

### Implementation Guardrails

- Do not rely only on frontend state to enforce revoked access.
- Use database constraints or transactional checks for duplicate share prevention.
- Treat accept/revoke races as security-relevant, not just UX edge cases.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/FamilySharingService.java`
- `apps/api/src/main/java/com/healthlens/api/repository/*Share*`
- `apps/api/src/main/java/com/healthlens/api/entity/ProfileShare.java`
- `apps/web/src/app/(dashboard)/*sharing*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 3.2
- `_bmad-output/planning-artifacts/review-source/production-review/p0-gates-checklist.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

- DB đã có partial unique index `uq_profile_shares_profile_viewer_active` (V015); bổ sung khóa `PESSIMISTIC_WRITE` trên invitation theo token + xử lý `DataIntegrityViolationException` khi insert share trùng.
- Thu hồi chỉ theo `(profileId, viewerId)` — bỏ fallback `findById` trên path segment để tránh nhầm UUID share vs viewer.

### File List

- `apps/api/src/main/java/com/healthlens/api/repository/ProfileInvitationRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java`
- `apps/api/src/test/java/com/healthlens/api/service/ProfileShareServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/ProfileShareConcurrencyIntegrationTest.java`
