# Story 5.4: Right-To-Delete Race And Cleanup Correctness

Status: ready-for-dev

## Execution Scope

**Area:** Data deletion security, token hashing, scheduler race, storage/FK cleanup  
**Priority:** P0

## Story

As a user exercising right-to-delete, I want account and health data deletion to be complete and race-safe, so that privacy obligations are met within the required window.

## Acceptance Criteria

1. **Given** deletion job and cancel request race, **When** both execute concurrently, **Then** only one valid final state is committed.
2. **Given** deletion completes, **When** cleanup verification runs, **Then** user health records, files, refresh tokens, invites, and profile shares are removed or anonymized according to policy.
3. **Given** deletion token leaks from logs, **When** inspected, **Then** raw token is not present because stored/logged value is hashed or redacted.
4. **Given** storage files are associated with health records, **When** bulk deletion runs, **Then** file keys are collected and objects deleted before records become unreachable.

## Tasks / Subtasks

- [ ] Store cancellation token hash, not raw token.
- [ ] Remove email/sensitive values from cancellation URLs.
- [ ] Add row locking / `SELECT FOR UPDATE SKIP LOCKED` or equivalent.
- [ ] Add optimistic locking where needed.
- [ ] Fix child/parent deletion ordering and storage object cleanup.
- [ ] Add race/concurrency tests.

## Dev Notes

- This supersedes old `epic-10/10-3-data-deletion-security.md` and `epic-10/10-11-database-constraint-cleanup.md` for right-to-delete cleanup scope.
- Keep audit-retention policy consistent with Story 4.1.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java`
- `apps/api/src/main/java/com/healthlens/api/entity/DataDeletionRequest.java`
- `apps/api/src/main/java/com/healthlens/api/repository/DataDeletionRequestRepository.java`
- `apps/api/src/main/resources/db/migration/`

## References

- Old sources: `epic-10/10-3-data-deletion-security.md`, `epic-10/10-11-database-constraint-cleanup.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 5.4

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
