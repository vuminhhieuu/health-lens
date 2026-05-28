# Refactor Master Roadmap - 2026-05-27

## Status

Đây là source of truth hiện hành cho chương trình refactor HealthLens. Khi có mâu thuẫn, thứ tự ưu tiên là:

1. Roadmap này quyết định phase order, accepted decisions, quality gates, rollback policy, và cách xử lý historical reviews.
2. `_bmad-output/planning-artifacts/refactor-epics-and-stories.md` quyết định backlog/story text chi tiết khi không mâu thuẫn với roadmap này.
3. Các file `review/*.md` cũ chỉ là input lịch sử, không phải nguồn lệnh active.

## Active Baseline

- Baseline report: `_bmad-output/implementation-artifacts/refactor/refactor-baseline-2026-05-27.md`
- Source backlog: `_bmad-output/planning-artifacts/refactor-epics-and-stories.md`
- Baseline branch: `tmp/task-refactor`
- Baseline date: `2026-05-27`

Before starting a later phase, re-check that the baseline still matches the current branch and working tree. If branch, dirty status, or baseline command results have materially changed, capture a new baseline note before classifying failures as pre-existing.

## Accepted Decisions

| Area | Decision | Active direction |
|---|---|---|
| Scripts | Dùng root `scripts/` nhưng vẫn group theo concern. | Move script vào `scripts/docker`, `scripts/infisical`, `scripts/web`, `scripts/mobile`, `scripts/db`, và sau này `scripts/openapi` hoặc `scripts/ci`. Mỗi script phải resolve `PROJECT_ROOT` ổn định từ mọi cwd. |
| Docker utilities | Extract common shell utilities sau khi script path mới tồn tại. | `scripts/docker/_common.sh` chứa preflight, color/no-color, duration, path helpers; Docker lifecycle scripts giữ behavior cũ. |
| Flyway | Không squash active migrations trong refactor governance. | Story R1.5 chỉ inventory và group migrations, đề xuất nhiều logical baseline thay vì một file khổng lồ, và ghi rollout checks cho fresh DB plus existing migrated DB. |
| OpenAPI | OpenAPI generator là hướng contract sync chính, nhưng phải có ownership decision trước khi generate. | R2.1 quyết định output location, command, commit policy, consuming packages, và migration path với `packages/shared`, web, mobile tương lai. Detailed active decision: `review/openapi-generated-contract-ownership-decision-2026-05-28.md`. |
| Shared contracts | Active runtime contracts phải tách khỏi planned/dead contracts. | `DOCUMENTS_*`, document status/type, subscription constants không được export như active contract nếu backend chưa có implementation. |
| Error codes | Compatibility layer trước, canonical public format sau. | Map backend semantic `ApiErrorCode` sang frontend handling trước khi đổi toàn bộ backend sang catalog-style codes. |
| Backend packaging | Feature-first package migration, package moves không trộn logic rewrite. | Consolidate near-feature modules trước (`ocr`, `ai`, `activity`, `notification`), sau đó support domains, rồi core domains; service decomposition đi sau package boundaries. |
| Frontend extraction | App Router pages thành route shells theo pattern đã document. | Bắt đầu với health-record review page, sau đó admin reference-data/audit/login; giữ UX, loading/error/empty states, accessibility, và API behavior hiện tại. |
| Mobile | Deferred khỏi active refactor program. | Ghi nhận dual structure nhưng chỉ xử lý trong Deferred Epic RD1 khi mobile work resume. Không block R1-R6. |
| Docs | Docs thay đổi theo phase, historical review cleanup ở cuối. | Active docs chỉ update khi command/path/package layout thay đổi; historical review files sẽ được mark superseded input ở R6.1. |

## Phase Order

### Phase 0 - Governance, Baseline, Roadmap

**Stories:** R1.1, R1.2

**Goal:** Refactor bắt đầu từ trạng thái đã biết, có source-of-truth và quality gates tối thiểu.

**Exit criteria:**

- Baseline report tồn tại và ghi rõ branch, `git status --short`, baseline checks, skipped/failed reasons.
- Roadmap này ghi accepted decisions cho scripts, Flyway, OpenAPI, backend, frontend, mobile, docs.
- Historical review notes được xem là input, không phải active instructions.

**Entry gate:** none beyond R1.1 story readiness.

**Per-story gates:** documentation-only changes run `git diff --check`; no code/package behavior changed.

**Exit gate before Phase 1:** baseline report exists, roadmap is current, and `git diff --check` is clean for R1 documentation changes.

**Rollback notes:** Revert roadmap/baseline artifact changes only. Không chạy destructive git commands; không rollback unrelated user work.

### Phase 1 - Root Automation And DB Baseline Analysis

**Stories:** R1.3, R1.4, R1.5, R1.6

**Goal:** Script automation discoverable từ root, Docker utility duplication giảm, Flyway squash risk được phân tích trước khi đụng active migrations.

**Exit criteria:**

- Scripts nằm dưới root `scripts/` theo nhóm concern và giữ help/non-destructive behavior.
- Docker scripts source common helper mà không đổi lifecycle behavior.
- Flyway migration inventory liệt kê migrations theo version order, group theo domain, và không sửa active migration files.
- README/dev/ops/deploy/Infisical docs và root package scripts trỏ đúng path mới.

**Entry gate:** Phase 0 exit gate complete and baseline freshness re-checked.

**Per-story gates:**

- Shell scripts: run `--help` và `--ci`/non-destructive checks cho scripts liên quan.
- Docs/package changes: `git diff --check`.
- Shared/web/mobile/API gates chỉ chạy khi story chạm vào package/app tương ứng.

**Exit gate before Phase 2:** root scripts, docs, package entrypoints, and Flyway analysis artifacts are consistent and validated by the per-story gates.

**Rollback notes:** Roll back the smallest affected story scope first. Do not revert completed R1.3-R1.6 work as one phase unless the entire phase has not been accepted and the rollback is explicitly approved. Flyway analysis is artifact-only; no DB rollback expected because active migrations must remain untouched.

### Phase 2 - API Contract Foundation With OpenAPI

**Stories:** R2.1-R2.6

**Goal:** Generated API contract và shared constants có ownership rõ, không còn active dead/planned contracts.

**Exit criteria:**

- OpenAPI ownership decision được document trước generated code.
- Generation command/config deterministic hoặc diff explainable.
- Dead/planned contracts không còn trong active barrel exports.
- Error-code compatibility layer cover backend semantic codes.
- CI phát hiện stale generated output hoặc active contract drift với exclusions explicit.

**Entry gate:** Phase 1 exit gate complete and baseline freshness re-checked.

**Per-story gates:**

- `pnpm --filter @healthlens/shared build`.
- Nếu web/mobile consume shared/generated output: run relevant web/mobile checks.
- API tests only required when backend OpenAPI annotations/routes/contracts are changed.

**Exit gate before Phase 3:** generated ownership, dead/planned contract handling, compatibility layer, and CI drift checks are documented and reproducible.

**Rollback notes:** Generated output must be reproducible. Revert generated files and consuming imports together; keep compatibility layer isolated from backend public error-code migration.

### Phase 3 - Backend Feature-First Package Migration

**Stories:** R3.1-R3.7

**Goal:** Backend code moved by bounded context with mirrored tests and architecture rules.

**Exit criteria:**

- Boundary rules define top-level feature packages, allowed dependencies, and `common/` ownership.
- Near-feature modules are consolidated before core domains.
- Package moves do not rewrite service responsibilities in the same story.
- Resource ownership for prompts, seed data, fonts, migrations, templates is documented and tested where loading paths change.

**Entry gate:** Phase 2 exit gate complete and baseline freshness re-checked.

**Per-story gates:**

- `cd apps/api && ./gradlew test` with Java 21.
- Architecture/package boundary tests updated with each move.
- No behavior changes unless the story explicitly says so.

**Exit gate before Phase 4:** package boundary rules are enforced, migrated test packages mirror source where applicable, and no package move is mixed with service-responsibility rewrites.

**Rollback notes:** Roll back one bounded package move at a time. Do not mix rollback with service decomposition or unrelated refactors.

### Phase 4 - Backend Service Decomposition

**Stories:** R4.1-R4.5

**Goal:** Split god services after package boundaries are stable.

**Exit criteria:**

- Responsibilities split into cohesive collaborators for health records, reference data admin, LLM, OCR, and admin audit where needed.
- Controllers preserve public behavior.
- Tests target new collaborators and existing workflows still pass.

**Entry gate:** Phase 3 exit gate complete and baseline freshness re-checked.

**Per-story gates:**

- `cd apps/api && ./gradlew test`.
- Focused unit tests for new service collaborators.
- Integration/behavior tests retained for critical workflows.

**Exit gate before Phase 5:** decomposed services preserve public controller behavior and critical workflow tests still pass.

**Rollback notes:** Revert each service split independently. Keep public API and database schema unchanged unless a separate story authorizes changes.

### Phase 5 - Web Feature Extraction And Route Shell Cleanup

**Stories:** R5.1-R5.6

**Goal:** Large App Router pages become route shells with feature components, hooks, and domain utilities.

**Exit criteria:**

- Web extraction pattern documented before large page moves.
- Health-record review page proves the pattern first.
- Admin pages follow after the pattern is stable.
- Loose domain libs move to explicit feature/domain folders.

**Entry gate:** Phase 4 exit gate complete and baseline freshness re-checked.

**Per-story gates:**

- `pnpm --filter web test`.
- Focused tests for extracted hooks/domain utilities and critical error/empty/loading states.
- Shared build when imports from `packages/shared` change.

**Exit gate before Phase 6:** route shells preserve route behavior and extracted feature modules have focused coverage for moved behavior.

**Rollback notes:** Revert one page/domain extraction at a time. Preserve route paths and user-visible behavior.

### Phase 6 - Documentation Source Of Truth And Historical Cleanup

**Stories:** R6.1-R6.3

**Goal:** Active docs match final commands/package layout; historical reviews are clearly marked as superseded inputs.

**Exit criteria:**

- Each historical review file points to this roadmap as current source of truth.
- Docs are organized by audience or documented current structure.
- Development/testing/deployment/operations docs use real current commands and paths.

**Entry gate:** Phase 5 exit gate complete and baseline freshness re-checked.

**Per-story gates:**

- `git diff --check`.
- Link/path spot checks for changed docs.
- Executable command references must be verified by checking the command exists in `package.json`, script files, or documented tooling config; when safe and non-destructive, run the command with `--help`, dry-run, or equivalent.
- App/package test gates only if docs changes include executable command changes that touch package scripts.

**Exit gate:** active docs and indexes point to current commands/paths, historical review files point back to this roadmap, and command references have been existence-checked or explicitly marked not runnable.

**Rollback notes:** Revert docs/index/path edits together if links break. Historical content should remain available for traceability.

## Minimum Refactor Quality Gates

- Shared/package contract changes must run `pnpm --filter @healthlens/shared build`. If shared exports are consumed by web or mobile, also run the relevant web/mobile checks.
- Web changes must run `pnpm --filter web test` and include focused coverage for extracted components, hooks, or domain utilities when behavior is moved.
- Mobile changes must run `pnpm --filter mobile lint` with a supported Node runtime (`>=20.19.4`). If the local runtime is older, record the warning explicitly and do not treat it as a clean environment.
- API changes must run `cd apps/api && ./gradlew test` with Java 21 available.
- Documentation-only roadmap changes must run `git diff --check` and content checks for the required sections/decisions.
- Any skipped or failed gate must be labeled as pre-existing only when the story did not change the related files. If the story changed related files, it must either fix the failure, get an explicit reviewer-approved exception, or remain incomplete.

## Historical Inputs

Các tài liệu dưới đây vẫn hữu ích để trace reasoning và constraints, nhưng chỉ là historical inputs. Supersession notes below state which guidance is active when the historical documents disagree:

- `review/api-package-structure-analysis.md` - historical API package-by-feature proposal. Active guidance: package moves happen in R3 after boundary rules and before service decomposition.
- `review/constants-scripts-deep-dive.md` - historical constants drift, script quality, dead contract findings. Active guidance: OpenAPI generator path starts with R2.1 ownership decision; compatibility layer precedes public error-code format migration.
- `review/current-project-refactor-audit-2026-05-26.md` - current-state correction for stale assumptions. Active guidance: use this audit's caution on dirty work, staged refactor, and no broad-bang refactor.
- `review/full-project-structure-analysis.md` - historical whole-repo structure audit. Active guidance: use its inventory as background only; current phase/story order comes from this roadmap.
- `review/sprint-refactoring-scripts-constants.md` - early scripts/Flyway/OpenAPI proposal. Active guidance: root scripts are grouped under `scripts/`; Flyway work is analysis/multi-baseline first, not one-file squash; OpenAPI generator is gated by ownership decision.
- `_bmad-output/planning-artifacts/refactor-epics-and-stories.md` - active refactor backlog and story text source. Active only where it does not conflict with this roadmap's precedence, phase, gate, and rollback rules.
