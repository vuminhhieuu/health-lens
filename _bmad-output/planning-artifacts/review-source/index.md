# Review Source Archive

## Purpose

This folder stores source review documents that were previously kept under `docs/`.

These files are not the canonical implementation backlog. They are retained as review evidence and traceability inputs for BMad planning artifacts.

Canonical planning outputs:

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md`
- `_bmad-output/planning-artifacts/core-review-docs-coverage-audit.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/`

## Archived Review Sources

| Source file | Original location | Current role | Coverage status |
| --- | --- | --- | --- |
| `REVIEW-FULL-v2.md` | `docs/REVIEW-FULL-v2.md` | Full raw review report with P0-P3 findings | Partially covered. Core AI/OCR/RAG/infra items are covered; web UX/auth/sharing/mobile cleanup still need separate backlog. |
| `REVIEW-DISPOSITION.md` | `docs/REVIEW-DISPOSITION.md` | Disposition against BMad output and source code | Used as source context. Some sections overlap with `PRODUCTION-READINESS.md`. |
| `PRODUCTION-READINESS.md` | `docs/PRODUCTION-READINESS.md` | Production readiness disposition | Used as source context. Potential duplicate of disposition content. |
| `REVIEW-PRODUCTION-MASTER.md` | `docs/REVIEW-PRODUCTION-MASTER.md` | Master production review and proposed backlog | Core items incorporated into `epic-core-improvements`; remaining product/auth/admin analytics items tracked in coverage audit. |
| `production-review/p0-gates-checklist.md` | `docs/production-review/p0-gates-checklist.md` | Go-live gate checklist | Used to validate core improvements; several gates remain partial in coverage audit. |
| `production-review/audit-logging-mini-adr.md` | `docs/production-review/audit-logging-mini-adr.md` | Audit spine ADR/source design | Covered mainly by Story `4.1` and related security/ops stories. |
| `production-review/epic-8-analytics-spec.md` | `docs/production-review/epic-8-analytics-spec.md` | Admin analytics event/chart spec | Not fully replaced by core improvements; needs admin analytics instrumentation story if production scope requires it. |

## What Should Stay In `docs/`

`docs/` should be reserved for documentation that team members read directly as project knowledge or operational guidance, such as:

- `docs/index.md`
- `docs/STAGING_DEPLOYMENT.md`
- `docs/architecture-placeholder.md` until a real architecture doc replaces it

Planning/review synthesis should live under `_bmad-output/planning-artifacts/`.

## Next Backlog Candidates

Based on the coverage audit, create additional epics/stories for:

- Web production readiness and screen-level UX fixes.
- Auth token/link hardening for verify-email, invitations, and cancel-deletion.
- Family sharing lifecycle correctness.
- Admin analytics DB-backed instrumentation.
- Mobile deferred readiness, if mobile remains in release scope.
- P2/P3 cleanup and accessibility backlog.
