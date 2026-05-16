# Story 7.4: Email Template Cleanup

Status: ready-for-dev

## Execution Scope

**Area:** Backend email templates, Vietnamese copy, maintainability  
**Priority:** P2; P1 for auth/privacy emails

## Story

As a maintainer, I want email HTML stored in templates instead of long inline strings, so that copy, localization, and tests are easier to manage.

## Acceptance Criteria

1. **Given** password reset email is sent, **When** template is rendered, **Then** output uses a template file and correct variables.
2. **Given** deletion/cancellation/invitation email is sent, **When** user opens it, **Then** copy is Vietnamese with diacritics.
3. **Given** email copy changes, **When** tests run, **Then** template rendering remains valid.
4. **Given** EmailService is reviewed, **When** code is inspected, **Then** long inline HTML blocks are removed or minimized.

## Tasks / Subtasks

- [ ] Move password reset email to template.
- [ ] Move cancellation/completion/invitation emails to templates where still inline.
- [ ] Normalize Vietnamese copy.
- [ ] Add template rendering tests.
- [ ] Keep service focused on variables and send behavior.

## Dev Notes

- Coordinate with Story 6.3 language normalization.
- Avoid changing email URLs/security tokens beyond active deletion/security stories.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/EmailService.java`
- `apps/api/src/main/resources/templates/email/`
- `apps/api/src/test/java/com/healthlens/api/service/`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 7.4

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
