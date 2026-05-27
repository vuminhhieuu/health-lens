# HealthLens — Current Project Refactor Audit

**Date:** 2026-05-26  
**Scope:** Current repository state, with prior files in `review/` treated as historical input.  
**Goal:** Decide how to refactor the whole project safely, update stale assumptions, and define an execution order.

---

## Executive Decision

Do **not** start with a single “refactor toàn bộ” branch. The current codebase has enough moving parts that a broad-bang refactor will create high merge risk, high test churn, and unclear rollback boundaries.

The correct strategy is a staged refactor program:

1. **Stabilize and baseline** current behavior.
2. **Remove stale/dead surface area** that creates false contracts.
3. **Add automated guardrails** for route/error/enum drift.
4. **Refactor one bounded context at a time**, beginning with the modules already closest to feature-based organization.
5. **Split god pages/services incrementally**, with tests moved in the same PR as production code.

---

## Important Current-State Notes

### Working Tree Is Not Clean

There are existing uncommitted changes in API services/tests, BMad artifacts, docs, and review files. Any refactor should start by separating these changes into a checkpoint commit or dedicated branch before structural moves.

Observed modified/added areas include:

- `apps/api/src/main/java/com/healthlens/api/service/*`
- `apps/api/src/test/java/com/healthlens/api/service/*`
- `docs/reference-data/*`
- `_bmad-output/*`
- existing `review/*.md`

### Prior Reviews Are Stale But Useful

The older review files are directionally useful, but they are no longer exact:

- API migrations are now **50**, not 43.
- Web review page is now **2782 lines**, not 2446.
- API source has grown to about **31K Java lines**.
- API tests have grown to about **20K Java test lines**.
- Docs now include `docs/reference-data/` and total markdown count is **20**.
- New API areas exist since the prior reviews: notification inbox/preferences, TOTP, user activity dimensions, reference dataset docs.

---

## Current Architecture Snapshot

```text
health-lens/
├── apps/
│   ├── api/       Spring Boot 4, Java 21, Flyway, Redis, Qdrant, S3, Spring AI
│   ├── web/       Next.js 16 App Router, React 19, TanStack Query, Zustand
│   └── mobile/    Expo Router, scaffold/early-stage structure
├── packages/
│   └── shared/    TypeScript constants, schemas, types, env config
├── services/
│   └── ocr-service/ FastAPI OCR microservice
├── docker/        Compose files and lifecycle scripts
├── infisical/     Secret management docs/scripts
├── docs/          Product/architecture/ops docs plus reference-data docs
└── review/        Architecture review notes and refactor plans
```

---

## Findings By Priority

## P0 — Stop-The-Line / Prep Work

### P0-1: Preserve Current Work Before Refactor

The repo has a dirty working tree. Large refactors involving package moves and import rewrites will conflict with the current active changes unless they are checkpointed first.

**Action:** Create a branch and checkpoint current work before any structural refactor.

```bash
git status --short
git switch -c refactor/project-structure-2026-05-26
```

Commit only when the current work is understood; do not blindly commit generated/build artifacts.

### P0-2: Confirm Build/Test Baseline

Before structural changes, capture current pass/fail status:

```bash
pnpm --filter @healthlens/shared build
pnpm --filter web test
pnpm --filter mobile lint
cd apps/api && ./gradlew test
```

If baseline is red, document existing failures and do not mix baseline fixes with refactor moves.

### P0-3: Treat Root Secret Concern As Mostly Resolved

Earlier review raised `.env.production` and `.env.staging*` tracking risk. Current `git ls-files` only shows `.env.example`. This is good.

**Remaining action:** Keep `.env.*` ignored and ensure production deploy docs reference Infisical or deployment secret storage rather than committed env files.

---

## P1 — Contract Drift And Dead Contracts

### P1-1: Backend/Frontend Error Codes Still Diverge

Backend emits simple codes such as:

```text
ACCOUNT_LOCKED, RATE_LIMITED, INVALID_CREDENTIALS, VALIDATION_ERROR, ...
```

Frontend shared package defines RFC-like grouped codes such as:

```text
AUTH_001, AUTH_002, AUTH_008, VAL_001, RES_001, ...
```

Current frontend code still checks backend-style codes directly in several places, so `packages/shared/constants/error-codes.ts` is not the real runtime source of truth.

**Decision:** Do not immediately force backend to adopt `AUTH_001` style unless you are willing to migrate tests, API clients, and UX copy at once.

**Recommended path:**

1. Add a backend-to-frontend error-code compatibility map in shared code.
2. Add tests proving all backend `ApiErrorCode` values have frontend message handling.
3. Later decide whether the public API contract remains semantic (`ACCOUNT_LOCKED`) or becomes catalog-style (`AUTH_008`).

### P1-2: Dead `DOCUMENTS` Contract Still Exists

`ApiRoutes.java` still has `DOCUMENTS_*` constants and `packages/shared/constants/api.ts` still has `ApiPaths.DOCUMENTS`, but current code has no `DocumentController`, `DocumentService`, `Document` entity, or document migration.

**Action:** Either remove it now or move it into a clearly named planned/future section that is not exported as an active API contract.

**Recommended:** Remove from active shared constants unless the feature is scheduled in the next sprint.

### P1-3: Status And Enum Drift Remains

The shared package has frontend constants for documents/subscriptions and uppercase `Gender`, while active profile schemas use lowercase `GENDER_OPTIONS` (`male`, `female`, `other`). Backend stores gender as strings and normalizes in multiple services.

**Action:**

- Remove or quarantine inactive `DocumentType`, `DocumentStatus`, `SubscriptionTier`.
- Make lowercase profile gender the current explicit contract unless backend enum migration is done in the same phase.
- Centralize gender normalization instead of repeating it in `UserService`, `ReferenceDataService`, `ReferenceDataAdminService`, and `LlmService`.

### P1-4: Route Sync Is Manual

Backend routes and frontend `ApiPaths` are manually maintained. This is acceptable short-term but needs a guardrail before broad refactor.

**Recommended first guardrail:** a lightweight CI script that checks active shared paths against Spring `ApiRoutes` constants and excludes known frontend-only routes.

**Do not start with full OpenAPI client generation** unless the team is ready to replace existing API client patterns.

---

## P2 — Backend Package Refactor

### Current Problem

API is still mostly package-by-layer:

```text
controller/  21 files
service/     52 files
repository/  38 files
entity/      37 files
dto/         94 files
```

Some feature-style packages exist, but core domains are scattered. This makes every large feature change touch many top-level packages.

### Largest Backend Files

| File | Lines | Refactor Direction |
|---|---:|---|
| `ReferenceDataAdminService.java` | 1851 | split import/preview/apply/audit concerns |
| `HealthRecordService.java` | 1711 | split query/command/analysis/context orchestration |
| `LlmService.java` | 1446 | split prompt assembly, provider call, parsing, fallback policy |
| `OcrService.java` | 1081 | split upload/job/provider/result persistence |
| `AdminAuditLogService.java` | 879 | split query/filter/export/statistics if needed |

### Recommended Package Target

Use feature-first packages with internal layers:

```text
com.healthlens.api/
├── auth/
├── profile/
├── healthrecord/
├── referencedata/
├── ocr/
├── ai/
├── notification/
├── activity/
├── consent/
├── reminder/
├── deletion/
├── admin/
└── common/
```

### Backend Refactor Order

1. **Move almost-feature modules first:** `ocr`, `ai`, `notification`, `activity`.
2. **Then move cohesive support domains:** `consent`, `reminder`, `deletion`.
3. **Then core domains:** `auth`, `profile`, `healthrecord`, `referencedata`.
4. **Only then split god services** if package moves are stable, or split service first inside old packages if imports are too volatile.

### Do Not Do This

- Do not move all entities/repositories/controllers in one commit.
- Do not rename packages and rewrite service responsibilities in the same commit.
- Do not squash 50 Flyway migrations during architecture refactor.

---

## P3 — Frontend Refactor

### Current Problem

Large Next.js page files contain UI, data fetching, orchestration, mapping, and mutation logic together.

Largest current files:

| File | Lines | Refactor Direction |
|---|---:|---|
| `health-records/review/[recordId]/page.tsx` | 2782 | route shell + feature components + hook |
| `admin/reference-data/approvals/page.tsx` | 1098 | admin feature components/hooks |
| `admin/reference-data/page.tsx` | 1025 | admin feature components/hooks |
| `(dashboard)/home/page.tsx` | 744 | dashboard widgets and data hook |
| `admin/login/page.tsx` | 691 | auth form component + session hook |

### Recommended Web Target

```text
apps/web/src/
├── app/                         route shells only
├── components/ui/               reusable primitives
├── components/layout/           layout/navigation
├── features/
│   ├── health-records/
│   ├── reference-data/
│   ├── profiles/
│   ├── auth/
│   └── notifications/
├── lib/
│   ├── api/
│   ├── auth/
│   ├── i18n/
│   └── utils/
└── stores/
```

This is cleaner than putting all feature components under `components/features` while hooks and domain utilities remain elsewhere.

### Frontend Refactor Order

1. Extract pure presentational components from `review/[recordId]/page.tsx` without changing behavior.
2. Extract `useHealthRecordReview` hook for queries/mutations/state.
3. Move profile mapping utilities from loose `lib/profileMappings.ts` into a profile feature or shared domain utility.
4. Refactor admin reference-data pages after the health-records page pattern is proven.

---

## P4 — Shared Package Refactor

### Current Problem

`packages/shared` is useful but mixes active contracts, planned concepts, frontend-only policy, and backend-adjacent constants.

Current largest files:

| File | Lines | Issue |
|---|---:|---|
| `constants/api.ts` | 253 | manually synced active + dead paths |
| `constants/error-codes.ts` | 230 | not aligned with backend runtime codes |
| `constants/status.ts` | 196 | includes inactive document/subscription status |
| `config/env.ts` | 158 | probably acceptable |

### Recommended Shared Structure

```text
packages/shared/
├── constants/
│   ├── api.ts
│   ├── errors.ts
│   ├── profile.ts
│   ├── upload.ts
│   └── index.ts
├── schemas/
│   ├── auth.ts
│   ├── profile.ts
│   ├── health-record.ts
│   └── user.ts
└── generated/          optional later, only after guardrails are stable
```

### Do First

- Remove inactive exports or move to `_planned.ts` with no default barrel export.
- Add `schemas/health-record.ts` only when active web/API payload validation needs it.
- Add a `pnpm --filter @healthlens/shared build` CI gate before route/error sync checks.

---

## P5 — Mobile Refactor

Mobile still has dual source roots:

```text
apps/mobile/app/
apps/mobile/components/
apps/mobile/hooks/
apps/mobile/lib/
apps/mobile/stores/
apps/mobile/src/app/
apps/mobile/src/components/
apps/mobile/src/hooks/
```

**Decision:** Since mobile is scaffold/early-stage, consolidate now before features are built.

Recommended convention:

```text
apps/mobile/
├── app/               Expo Router root
└── src/
    ├── components/
    ├── hooks/
    ├── lib/
    ├── stores/
    └── constants/
```

Move root-level `components`, `hooks`, `lib`, and `stores` into `src/`, but keep Expo Router routes in root `app/` unless the app is configured for `src/app` only.

---

## P6 — Scripts, Docker, Infisical

### Scripts

Prior reviews disagree on script centralization. Current project layout supports keeping scripts near their concern:

```text
docker/scripts/       Docker lifecycle
infisical/scripts/    secret management
apps/web/scripts/     web-specific smoke check
apps/mobile/scripts/  mobile reset scaffold
```

**Decision:** Do not centralize all scripts into root `/scripts`.

**Action:** Extract duplicated Docker shell utilities into `docker/scripts/_common.sh` only.

### Migrations

There are now 50 Flyway migrations. Do not squash them as part of this refactor. Squashing migrations is a database lifecycle decision, not a code-structure cleanup.

If startup time or migration readability becomes a problem, create a separate database-baseline initiative with production/staging rollout design.

---

## P7 — Docs And Review Files

Docs are no longer just flat; `docs/reference-data/` exists. The docs structure is acceptable for now.

Recommended next docs action:

- Keep existing docs stable during code refactor.
- Update docs only when a refactor phase changes package layout or developer commands.
- Replace the old review set with this current audit plus phase-specific implementation notes.

---

## Recommended Refactor Roadmap

### Phase 0 — Baseline And Guardrails

**Goal:** Make refactor safe.

- Checkpoint current dirty work.
- Run and record current build/test baseline.
- Add route/error/status drift checks or at least inventory scripts.
- Clean untracked build/ghost directories if they are not needed.

**Exit criteria:** baseline documented; CI can detect shared contract drift.

### Phase 1 — Low-Risk Cleanup

**Goal:** Remove known false contracts and clutter.

- Remove/quarantine `DOCUMENTS` active API constants.
- Remove/quarantine inactive document/subscription status constants.
- Fix shared gender contract inconsistency.
- Extract Docker `_common.sh`.
- Consolidate mobile folder structure.

**Exit criteria:** no active exported constants for unimplemented domains; mobile has one convention.

### Phase 2 — Backend Feature Packaging

**Goal:** Move code without changing behavior.

- Move `ocr`-related controller/config/entity/repository/dto/events into `ocr/`.
- Move `activity` into one package.
- Move `notification` into one package.
- Introduce `common/` for true cross-cutting packages.

**Exit criteria:** tests pass after package moves; architecture tests updated to enforce new boundaries.

### Phase 3 — Frontend Feature Extraction

**Goal:** Reduce page-file complexity without changing UX.

- Refactor health-record review page first.
- Extract hooks/components in small PRs.
- Then apply the same pattern to admin reference-data pages.

**Exit criteria:** page files become route composition shells; tests/smoke checks still pass.

### Phase 4 — Service Decomposition

**Goal:** Reduce god services after package boundaries are stable.

- Split `HealthRecordService` by query/command/analysis/context responsibilities.
- Split `ReferenceDataAdminService` by preview/import/apply/audit responsibilities.
- Split `LlmService` by prompt/provider/parser/fallback responsibilities.
- Split `OcrService` only after OCR package is consolidated.

**Exit criteria:** no core service above ~500–700 lines unless justified by cohesive logic.

### Phase 5 — Contract Generation Or OpenAPI

**Goal:** Replace manual sync only after architecture is stable.

Options:

- Lightweight custom route/error inventory script: best first step.
- OpenAPI-generated client: good later, but requires frontend API client migration.
- Java-to-TypeScript constants generator: useful for enums/constants, less useful for actual controller coverage.

**Exit criteria:** frontend no longer depends on manually duplicated backend contracts without CI verification.

---

## Practical Branch Plan

Use several focused branches/PRs:

1. `refactor/baseline-guardrails`
2. `refactor/shared-contract-cleanup`
3. `refactor/mobile-structure`
4. `refactor/api-ocr-activity-notification-packages`
5. `refactor/web-health-record-review-page`
6. `refactor/api-health-record-service-split`
7. `refactor/api-reference-data-service-split`

Do not combine backend package moves, frontend page extraction, and constants contract changes in one branch.

---

## Answer To “Nên Làm Gì?”

If the goal is to refactor the whole project responsibly, start with **Phase 0 and Phase 1**, not the package-by-feature migration immediately.

Recommended immediate work:

1. Checkpoint current dirty work.
2. Run baseline tests/builds and record failures.
3. Clean dead contracts in shared/backend constants.
4. Add drift guardrails.
5. Then execute backend/frontend refactors in separate bounded phases.

This gives the project a controlled path from “review notes” to actual refactor execution without turning the next branch into an unreviewable rewrite.
