# Story 6.5: Medical Copy And Disclaimer Review

Status: ready-for-dev

## Execution Scope

**Area:** AI/medical copy, disclaimers, recommendation safety  
**Priority:** P1

## Story

As a user reading AI health explanations, I want the language to be clear that HealthLens is informational, so that I do not mistake the app for diagnosis or medical instruction.

## Acceptance Criteria

1. **Given** AI gives lifestyle recommendations, **When** user views them, **Then** copy includes concise medical disclaimer.
2. **Given** a metric is abnormal, **When** explanation is shown, **Then** it avoids diagnosis language and recommends appropriate follow-up.
3. **Given** prompt/output tests run, **When** disclaimer is missing, **Then** tests fail.
4. **Given** copy is reviewed, **When** changes are applied, **Then** Vietnamese tone remains clear and consistent.

## Tasks / Subtasks

- [ ] Review LLM recommendation prompt and fallback copy.
- [ ] Standardize disclaimer text.
- [ ] Add tests/checks for required disclaimer.
- [ ] Update frontend display copy if needed.

## Dev Notes

- Do not overstate certainty. HealthLens should not diagnose.
- Coordinate with Story 3.1/3.2.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 6.5

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
