# Story 3.3: RAG Corpus Governance And Admin Review Workflow

Status: done

## Execution Scope

**Area:** RAG corpus metadata, ingestion governance, approval/versioning  
**Priority:** P1

## Story

As an admin maintaining medical explanation content, I want RAG corpus changes to be versioned, reviewed, and auditable, so that AI explanations use trustworthy Vietnamese medical context.

## Acceptance Criteria

1. **Given** a corpus update is ingested, **When** ingestion completes, **Then** report includes source version, chunk count, embedding model, and errors.
2. **Given** a corpus chunk is not approved, **When** retrieval runs, **Then** the chunk is not used in user-facing explanation.
3. **Given** a corpus version causes issues, **When** admin rolls back, **Then** previous approved version becomes active.
4. **Given** corpus metadata is inspected, **When** audit requires evidence, **Then** reviewer/effective date/source version are available.

## Tasks / Subtasks

- [x] Add corpus metadata model.
- [x] Add ingestion report.
- [x] Add approval/effective version rules.
- [x] Add rollback process.
- [x] Add retrieval filter for approved active corpus only.

### Review Findings

- [x] [Review][Patch] Ingestion auto-approves every corpus update instead of separating review/approval [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java:78]
- [x] [Review][Patch] Corpus governance state is in-memory, so rollback/audit/active version disappear after restart [apps/api/src/main/java/com/healthlens/api/service/RagCorpusGovernanceService.java:21]
- [x] [Review][Patch] Blank or null sourceVersion can be written as approved corpus metadata but never become retrievable [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java:64]
- [x] [Review][Patch] Empty corpus can become active and mask the previous usable version [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java:74]
- [x] [Review][Patch] Re-ingesting the same sourceVersion can leave stale chunks from the previous ingest [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java:109]
- [x] [Review][Patch] Ingested language metadata is not normalized to match retrieval filter normalization [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java:114]
- [x] [Review][Patch] Rollback mutates state before validating that rollback is possible [apps/api/src/main/java/com/healthlens/api/service/RagCorpusGovernanceService.java:60]
- [x] [Review][Patch] Rollback overwrites original approval audit metadata for the problematic version [apps/api/src/main/java/com/healthlens/api/service/RagCorpusGovernanceService.java:63]

## Dev Notes

- Do not duplicate admin reference-data approval workflow unless corpus becomes admin-managed through the same UI.
- Keep `metric-explanations.vi.json` reviewable and versioned.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java`
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`
- `apps/api/src/main/resources/ai/metric-explanations.vi.json`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.3
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 11

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.service.MetricExplanationIngestionServiceTest' --tests 'com.healthlens.api.service.MetricExplanationRetrievalServiceTest'` - passed.
- `cd apps/api && ./gradlew test` - passed.
- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.service.MetricExplanationIngestionServiceTest' --tests 'com.healthlens.api.service.MetricExplanationRetrievalServiceTest' --tests 'com.healthlens.api.service.RagCorpusGovernanceServiceTest'` - passed after review fixes.
- `cd apps/api && ./gradlew check` - passed.

### Implementation Plan

- Add an in-process RAG corpus governance registry for approved corpus version metadata, active version lookup, audit inspection, and rollback to the previous approved version.
- Extend metric explanation ingestion to return a structured report and stamp every ingested chunk with source version, approval status, reviewer, effective date, and embedding config metadata.
- Gate metric explanation retrieval by active approved corpus version, with reference-data/generic fallback when no approved corpus is active or Qdrant misses.

### Completion Notes List

- Implemented `RagCorpusGovernanceService` with approved-version metadata, active approved version lookup, rollback, and metadata inspection.
- Added `MetricExplanationIngestionService.IngestionReport`; ingestion now reports source version, chunk count, embedding model/dimension, reviewer/effective date, and errors.
- Added governed RAG metadata to ingested documents and updated ingestion job logging to include report fields.
- Updated retrieval to search only approved documents from the current active corpus version; unapproved/no-active corpus paths safely fall back.
- Added unit coverage for ingestion reports, audit metadata, active approved retrieval filtering, no-active fallback, and rollback behavior.
- Resolved code review findings: corpus versions are now persisted in `rag_corpus_versions`, ingestion records `pending_review` versions instead of auto-approval, retrieval uses durable active approved version state, invalid/empty/duplicate versions are rejected before upsert, language metadata is normalized, and rollback validates previous state before mutation while preserving approval audit metadata.

### File List

- `apps/api/src/main/java/com/healthlens/api/entity/RagCorpusVersion.java`
- `apps/api/src/main/java/com/healthlens/api/repository/RagCorpusVersionRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/RagCorpusGovernanceService.java`
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionJob.java`
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java`
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`
- `apps/api/src/main/resources/db/migration/V035__create_rag_corpus_versions.sql`
- `apps/api/src/test/java/com/healthlens/api/service/MetricExplanationIngestionServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/RagCorpusGovernanceServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/MetricExplanationRetrievalServiceTest.java`

### Change Log

- 2026-05-19: Implemented RAG corpus governance/reporting/rollback and active approved retrieval filtering.
- 2026-05-19: Resolved code review findings for durable governance, explicit approval state, validation guards, language normalization, rollback safety, and audit preservation.
