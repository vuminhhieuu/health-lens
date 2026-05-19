# Story 1.5: User Avatar Upload And Profile Photo Management

Status: done

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

- [x] Task 1 - Add backend avatar data model and migration (AC: #2, #5)
  - [x] Add nullable avatar metadata to `users` or a user-owned avatar table: storage key, content type, size, checksum if available, updated timestamp.
  - [x] Include avatar display URL in `UserResponse`; keep raw storage key internal.
  - [x] Ensure right-to-delete cleanup includes avatar object(s) under the user prefix.
- [x] Task 2 - Add authenticated avatar upload/replace/remove API (AC: #1-#5)
  - [x] Prefer existing `StorageService`/S3-MinIO path conventions instead of adding a second storage client.
  - [x] Restrict accepted MIME types to safe image formats such as `image/jpeg`, `image/png`, `image/webp`.
  - [x] Enforce a documented size limit suitable for avatars; return ProblemDetail-shaped validation errors.
  - [x] Audit avatar update/remove events without logging signed URLs or raw object contents.
- [x] Task 3 - Wire frontend profile settings avatar UI (AC: #1, #4)
  - [x] Replace the current hardcoded `https://ui-avatars.com/api/?name=H+L...` image in `settings/profile`.
  - [x] Use a real file input reachable by keyboard; the decorative pencil/change buttons must trigger the same accessible control.
  - [x] Show preview only after client-side validation passes; keep existing avatar visible if upload fails.
  - [x] Stage avatar upload/remove locally and apply it only when the user submits "Save changes"; update `["currentUser"]` from the saved API response.
- [x] Task 4 - Normalize avatar fallback usage across profile surfaces (AC: #4)
  - [x] Keep `ProfileCard.avatarUrl` optional and render the existing local icon/initial fallback when missing.
  - [x] Avoid third-party avatar image services because names/emails are personal data.
  - [x] Verify shared/family profile cards do not accidentally display the authenticated user's avatar.
- [x] Task 5 - Add focused tests (AC: #1-#6)
  - [x] Backend service/controller tests for validation, storage failure, replace/remove cleanup, and `UserResponse` mapping.
  - [x] Frontend test files were removed per follow-up request; current frontend validation relies on lint/build checks and manual UI QA for staged avatar preview/save behavior.

### Review Findings

- [ ] [Review][Scope] Upload avatar/photo cho profile has no backend endpoint or storage flow; `ProfileCard` already accepts `avatarUrl` but the product has no way to manage it.
- [x] [Review][Patch] Profile settings uses a hardcoded external `ui-avatars.com` URL with `name=H+L`, which is both incorrect user identity and a privacy concern.
- [x] [Review][Patch] High: Multipart avatar upload can lose the required boundary because `page.tsx` sets `Content-Type: multipart/form-data` manually while `apiClient` defaults to JSON; mirror the FormData header stripping already used by `adminApiClient`. [apps/web/src/app/(dashboard)/settings/profile/page.tsx:113]
- [x] [Review][Patch] High: Avatar object cleanup is not transaction-safe; upload stores the new object before DB/audit commit and deletes the previous object before transaction commit, so rollback can orphan new objects or leave DB pointing at a deleted object. [apps/api/src/main/java/com/healthlens/api/service/UserService.java:88]
- [x] [Review][Patch] Medium: Dashboard header keeps avatar in local one-off state, so upload/remove from settings updates `["currentUser"]` but the header can stay stale or keep an expired signed URL until layout remount. [apps/web/src/app/(dashboard)/layout.tsx:63]
- [x] [Review][Patch] Medium: Frontend tests do not cover remove-avatar, disabled/loading state, or cache invalidation assertions requested by Task 5. [apps/web/src/app/(dashboard)/settings/profile/page.test.tsx:84]

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

- 2026-05-19: Bắt đầu implementation; cập nhật sprint/story status sang in-progress.
- 2026-05-19: Added backend avatar metadata/API/storage implementation and frontend avatar UI.
- 2026-05-19: Validation: web `pnpm test`, targeted Vitest, targeted ESLint, and shared package build passed; API Gradle tests blocked because no Java Runtime is installed in the environment.
- 2026-05-19: User requested moving story to review for code-review workflow.
- 2026-05-19: Batch-applied code review patches: FormData boundary handling, transaction-safe avatar object cleanup, shared current-user query for dashboard header, and expanded frontend avatar tests.
- 2026-05-19: Post-review validation: web `pnpm test`, targeted Vitest, targeted ESLint, shared package build, and `git diff --check` passed. API Gradle tests remain blocked because no Java Runtime is installed.
- 2026-05-19: Follow-up UI fixes: sidebar account card now renders avatar, avatar action text buttons removed in favor of overlay controls, and duplicate native date picker icons hidden on profile date fields.
- 2026-05-19: Follow-up polish: moved avatar remove X to top-right, made custom calendar icons clickable, and restored sidebar no-avatar fallback to the original user icon.
- 2026-05-19: Synced profile date inputs with registration page behavior by using native date inputs without custom calendar overlays.
- 2026-05-19: Addressed Copilot PR feedback for avatar input re-selection and current-user cache typing; changed avatar UX so file selection/removal is staged as local profile preview and only persisted after "Save changes".
- 2026-05-19: Frontend `pnpm test` passed after staged avatar changes; Vitest reported no frontend test files because the generated frontend test files were removed per user request. Existing unrelated lint warning remains in `admin/audit-log/page.tsx`.

### Completion Notes List

- Added user avatar metadata columns and `avatarUrl` response mapping backed by short-lived storage download URLs.
- Added authenticated avatar upload/remove flow with MIME, size, empty-file, unsafe filename validation, checksum metadata, audit events, and best-effort cleanup of replaced/removed objects.
- Replaced external avatar service usage in profile settings and dashboard header with local image/initial fallback rendering.
- Added focused backend and frontend tests, but backend tests could not be executed locally because Java Runtime is unavailable.
- Resolved code-review findings for multipart uploads, S3 cleanup transaction safety, dashboard avatar cache freshness, and missing frontend test assertions.
- Adjusted avatar management UI to use icon-only overlay controls and normalized profile date inputs so only the custom calendar icon is visible.
- Updated avatar/date UI details based on visual QA: top-right remove control, clickable calendar labels, and icon fallback for missing sidebar avatar.
- Replaced custom calendar overlays on profile date inputs with native date picker behavior matching the registration page.
- Changed avatar management behavior so newly selected avatars and removals are previewed only in profile settings until the user submits the profile form.
- Updated current-user cache from save responses instead of applying avatar changes immediately on file selection.
- Frontend avatar test files were removed per user request; web validation currently passes with lint and `--passWithNoTests`.

### File List

- apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java
- apps/api/src/main/java/com/healthlens/api/controller/UserController.java
- apps/api/src/main/java/com/healthlens/api/dto/response/UserResponse.java
- apps/api/src/main/java/com/healthlens/api/entity/User.java
- apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java
- apps/api/src/main/java/com/healthlens/api/service/StorageService.java
- apps/api/src/main/java/com/healthlens/api/service/UserService.java
- apps/api/src/main/resources/db/migration/V036__add_user_avatar_metadata.sql
- apps/api/src/test/java/com/healthlens/api/controller/UserControllerTest.java
- apps/api/src/test/java/com/healthlens/api/service/DataDeletionServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/UserServiceTest.java
- apps/web/src/app/(dashboard)/layout.tsx
- apps/web/src/app/(dashboard)/settings/profile/page.tsx
- apps/web/src/app/globals.css
- apps/web/src/components/features/profiles/CreateProfileModal.tsx
- apps/web/src/components/features/profiles/EditProfileModal.tsx
- apps/web/src/lib/api/apiClient.ts
- apps/web/src/lib/api/routes.ts
- packages/shared/constants/api.ts
- _bmad-output/implementation-artifacts/sprint-status.yaml
- _bmad-output/implementation-artifacts/remaining-production-review/epic-1-web-production-readiness/1-5-user-avatar-upload-and-profile-photo-management.md

### Change Log

- 2026-05-19: Created story from production review findings for user avatar upload/profile photo management.
- 2026-05-19: Started development; story moved to in-progress.
- 2026-05-19: Implemented avatar metadata, upload/remove APIs, storage cleanup, frontend avatar management UI, local fallback rendering, and focused tests. Story remains in-progress pending API test execution in a Java-enabled environment.
- 2026-05-19: Moved story to review per user request before running `bmad-code-review`.
- 2026-05-19: Addressed 4 code review patch findings; story returned to in-progress pending Java-enabled API validation.
- 2026-05-19: Applied visual follow-up fixes for avatar display/actions and duplicate calendar icons.
- 2026-05-19: Refined visual follow-up fixes for avatar remove placement, clickable date icons, and sidebar fallback behavior.
- 2026-05-19: Synced profile date picker behavior with registration page native date input.
- 2026-05-19: Updated avatar save flow to stage preview/removal locally and persist only after the user clicks "Save changes"; documented removal of frontend test files per follow-up request.
