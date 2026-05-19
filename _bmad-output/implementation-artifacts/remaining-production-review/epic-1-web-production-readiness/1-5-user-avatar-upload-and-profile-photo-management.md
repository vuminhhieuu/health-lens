# Story 1.5: User Avatar Upload And Profile Photo Management

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / web profile readiness  
**Area:** User profile settings, avatar upload, storage-backed user photo, privacy-safe image rendering  
**Priority:** P2

## Story

As a HealthLens user,  
I want to upload and manage my own avatar,  
so that my account/profile identity is recognizable without relying on hardcoded or external avatar services.

## Acceptance Criteria

1. User can upload a profile avatar from the profile settings page using an image-only control with clear loading, success, error, and keyboard-accessible states.
2. Backend stores avatar metadata for the authenticated user and exposes `avatarUrl` or an equivalent signed/display URL through `GET /api/v1/users/me`.
3. Uploaded avatar validation rejects unsupported content types, oversized files, empty files, and unsafe filenames before persisting metadata.
4. The profile settings page and profile cards stop using the hardcoded `ui-avatars.com` URL; fallback uses local initials/icon rendering derived from the user/profile name.
5. Replacing or removing an avatar cleans up the previous storage object where feasible and never blocks account/profile data updates.
6. Tests cover upload success, validation errors, storage failure, avatar replacement/removal, user response mapping, and frontend fallback rendering.

## Tasks / Subtasks

- [ ] Task 1 - Add backend avatar data model and migration (AC: #2, #5)
  - [ ] Add nullable avatar metadata to `users` or a user-owned avatar table: storage key, content type, size, checksum if available, updated timestamp.
  - [ ] Include avatar display URL in `UserResponse`; keep raw storage key internal.
  - [ ] Ensure right-to-delete cleanup includes avatar object(s) under the user prefix.
- [ ] Task 2 - Add authenticated avatar upload/replace/remove API (AC: #1-#5)
  - [ ] Prefer existing `StorageService`/S3-MinIO path conventions instead of adding a second storage client.
  - [ ] Restrict accepted MIME types to safe image formats such as `image/jpeg`, `image/png`, `image/webp`.
  - [ ] Enforce a documented size limit suitable for avatars; return ProblemDetail-shaped validation errors.
  - [ ] Audit avatar update/remove events without logging signed URLs or raw object contents.
- [ ] Task 3 - Wire frontend profile settings avatar UI (AC: #1, #4)
  - [ ] Replace the current hardcoded `https://ui-avatars.com/api/?name=H+L...` image in `settings/profile`.
  - [ ] Use a real file input reachable by keyboard; the decorative pencil/change buttons must trigger the same accessible control.
  - [ ] Show preview only after client-side validation passes; keep existing avatar visible if upload fails.
  - [ ] Invalidate `["currentUser"]` after upload/remove and update auth/user cache if applicable.
- [ ] Task 4 - Normalize avatar fallback usage across profile surfaces (AC: #4)
  - [ ] Keep `ProfileCard.avatarUrl` optional and render the existing local icon/initial fallback when missing.
  - [ ] Avoid third-party avatar image services because names/emails are personal data.
  - [ ] Verify shared/family profile cards do not accidentally display the authenticated user's avatar.
- [ ] Task 5 - Add focused tests (AC: #1-#6)
  - [ ] Backend service/controller tests for validation, storage failure, replace/remove cleanup, and `UserResponse` mapping.
  - [ ] Frontend tests for disabled/loading state, invalid file rejection, success toast/cache invalidation, and fallback rendering.

### Review Findings

- [ ] [Review][Scope] Upload avatar/photo cho profile has no backend endpoint or storage flow; `ProfileCard` already accepts `avatarUrl` but the product has no way to manage it.
- [ ] [Review][Patch] Profile settings uses a hardcoded external `ui-avatars.com` URL with `name=H+L`, which is both incorrect user identity and a privacy concern.

## Dev Notes

### Implementation Guardrails

- Treat avatar as account identity, not medical data. Do not store it on health records.
- Do not send full name/email to external avatar services. Use local initials/icon fallback.
- Do not save user profile form fields when the user only changes avatar, unless the user explicitly submits the profile form.
- Avoid broad image processing dependencies unless needed for validation; MIME, size, and object-key controls are the minimum production requirement.
- Keep signed download URLs short-lived if the bucket is private.

### Current Code Intelligence

- `apps/web/src/app/(dashboard)/settings/profile/page.tsx` currently renders the account avatar area and hardcodes `https://ui-avatars.com/api/?name=H+L&background=00685f&color=fff&size=256`.
- `apps/web/src/components/features/profiles/ProfileCard.tsx` already supports optional `avatarUrl` and falls back to a local `User` icon.
- `apps/api/src/main/java/com/healthlens/api/entity/User.java` currently has no avatar fields.
- `apps/api/src/main/java/com/healthlens/api/dto/response/UserResponse.java` currently returns id, email, fullName, birthDate, gender, emailVerified, consentGiven.
- `apps/api/src/main/java/com/healthlens/api/service/StorageService.java` already supports signed PUT/GET URLs and object deletion by prefix.
- Existing DB migrations live under `apps/api/src/main/resources/db/migration/`; next migration should follow the current sequence.

### Likely Files

- `apps/web/src/app/(dashboard)/settings/profile/page.tsx`
- `apps/web/src/components/features/profiles/ProfileCard.tsx`
- `apps/web/src/lib/api/routes.ts`
- `packages/shared/constants/api.ts`
- `apps/api/src/main/java/com/healthlens/api/controller/UserController.java`
- `apps/api/src/main/java/com/healthlens/api/service/UserService.java`
- `apps/api/src/main/java/com/healthlens/api/service/StorageService.java`
- `apps/api/src/main/java/com/healthlens/api/entity/User.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/UserResponse.java`
- `apps/api/src/main/resources/db/migration/V036__*.sql`
- `apps/api/src/test/java/com/healthlens/api/service/UserServiceTest.java`

### References

- `_bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md` finding 5.1 and 8.8
- `_bmad-output/planning-artifacts/review-source/PRODUCTION-READINESS.md` finding 5.1 and 8.8
- `_bmad-output/planning-artifacts/ux-design-specification.md` avatar references
- `_bmad-output/implementation-artifacts/remaining-production-review/epic-1-web-production-readiness/1-4-profile-and-history-screen-type-safety-and-edge-cases.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

TBD

### Completion Notes List

TBD

### File List

TBD

### Change Log

- 2026-05-19: Created story from production review findings for user avatar upload/profile photo management.
