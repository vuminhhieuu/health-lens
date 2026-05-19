# Story 3.5: Trusted Online RAG Source Adapter With Citation And Cache

Status: done

## Execution Scope

**Area:** Trusted source retrieval, online RAG governance, citation/cache/audit  
**Priority:** P1, P0 if online evidence becomes user-facing

## Story

As a product owner, I want online RAG limited to trusted and auditable sources, so that HealthLens can use updated knowledge without exposing users to unvetted web content.

## Acceptance Criteria

1. **Given** a source is not allowlisted, **When** online retrieval is attempted, **Then** the source is rejected.
2. **Given** a trusted source is retrieved, **When** content is used, **Then** snapshot metadata is persisted for audit.
3. **Given** retrieved online evidence is low quality or unreviewed, **When** AI output is generated, **Then** system excludes it or marks review required.
4. **Given** citation metadata exists, **When** admin/audit view inspects an answer, **Then** source URL, publisher, retrievedAt, snapshotHash, and reviewStatus are available.

## Tasks / Subtasks

- [x] Add trusted source allowlist.
- [x] Add source metadata model.
- [x] Add retrieval cache/snapshot hash.
- [x] Add review status and exclusion rules.
- [x] Add tests for rejected, cached, approved, and unreviewed sources.

### Review Findings

- [x] [Review][Defer] AC4 admin/audit inspection surface is ambiguous — deferred: Defer admin/audit inspection surface to a follow-up story because this story should finish adapter-level retrieval, citation metadata, persistence, and cache hardening first; answer-linked admin/audit API/UI needs separate scope.
- [x] [Review][Patch] Revalidate redirects and require HTTPS before fetching trusted online sources [apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourcePolicy.java:26]
- [x] [Review][Patch] Add HTTP timeouts and max response size before storing snapshots [apps/api/src/main/java/com/healthlens/api/service/rag/RestTemplateOnlineRagHttpClient.java:14]
- [x] [Review][Patch] Validate source URL, host, and publisher lengths before network fetch and save [apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourceAdapter.java:52]
- [x] [Review][Patch] Add cache freshness or explicit refresh policy for online snapshots [apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourceAdapter.java:52]


## Dev Notes

- Do not implement unrestricted web search.
- Treat online content as hostile until allowlisted, cached, and reviewed.

## Likely Files

- New RAG online adapter package under `apps/api/src/main/java/com/healthlens/api/`
- RAG retrieval service
- DB migration if metadata is persisted

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.5
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 24

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew --no-daemon test --tests com.healthlens.api.service.rag.TrustedOnlineRagSourceAdapterTest` - passed.
- `./gradlew --no-daemon test --tests com.healthlens.api.HealthLensApplicationTests --tests com.healthlens.api.service.AuthServiceIntegrationTest --tests com.healthlens.api.service.rag.TrustedOnlineRagSourceAdapterTest` - passed after adding constructor `@Autowired`.
- `./gradlew --no-daemon test` - passed, 430 tests.

### Completion Notes List

- Added an online RAG source policy backed by `app.ai.online-rag.allowlisted-hosts`, defaulting to an empty allowlist so online retrieval is opt-in.
- Added a trusted online RAG adapter that accepts explicit URLs only, rejects non-allowlisted hosts without HTTP access, snapshots allowlisted content, computes SHA-256 snapshot hashes, and returns content to AI only when the cached snapshot is approved and not excluded.
- Added persisted online source metadata including source URL, canonical host, publisher, retrievedAt, snapshotHash, reviewStatus, exclusion flag, content length, and content snapshot for audit/citation inspection.
- Added unit tests covering rejected, newly cached/review-required, approved cached, and unreviewed cached source behavior.

### File List

- apps/api/src/main/java/com/healthlens/api/entity/OnlineRagReviewStatus.java
- apps/api/src/main/java/com/healthlens/api/entity/OnlineRagSourceSnapshot.java
- apps/api/src/main/java/com/healthlens/api/repository/OnlineRagSourceSnapshotRepository.java
- apps/api/src/main/java/com/healthlens/api/service/rag/OnlineRagHttpClient.java
- apps/api/src/main/java/com/healthlens/api/service/rag/RestTemplateOnlineRagHttpClient.java
- apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourceAdapter.java
- apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourcePolicy.java
- apps/api/src/main/resources/application.yml
- apps/api/src/main/resources/db/migration/V037__create_online_rag_source_snapshots.sql
- apps/api/src/test/java/com/healthlens/api/service/rag/TrustedOnlineRagSourceAdapterTest.java

### Change Log

- 2026-05-19T14:03:27+07:00 - Implemented trusted online RAG source adapter with citation metadata, snapshot cache, review gating, and regression tests.

### Review Findings

- [x] [Review][Defer] AC4 admin/audit inspection surface is ambiguous — deferred: Defer admin/audit inspection surface to a follow-up story because this story should finish adapter-level retrieval, citation metadata, persistence, and cache hardening first; answer-linked admin/audit API/UI needs separate scope.
- [x] [Review][Patch] Revalidate redirects and require HTTPS before fetching trusted online sources [apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourcePolicy.java:26]
- [x] [Review][Patch] Add HTTP timeouts and max response size before storing snapshots [apps/api/src/main/java/com/healthlens/api/service/rag/RestTemplateOnlineRagHttpClient.java:14]
- [x] [Review][Patch] Validate source URL, host, and publisher lengths before network fetch and save [apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourceAdapter.java:52]
- [x] [Review][Patch] Add cache freshness or explicit refresh policy for online snapshots [apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourceAdapter.java:52]

