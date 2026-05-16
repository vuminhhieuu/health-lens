# Story 6.3: Vietnamese Language Normalization And Message Catalog

Status: ready-for-dev

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
- [ ] Add grep check for common no-diacritic user-facing strings.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
