# Story 5.4: Right-To-Delete Race And Cleanup Correctness

Status: done

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

- [x] Store cancellation token hash, not raw token.
- [x] Remove email/sensitive values from cancellation URLs.
- [x] Add row locking / `SELECT FOR UPDATE SKIP LOCKED` or equivalent.
- [x] Add optimistic locking where needed.
- [x] Fix child/parent deletion ordering and storage object cleanup.
- [x] Add race/concurrency tests.

### Review Findings

- [x] [Review][Patch] Profile share audit logs can block profile deletion; retain audit logs by migrating FK columns to nullable `ON DELETE SET NULL` [apps/api/src/main/resources/db/migration/V027__create_profile_share_audit_logs_table.sql:28]
- [x] [Review][Patch] Incoming invitation email PII survives account deletion [apps/api/src/main/java/com/healthlens/api/repository/ProfileInvitationRepository.java:24]
- [x] [Review][Patch] Health-record object deletion can fail silently before records become unreachable [apps/api/src/main/java/com/healthlens/api/service/StorageService.java:299]
- [x] [Review][Patch] Explicit S3 multi-object delete is not chunked to the 1000-key request limit [apps/api/src/main/java/com/healthlens/api/service/StorageService.java:285]
- [x] [Review][Patch] OCR dead-letter rows are not cleaned or anonymized before health records are deleted [apps/api/src/main/java/com/healthlens/api/entity/OcrDeadLetter.java:25]
- [x] [Review][Patch] `FOR UPDATE SKIP LOCKED` query does not actually claim rows across scheduler instances [apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java:280]
- [x] [Review][Patch] Acceptance tests do not prove real token hashing, PostgreSQL locking race, or FK cleanup behavior [apps/api/src/test/java/com/healthlens/api/service/DataDeletionServiceTest.java:257]

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

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.DataDeletionServiceTest` — passed.
- `cd apps/api && ./gradlew test` — passed.
- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.DataDeletionServiceTest --tests com.healthlens.api.service.StorageServiceTest` — passed.
- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.DataDeletionServiceIntegrationTest` — passed.
- `cd apps/api && ./gradlew test` — passed after review patches.

### Completion Notes List

- Cancellation tokens are generated raw only for the outbound link, stored and looked up as SHA-256 hashes, and migrated from the old raw-token column to `cancellation_token_hash` using PostgreSQL `pgcrypto`.
- Cancellation URLs now contain only the token query parameter; request timestamps and user/email-sensitive values are not embedded in the URL.
- Scheduler lookup uses a native `FOR UPDATE SKIP LOCKED` due-request query, while cancellation and execution continue to take row-level write locks before state transitions.
- Added optimistic locking to `DataDeletionRequest` via JPA `@Version` and a Flyway `version` column.
- Deletion execution now collects health record file keys before deleting DB rows, deletes those exact objects, then runs prefix cleanup as a backstop.
- Cleanup ordering now explicitly removes health record shares/invitations, profile shares/invitations, follow-up reminders, OCR jobs, health records, profiles, auth tokens, and consent logs before user anonymization.
- Tests cover hashed token storage/link hygiene, skip-locked scheduler dispatch, explicit file-key cleanup, child cleanup calls, terminal-state cancellation handling, and full API regression.
- Review patches resolved: incoming invitee-email PII is deleted, OCR dead letters are removed before health records, exact object deletion now fails the deletion transaction on errors, explicit object deletes are chunked to S3 limits, and `SKIP LOCKED` now claims one request inside the deletion transaction.
- Profile share audit logs are retained while allowing profile deletion by migrating `profile_id` to nullable `ON DELETE SET NULL`.
- Added PostgreSQL integration coverage for real FK cleanup, invitee email deletion, OCR dead-letter cleanup, retained audit logs, and user anonymization.

### File List

- `apps/api/src/main/java/com/healthlens/api/entity/DataDeletionRequest.java`
- `apps/api/src/main/java/com/healthlens/api/repository/DataDeletionRequestRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/HealthRecordInvitationRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/HealthRecordRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/HealthRecordShareRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/OcrJobExecutionRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/OcrDeadLetterRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/ProfileInvitationRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/ProfileShareRepository.java`
- `apps/api/src/main/java/com/healthlens/api/entity/ProfileShareAuditLog.java`
- `apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java`
- `apps/api/src/main/java/com/healthlens/api/service/StorageService.java`
- `apps/api/src/main/resources/db/migration/V039__harden_data_deletion_token_and_cleanup_race.sql`
- `apps/api/src/main/resources/db/migration/V040__retain_profile_share_audit_logs_on_profile_delete.sql`
- `apps/api/src/test/java/com/healthlens/api/service/DataDeletionServiceIntegrationTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/DataDeletionServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/StorageServiceTest.java`

### Change Log

- 2026-05-19: Implemented right-to-delete race hardening, hashed cancellation-token storage, cleanup ordering, storage object deletion correctness, migration, and regression tests.
- 2026-05-19: Resolved code review findings for audit-log retention, invitee-email cleanup, OCR dead-letter cleanup, exact storage deletion failure handling/chunking, scheduler claim locking, and PostgreSQL integration coverage.
- 2026-05-19: Resolved Copilot PR comments by aligning stale comments, renaming cancellation-token hash accessors, and retaining SQL `pgcrypto` token-hash backfill by deployment choice.
