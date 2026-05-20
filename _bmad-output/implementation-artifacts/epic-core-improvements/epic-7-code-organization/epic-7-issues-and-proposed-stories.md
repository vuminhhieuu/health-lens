# Epic 7 Issue Synthesis And Proposed Story Backlog

Status: synthesis  
Date: 2026-05-20  
Scope: consolidate Epic 7 findings across source organization, event-driven consistency, async boundaries, frontend decomposition, shared contracts, mobile structure, and documentation drift.

## Executive Summary

Epic 7 should not stop at the existing four refactor stories. Those stories are valid, but they only cover part of the maintainability and source-organization debt now visible in the repository.

The review found two kinds of work:

1. Safe structural refactors that improve navigation and isolate responsibility without changing behavior.
2. Architecture confirmation stories that must define rules before larger refactors proceed, especially around event-driven async work.

## Consolidated Findings

### A. Existing Epic 7 Stories Are Necessary But Incomplete

Current stories still make sense:

- `7-1` backend AI/OCR/RAG package boundary refactor.
- `7-2` health record review page decomposition.
- `7-3` shared profile sharing hook extraction.
- `7-4` email template cleanup.

They should remain in Epic 7, but they do not cover all organization debt.

### B. Event-Driven Architecture Is Only Partially Standardized

Observed state:

- OCR is meaningfully event-driven through Redis Streams, consumer groups, retry, and DLQ.
- Verification email uses stream-based delivery.
- Other emails still mix direct service calls, after-commit direct dispatch, and stream publishing.
- Stream publishing and consumer bootstrap logic are duplicated across services.

Implication:

The codebase does not yet have one explicit rule for when asynchronous work must be event-driven. This must be confirmed before broader backend refactors, or package boundaries may be rearranged twice.

### C. Backend Source Organization Needs Clearer Bounded Contexts

Observed state:

- Backend remains mostly layer-first: `controller`, `service`, `repository`, `entity`, `dto`.
- AI/OCR/RAG already started partial extraction, but core classes still live in generic `service/`.
- Empty or misleading package roots such as `event` and `model` exist.
- Large services still combine multiple responsibilities.

Implication:

Epic 7 should adopt one explicit rule:

`layer-first baseline, bounded-context extraction for volatile domains`

Initial extracted domains should be:

- `ai.*`
- `ocr.*`
- `events.*`
- `healthrecord.*`
- `admin.reference.*` as needed

### D. Frontend Feature Foldering Exists But Major Pages Still Act As Feature Modules

Observed state:

- `components/features/*` and `hooks/*` exist and are useful.
- Several route pages still own data fetching, mutations, state, type conversion, modals, helpers, and rendering in one file.
- The admin surface has the next biggest page-level organization problems after the health-record review page.

Implication:

After `7-2`, the next frontend organization stories should target admin audit log and admin reference-data pages.

### E. Shared Contracts Need Organization Before They Need Generation

Observed state:

- Route constants are already handled with explicit synchronization rules.
- High-churn request/response types still live locally in pages and services.
- Newer domains such as audit log, reference data, sharing, and health-record review are vulnerable to type drift.

Implication:

Epic 7 should add a lightweight contract organization story. This should be manual organization first, not code generation.

### F. Mobile Source Structure And Documentation Also Need Cleanup

Observed state:

- Mobile contains both root `app/` and `src/app/` source roots.
- Some docs describing the source tree and migrations are now stale relative to the actual repository.

Implication:

These are lower-risk but still valid Epic 7 maintainability items and should be tracked explicitly instead of being left as hidden debt.

## Proposed Additional Stories

### 7.5 Split Admin Audit Log Page Into Feature Modules

Reason:

`apps/web/src/app/admin/audit-log/page.tsx` is one of the largest route files and mixes filters, URL state, CSV export, citation review, detail modal, formatting helpers, and rendering.

### 7.6 Split Admin Reference Data Pages Into Feature Modules

Reason:

Reference-data admin pages are large, repeat local helpers, and mix page concerns with feature concerns.

### 7.7 Backend Health Record Service Responsibility Split

Reason:

`HealthRecordService.java` is the largest backend service and crosses upload, OCR publication, access policy, mapping, persistence transitions, delete/purge, PDF assembly, and audit behavior.

### 7.8 Shared Frontend API Contract Types For High-Churn Features

Reason:

Type drift risk is increasing in audit log, reference data, sharing, and health-record review flows.

### 7.9 Confirm And Document Event-Driven Architecture

Reason:

This is the missing architecture decision that should happen before broader backend reorganization.

### 7.10 Unify Email Event Delivery

Reason:

Current email architecture is hybrid and inconsistent with the earlier intent of event-driven delivery.

### 7.11 Introduce Application Stream/Event Boundary

Reason:

Redis Stream publishing and consumer bootstrap logic are duplicated and currently hidden inside domain services.

### 7.12 Normalize Mobile Source Root

Reason:

The mobile app has source-root ambiguity that makes navigation and future cleanup harder than needed.

### 7.13 Refresh Source Tree And Architecture Docs

Reason:

Project documentation has drifted from the current source tree and migration history.

## Recommended Execution Order

1. `7-9` Confirm and document event-driven architecture.
2. `7-1` Backend AI/OCR/RAG package boundary refactor.
3. `7-11` Introduce application stream/event boundary.
4. `7-10` Unify email event delivery.
5. `7-2` Split health record review page into feature components and hooks.
6. `7-5` Split admin audit log page into feature modules.
7. `7-6` Split admin reference-data pages into feature modules.
8. `7-3` Extract shared profile sharing hook.
9. `7-4` Email template cleanup.
10. `7-7` Backend health record service responsibility split.
11. `7-8` Shared frontend API contract types for high-churn features.
12. `7-12` Normalize mobile source root.
13. `7-13` Refresh source tree and architecture docs.

## Planning Guidance

- Do not start `7-1` as a larger architecture refactor until `7-9` confirms event and package boundary rules.
- Do not fold `7-7` into `7-1`; that would turn a package move into behavior-sensitive service surgery.
- Do not mix UI consistency work into `7-2`, `7-5`, or `7-6`; those stories are about source organization, not visual redesign.
- Do not update `sprint-status.yaml` until this expanded backlog is reviewed and approved.

## References

- `code-organization-assessment.md`
- `source-code-architecture-review.md`
