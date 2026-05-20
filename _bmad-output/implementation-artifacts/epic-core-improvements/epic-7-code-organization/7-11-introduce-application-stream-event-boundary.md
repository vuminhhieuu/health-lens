# Story 7.11: Introduce Application Stream/Event Boundary

Status: proposed

## Execution Scope

**Area:** Backend event publishing boundary, stream abstraction, source organization  
**Priority:** P1/P2

## Story

As a backend developer, I want a small application event boundary around stream publishing and consumer support code, so that Redis Stream usage is consistent and no longer duplicated across domain services.

## Acceptance Criteria

1. **Given** stream publishing occurs for OCR or email flows, **When** the story is complete, **Then** domain services publish through a focused event boundary instead of direct ad hoc stream calls.
2. **Given** event payloads and stream names are reviewed, **When** developers inspect the source tree, **Then** those concerns live under an explicit event package structure.
3. **Given** existing OCR and email flows run, **When** tests execute, **Then** behavior remains unchanged after the publishing boundary is introduced.

## Tasks / Subtasks

- [ ] Introduce a small event package structure for stream names, payload records, and publisher interfaces.
- [ ] Move direct `redisTemplate.opsForStream().add(...)` usage behind the chosen boundary where appropriate.
- [ ] Extract shared consumer bootstrap or support code only where duplication is real.
- [ ] Keep the abstraction small and aligned to current OCR and email use cases.
- [ ] Preserve retry, pending, and DLQ behavior already working in OCR.

## Dev Notes

- This is not a generic event framework story.
- Use only enough abstraction to centralize ownership and reduce duplication.
- Align package names and responsibilities with the architecture decision from `7-9`.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/dto/event/EmailEvent.java`
- `apps/api/src/main/java/com/healthlens/api/`

## References

- `epic-7-issues-and-proposed-stories.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
