# Story 6.3: Vietnamese Language Normalization And Message Catalog

Status: done

## Execution Scope

**Area:** Frontend/backend copy, Vietnamese with diacritics, error code/message catalog, email copy  
**Priority:** P1; P0 for auth/security/compliance copy

## Story

As a Vietnamese user, I want all user-facing messages to use proper Vietnamese with diacritics, so that the product feels professional and clear.

## Acceptance Criteria

1. **Given** auth error occurs, **When** user sees message, **Then** it is Vietnamese with diacritics and consistent tone.
2. **Given** backend returns an error, **When** frontend maps it, **Then** stable error code is preferred over brittle raw message matching.
3. **Given** password reset/deletion/invitation email is sent, **When** user opens it, **Then** copy uses Vietnamese with diacritics.
4. **Given** tests assert user-facing copy, **When** copy is normalized, **Then** tests are updated to expected diacritic copy or error codes.

## Tasks / Subtasks

- [ ] Add frontend message catalog.
- [ ] Add backend error code/message strategy.
- [ ] Normalize auth, validation, deletion, sharing, and email copy.
- [ ] Update tests.
- [ ] Normalize common no-diacritic user-facing strings found during review.

### Review Findings

- [x] [Review][Patch] User-facing no-diacritic strings remained after initial normalization
- [x] [Review][Patch] Forgot-password rate-limit detail still renders `phut`/`giay` without diacritics [apps/api/src/main/java/com/healthlens/api/security/ForgotPasswordRateLimiter.java:38]
- [x] [Review][Patch] JWT filter pending-deletion response lacks stable `errorCode` and still uses an English title [apps/api/src/main/java/com/healthlens/api/security/JwtAuthenticationFilter.java:148]
- [x] [Review][Patch] Login pending-deletion UI depends only on `errorCode` and no longer falls back to the stable error `type` [apps/web/src/lib/i18n/messages.ts:104]
- [x] [Review][Patch] Profile invitation accept still displays raw backend `detail` before stable catalog/code mapping [apps/web/src/app/(auth)/invitations/accept/page.tsx:70]
- [x] [Review][Patch] Health-record invitation accept flow was not moved to the shared message catalog [apps/web/src/app/(auth)/health-record-invitations/accept/page.tsx:62]
- [x] [Review][Patch] `retryAfterMinutes` can render invalid minute counts from malformed API payloads [apps/web/src/lib/i18n/messages.ts:85]
- [x] [Review][Patch] Register still collapses backend auth validation/error codes into a generic failure message [apps/web/src/app/(auth)/register/page.tsx:77]
- [x] [Review][Defer] Cancel-deletion client treats every backend 409 as cancellation success [apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx:148] — deferred, pre-existing

## Dev Notes

- Do not treat test names/comments as user-facing unless they leak into UI.
- Backend can return both stable code and localized message.

## Likely Files

- `apps/web/src/lib/i18n/messages.ts`
- `apps/api/src/main/java/com/healthlens/api/exception/*`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailService.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/*`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 6.3

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-05-17: Added frontend message catalog and API error-code strategy; web validation passed.
- 2026-05-17: `./gradlew test` could not run because the local environment has no Java Runtime.
- 2026-05-17: Code review patches applied; i18n guard and web validation passed.

### Completion Notes List

- Implemented frontend Vietnamese message catalog with stable API error-code extraction and auth/deletion/sharing mappings.
- Added backend `ApiErrorCode` catalog and `errorCode` properties to `ProblemDetail` responses.
- Normalized auth, validation, deletion, sharing, admin auth, rate-limit, and email copy to Vietnamese with diacritics in targeted user-facing paths.
- Updated frontend and API tests to assert diacritic copy and stable `errorCode` fields where applicable.
- Normalized common no-diacritic user-facing strings found during implementation and review.
- Addressed review findings for i18n guard coverage, pending-deletion error codes, catalog-based invitation/register errors, and retry-after validation.
- API regression tests remain blocked until Java is available locally.

### File List

- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-6-frontend-consistency/6-3-vietnamese-language-normalization-and-message-catalog.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/AdminTotpVerifyRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/CreateUploadUrlRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/DeleteAccountRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/ForgotPasswordRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/InviteHealthRecordRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/LoginRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/ResetPasswordRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/VerifyEmailRequest.java`
- `apps/api/src/main/java/com/healthlens/api/exception/ApiErrorCode.java`
- `apps/api/src/main/java/com/healthlens/api/exception/GlobalExceptionHandler.java`
- `apps/api/src/main/java/com/healthlens/api/security/AdminAuthRateLimiter.java`
- `apps/api/src/main/java/com/healthlens/api/security/ForgotPasswordRateLimiter.java`
- `apps/api/src/main/java/com/healthlens/api/security/LoginRateLimiter.java`
- `apps/api/src/main/java/com/healthlens/api/service/AdminAuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordShareService.java`
- `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java`
- `apps/api/src/test/java/com/healthlens/api/controller/AuthControllerTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/AdminAuthServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java`
- `apps/web/src/app/(auth)/forgot-password/page.tsx`
- `apps/web/src/app/(auth)/invitations/accept/page.tsx`
- `apps/web/src/app/(auth)/login/page.tsx`
- `apps/web/src/app/(auth)/register/page.tsx`
- `apps/web/src/app/(auth)/reset-password/page.tsx`
- `apps/web/src/hooks/useAccountDeletion.ts`
- `apps/web/src/lib/i18n/messages.ts`
- `apps/web/src/lib/i18n/messages.test.ts`
- `package.json`

## Change Log

- 2026-05-17: Implemented Vietnamese message catalog, backend error codes, normalized target copy, updated tests, and added i18n grep guard. API test execution blocked by missing Java runtime.
