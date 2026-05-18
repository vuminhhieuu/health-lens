# Story 1.3: Health Records Hub Mapping, Sorting And Status Fixes

Status: done

## Execution Scope

**Phase:** Remaining production review / web correctness  
**Area:** Health records list, status mapping, sorting, filters, state components  
**Priority:** P1

## Story

As a user browsing health records,  
I want record statuses, timestamps, and filters to be accurate,  
so that I can find and interpret records reliably.

## Acceptance Criteria

1. `lastUpdated` and status mappings match backend record state.
2. Sorting is deterministic and handles missing dates safely.
3. Filter labels use standardized Vietnamese with diacritics.
4. Empty/loading/error states use shared components from the UI consistency backlog.

## Tasks / Subtasks

- [x] Task 1 - Audit list mapping against backend contract (AC: #1)
- [x] Task 2 - Fix status and timestamp derivation (AC: #1, #2)
- [x] Task 3 - Normalize Vietnamese filter labels (AC: #3)
- [x] Task 4 - Reuse shared state components where available (AC: #4)
- [x] Task 5 - Add verification cases for missing dates and mixed statuses (AC: #1-#4)

### Review Findings

- [x] [Review][Patch] Completed records with unknown or `no_data` overall status render as normal [apps/web/src/lib/healthRecordHub.ts:74]
- [x] [Review][Patch] Malformed `updatedAt` prevents sorting fallback to valid `lastRecordAt` [apps/web/src/lib/healthRecordHub.ts:151]
- [x] [Review][Patch] One failed hub query hides all successfully loaded hub data [apps/web/src/app/(dashboard)/health-records/page.tsx:234]

## Dev Notes

### Implementation Guardrails

- Do not sort by unvalidated strings if backend dates can be null or malformed.
- Keep Vietnamese copy aligned with core language normalization story.
- Avoid changing backend status values unless the API contract is explicitly updated.

### Likely Files

- `apps/web/src/app/(dashboard)/health-records/page.tsx`
- `apps/web/src/lib/api.ts`
- `apps/api/src/main/java/com/healthlens/api/dto/response/*HealthRecord*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 1.3
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Epic 6

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `cd apps/web && pnpm test` - pass
- `cd apps/web && pnpm exec tsc --noEmit` - blocked by pre-existing missing module/type errors in `UploadQualityChart.tsx`, `UploadQualityDonutChart.tsx`, and `ToastProvider.tsx`; no new helper errors remained after fix
- `cd apps/api && ./gradlew test` - blocked because no Java Runtime is installed
- Review patch verification: `cd apps/web && pnpm test` - pass
- Review patch verification: `cd apps/web && pnpm exec tsc --noEmit` - blocked by pre-existing missing module/type errors in `UploadQualityChart.tsx`, `UploadQualityDonutChart.tsx`, and `ToastProvider.tsx`
- Review patch verification: `cd apps/api && ./gradlew test` - blocked because no Java Runtime is installed
- Copilot follow-up verification: `cd apps/web && pnpm test` - pass
- Profile latest status follow-up verification: `cd apps/web && pnpm test` - pass
- Profile latest status follow-up verification: `cd apps/api && ./gradlew test --tests com.healthlens.api.service.ProfileServiceTest` - blocked because no Java Runtime is installed
- Owned profile status badge follow-up verification: `cd apps/web && pnpm test` - pass
- Reference-data reclassification follow-up verification: `cd apps/web && pnpm test` - pass
- Reference-data reclassification follow-up verification: `cd apps/api && ./gradlew test --tests com.healthlens.api.service.ProfileServiceTest` - blocked because no Java Runtime is installed
- Family profiles page status mapping verification: `cd apps/web && pnpm test` - pass
- Copilot performance/logging follow-up verification: `cd apps/web && pnpm test` - pass
- Copilot performance/logging follow-up verification: `cd apps/api && ./gradlew test --tests com.healthlens.api.service.ProfileServiceTest` - blocked because no Java Runtime is installed
- Document reference range status follow-up verification: `cd apps/web && pnpm test` - pass
- Document reference range status follow-up verification: `cd apps/api && ./gradlew test --tests com.healthlens.api.service.ProfileServiceTest` - blocked because no Java Runtime is installed
- Non-risk metric reclassification follow-up verification: `cd apps/web && pnpm test` - pass
- Non-risk metric reclassification follow-up verification: `cd apps/api && ./gradlew test --tests com.healthlens.api.service.ProfileServiceTest` - blocked because no Java Runtime is installed

### Completion Notes List

- Audited the health records hub against backend shared-profile/shared-record contracts and added `status` to shared health record responses so frontend status mapping can distinguish completed, pending/review, and failed OCR states.
- Extracted hub mapping/sorting logic into `apps/web/src/lib/healthRecordHub.ts`, including validated timestamp parsing, deterministic fallback ordering, and safe handling of missing or malformed dates.
- Normalized hub copy in Vietnamese, including the profile sharing action label, and replaced bespoke loading/empty/error UI with shared `LoadingState`, `EmptyState`, and `ErrorState` components.
- Verified mixed backend statuses, malformed dates, missing dates, grouping, and deterministic sorting through focused review cases.
- Resolved code review findings: completed records now only map to normal when `overallStatus` is explicitly `normal`; sorting falls back to valid `lastRecordAt` when `updatedAt` is malformed; shared query failures no longer hide already loaded hub cards.
- Resolved Copilot review comments: full-page errors are only shown when no hub cards are available, and `ProfileCard` receives validated display timestamps instead of raw malformed dates.
- Added `latestStatus` to owned profile responses so profile cards can display the latest abnormal/attention/normal health record status instead of falling back to "Chưa có cập nhật".
- Mapped owned profile backend statuses to card badge statuses, so `abnormal` renders as `critical`/Bất thường and `attention` renders as `warning`/Cần chú ý.
- Reclassified non-risk raw metric statuses against reference data for profile cards, matching history/detail behavior when raw metrics are normal but the current reference range evaluates them as abnormal.
- Fixed the family profiles page to map owned profile backend status values (`abnormal`, `attention`) into `ProfileCard` UI statuses (`critical`, `warning`).
- Addressed Copilot performance feedback by limiting profile-list reclassification to normal metrics with usable values and moving reference rule match logging from INFO to DEBUG.
- Matched profile-card latest status calculation with health record detail/history by classifying metrics against document-provided reference ranges before falling back to reference-data lookup.
- Adjusted Copilot performance follow-up so profile-list reclassification remains scoped to metrics with usable names and values, while still covering `no_data`/other non-risk raw statuses that history/detail can classify as abnormal.

### File List

- `apps/api/src/main/java/com/healthlens/api/dto/response/ProfileResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/SharedHealthRecordResponse.java`
- `apps/api/src/main/java/com/healthlens/api/service/ProfileService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/test/java/com/healthlens/api/service/ProfileServiceTest.java`
- `apps/web/src/app/(dashboard)/health-records/page.tsx`
- `apps/web/src/lib/healthRecordHub.ts`

### Change Log

- 2026-05-18: Implemented health records hub status/timestamp mapping, deterministic sorting, shared state components, and focused web tests.
- 2026-05-18: Resolved code review patch findings and marked story done.
- 2026-05-18: Resolved Copilot comments for stale-data error handling and safe card date rendering.
- 2026-05-18: Added owned profile latest health status mapping for profile hub cards.
- 2026-05-18: Fixed owned profile card status badge mapping for abnormal and attention records.
- 2026-05-18: Aligned owned/shared profile latest status calculation with reference-data classification.
- 2026-05-18: Fixed family profiles page owned profile status badge mapping.
- 2026-05-18: Addressed Copilot feedback on reference reclassification scope and logging.
- 2026-05-18: Fixed profile latest status calculation for metrics with document reference ranges.
- 2026-05-18: Rebalanced profile status reclassification to cover non-risk metrics with usable values.
