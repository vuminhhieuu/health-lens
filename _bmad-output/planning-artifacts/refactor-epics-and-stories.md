---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
inputDocuments:
  - _bmad-output/planning-artifacts/prd.md
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/ux-design-specification.md
  - review/api-package-structure-analysis.md
  - review/constants-scripts-deep-dive.md
  - review/current-project-refactor-audit-2026-05-26.md
  - review/full-project-structure-analysis.md
  - review/sprint-refactoring-scripts-constants.md
  - current source tree snapshot
workflowType: refactor-epics
project_name: health-lens
user_name: ie303
date: '2026-05-27'
status: validation-complete-awaiting-workflow-completion
---

# health-lens - Refactor Epic Breakdown

## Overview

Tài liệu này phân rã chương trình refactor HealthLens thành epics và stories triển khai được. Mục tiêu không phải đổi tính năng sản phẩm, mà là làm cho repo dễ phát triển tiếp: contract backend/frontend rõ ràng, script automation thống nhất, backend theo bounded context, frontend giảm page-level complexity, và migration/docs có quy trình an toàn.

## Requirements Inventory

### Functional Requirements

RFR1: Chương trình refactor phải bắt đầu bằng governance và baseline rõ ràng: ghi nhận working tree, baseline build/test, existing failures, minimum quality gates, và rule không proceed khi baseline chưa rõ.

RFR2: Hệ thống script tự động hóa phải được gom về root repo dưới `scripts/`, nhưng vẫn phân nhóm rõ theo concern như Docker, Infisical, web, mobile, db, ci/openapi.

RFR3: Các Docker lifecycle scripts phải giữ nguyên behavior hiện tại, đồng thời dùng shared utility để giảm duplicate preflight, color, duration, và path-resolution logic.

RFR4: Các tài liệu vận hành và package scripts phải trỏ đến script path mới, không còn hướng dẫn path cũ gây nhầm lẫn.

RFR5: Flyway migration squash phải bắt đầu bằng inventory và grouping analysis, không squash tất cả về một file, không thay đổi migration active khi chưa có rollout plan.

RFR6: Một master refactor roadmap/source-of-truth phải được tạo sớm để thay thế các review notes mâu thuẫn trong quá trình triển khai.

RFR7: OpenAPI generator phải trở thành nền tảng contract sync giữa API và web/mobile, nhưng story đầu tiên phải quyết định ownership/design cho generated output trước khi generate code.

RFR8: Dead/planned contracts như `DOCUMENTS_*`, `DocumentType`, `DocumentStatus`, `SubscriptionTier` không được export như active runtime contract.

RFR9: Backend/frontend error-code drift phải được xử lý bằng compatibility layer trước, rồi mới quyết định canonical public error-code format.

RFR10: Shared constants phải tách active contract, planned contract, frontend-only policy, upload/profile/metric constants thành các module rõ ràng.

RFR11: CI phải phát hiện generated OpenAPI client stale và drift giữa active backend API contract với frontend usage.

RFR12: Backend package structure phải chuyển dần từ hybrid layer/feature sang feature-first packages với internal layers và `common/` cho cross-cutting concerns.

RFR13: Backend package moves không được trộn với rewrite logic service lớn trong cùng story/PR.

RFR14: Các module gần feature-ready như `ocr`, `ai`, `activity`, `notification` phải được consolidate trước core domains.

RFR15: Core backend domains như `auth`, `profile`, `healthrecord`, `referencedata`, `admin` phải có package boundary rõ và test structure mirror source.

RFR16: God services phải được split sau khi package boundaries ổn định, ưu tiên `HealthRecordService`, `ReferenceDataAdminService`, `LlmService`, `OcrService`, `AdminAuditLogService`.

RFR17: Frontend App Router pages phải chuyển dần thành route shells; UI/state/data orchestration đi vào feature components/hooks/domain utilities.

RFR18: Web health-record review page phải là mẫu đầu tiên cho frontend feature extraction vì hiện là page lớn nhất.

RFR19: Admin reference-data pages và admin audit/login pages phải được refactor sau khi health-record review pattern đã proven.

RFR20: Loose web lib files như `healthRecordHub.ts`, `profileMappings.ts`, `notify.ts` phải được move vào feature/domain folders phù hợp.

RFR21: Mobile dual source structure được ghi nhận nhưng defer khỏi chương trình refactor trước mắt, trừ khi có story riêng sau.

RFR22: Documentation cleanup cuối chương trình phải phân biệt tài liệu historical review với tài liệu kế hoạch đang áp dụng.

RFR23: Existing product backlog `_bmad-output/planning-artifacts/epics.md` không được ghi đè bởi refactor backlog.

### NonFunctional Requirements

RNFR1: Mọi phase refactor phải có baseline build/test hoặc ghi nhận rõ existing failures trước khi đổi cấu trúc.

RNFR2: Mỗi story phải có phạm vi reviewable, rollbackable, không kết hợp nhiều bounded contexts độc lập.

RNFR3: Không dùng destructive git commands để rollback thay đổi không thuộc story hiện tại.

RNFR4: Refactor phải giữ behavior hiện tại trừ khi story ghi rõ behavior change.

RNFR5: Mọi generated artifact phải có command tái tạo và CI check chống stale output.

RNFR6: DB migration squash phải được test trên fresh DB và existing migrated DB trước khi thay active migrations.

RNFR7: OpenAPI migration không được buộc rewrite toàn bộ frontend API client trong một PR.

RNFR8: Backend package moves phải giữ test pass hoặc update/import test trong cùng PR.

RNFR9: Frontend page extraction phải giữ UX và route behavior hiện tại; testing/smoke coverage phải đi kèm.

RNFR10: Documentation changes phải cập nhật command/path thật, không chỉ copy review notes.

### Additional Requirements From Architecture And Source

- Current source snapshot shows API has about 31K Java source lines and 20K Java test lines.
- Largest backend source files: `ReferenceDataAdminService.java` 1851 lines, `HealthRecordService.java` 1711, `LlmService.java` 1446, `OcrService.java` 1081, `AdminAuditLogService.java` 879.
- Backend packages still include layer-based roots: `controller`, `service`, `entity`, `repository`, `dto`, `exception`, while `ocr`, `ai`, `audit`, `notification` are closer to feature organization.
- Shared `ApiPaths.DOCUMENTS` and backend `ApiRoutes.DOCUMENTS_*` exist while source has no implemented document domain.
- Shared `error-codes.ts` uses catalog style codes, backend `ApiErrorCode` emits semantic codes.
- Shared `status.ts` includes inactive document/subscription concepts and uppercase gender values while active profile usage has lowercase options elsewhere.
- Web largest pages: health-record review 2782 lines, admin reference-data approvals 1098, admin reference-data 1025, dashboard home 744, admin login 691.
- Web `components/features` exists but major feature logic remains in route page files.
- Web `lib` still contains loose domain files: `healthRecordHub.ts`, `profileMappings.ts`, `notify.ts`.
- Mobile still has both root-level folders and `src/` folders; this is deferred by user decision.
- Flyway currently has 50 migrations from `V001` to `V050`.
- API resources place seed data and prompts under `resources/ai`, which should be separated in a later backend/resource cleanup story.

### UX Design Requirements

UXR1: Frontend extraction must preserve existing UX semantics, accessibility behavior, and route-level loading/error/empty states.

UXR2: Health-record review extraction must keep progressive disclosure, health status readability, recommendations/disclaimer display, and review actions intact.

UXR3: Admin reference-data refactor must preserve preview/approval/audit clarity and avoid hiding risk-bearing workflows behind generic components.

UXR4: User-facing route shells must remain small and readable without adding marketing-style or instructional UI not currently requested.

UXR5: Componentization should follow existing design system patterns and avoid introducing a new UI library during structural refactor.

## Proposed Epic List

### Epic R1: Refactor Governance, Baseline And Root Automation

Developers can safely start the refactor program with known repository state, documented quality gates, a current master roadmap, root-level automation structure, and DB migration squash prepared through analysis only.

**Requirements covered:** RFR1, RFR2, RFR3, RFR4, RFR5, RFR6, RNFR1, RNFR2, RNFR6, RNFR10

### Epic R2: API Contract Foundation With OpenAPI

Frontend and future mobile development can rely on generated API contracts and controlled shared constants instead of manually duplicated active/planned runtime contracts. The first story must decide generated output ownership and migration design before generating or adopting code.

**Requirements covered:** RFR7, RFR8, RFR9, RFR10, RFR11, RNFR5, RNFR7

### Epic R3: Backend Feature-First Package Migration

Backend developers can change a domain by working inside a bounded package with internal layers and mirrored tests, starting with lower-risk near-feature modules before core domains.

**Requirements covered:** RFR12, RFR13, RFR14, RFR15, RNFR2, RNFR4, RNFR8

### Epic R4: Backend Service Decomposition

Core backend behavior becomes easier to test and change because god services are split by cohesive responsibilities after package boundaries are stable.

**Requirements covered:** RFR16, RNFR2, RNFR4, RNFR8

### Epic R5: Web Feature Extraction And Route Shell Cleanup

Web developers can maintain high-risk screens through feature components, hooks, and domain utilities instead of thousand-line route pages, while preserving current UX.

**Requirements covered:** RFR17, RFR18, RFR19, RFR20, UXR1, UXR2, UXR3, UXR4, UXR5, RNFR9

### Epic R6: Documentation Source Of Truth And Historical Review Cleanup

The team finishes with clear phase docs, updated operational/developer documentation, and historical review files marked as inputs instead of conflicting instructions.

**Requirements covered:** RFR22, RFR23, RNFR10

### Deferred Epic RD1: Mobile Structure Consolidation

Mobile code eventually uses one folder convention, but this is intentionally deferred until mobile work resumes.

**Requirements covered:** RFR21

## Requirements Coverage Map

RFR1: Epic R1 - governance, baseline, and quality gates.
RFR2: Epic R1 - root script structure.
RFR3: Epic R1 - Docker utility extraction.
RFR4: Epic R1 - docs/package command path updates.
RFR5: Epic R1 - Flyway migration inventory and grouping.
RFR6: Epic R1 - early master refactor roadmap/source of truth.
RFR7: Epic R2 - OpenAPI ownership/design before generation.
RFR8: Epic R2 - dead/planned contract cleanup.
RFR9: Epic R2 - error-code compatibility path.
RFR10: Epic R2 - shared constants modularization.
RFR11: Epic R2 - CI stale/generated contract checks.
RFR12: Epic R3 - backend feature-first package target.
RFR13: Epic R3 - package moves separated from logic rewrites.
RFR14: Epic R3 - near-feature modules first.
RFR15: Epic R3 - core domain package boundaries and mirrored tests.
RFR16: Epic R4 - god service decomposition.
RFR17: Epic R5 - route shells and feature extraction.
RFR18: Epic R5 - health-record review first.
RFR19: Epic R5 - admin pages after pattern proven.
RFR20: Epic R5 - loose web lib cleanup.
RFR21: Deferred Epic RD1 - mobile later.
RFR22: Epic R6 - documentation cleanup and historical review treatment.
RFR23: Epic R6 - avoid overwriting product backlog.

RNFR1: Epic R1.
RNFR2: Epics R1, R3, R4.
RNFR3: All epics.
RNFR4: Epics R3, R4.
RNFR5: Epic R2.
RNFR6: Epic R1.
RNFR7: Epic R2.
RNFR8: Epics R3, R4.
RNFR9: Epic R5.
RNFR10: Epics R1, R6.

UXR1: Epic R5.
UXR2: Epic R5.
UXR3: Epic R5.
UXR4: Epic R5.
UXR5: Epic R5.

## Epic R1: Refactor Governance, Baseline And Root Automation

Developers can safely start the refactor program with known repository state, documented quality gates, a current master roadmap, root-level automation structure, and DB migration squash prepared through analysis only.

### Story R1.1: Capture Refactor Baseline And Quality Gates

As a developer,
I want the current repository state and baseline checks recorded before refactor work starts,
So that later failures can be distinguished from existing failures.

**Acceptance Criteria:**

**Given** the current dirty working tree
**When** the baseline story is executed
**Then** the output records `git status --short`, current branch, and any pre-existing modified/untracked areas relevant to the refactor.
**And** baseline commands are attempted or explicitly documented as skipped with reason: shared build, web test, mobile lint, and API test.

**Given** a baseline command fails
**When** the failure is recorded
**Then** the failure is labeled as pre-existing unless the story changed the related files.
**And** minimum quality gates for future stories are documented in the master refactor roadmap.

#### Minimum Refactor Quality Gates

The R1.1 baseline is recorded in `_bmad-output/implementation-artifacts/refactor/refactor-baseline-2026-05-27.md`. The master roadmap placeholder is `review/refactor-master-roadmap-2026-05-27.md`; Story R1.2 expands that file into the full source of truth.

- Shared/package contract changes must run `pnpm --filter @healthlens/shared build`. If shared exports are consumed by web or mobile, also run the relevant web/mobile checks.
- Web changes must run `pnpm --filter web test` and include focused coverage for extracted components, hooks, or domain utilities when behavior is moved.
- Mobile changes must run `pnpm --filter mobile lint` with a supported Node runtime (`>=20.19.4`). If the local runtime is older, record the warning explicitly and do not treat it as a clean environment.
- API changes must run `cd apps/api && ./gradlew test` with Java 21 available.
- Any skipped or failed gate must be labeled as pre-existing only when the story did not change the related files. If the story changed related files, it must either fix the failure, get an explicit reviewer-approved exception, or remain incomplete.

### Story R1.2: Create Master Refactor Roadmap Source Of Truth

As a developer,
I want a single current roadmap for the refactor program,
So that historical review notes do not give conflicting implementation direction.

**Acceptance Criteria:**

**Given** the existing `review/*.md` files contain conflicting recommendations
**When** the master roadmap is created
**Then** it identifies the accepted decisions for scripts, Flyway squash, OpenAPI, backend packaging, frontend extraction, mobile deferral, and docs handling.
**And** it links to historical review files as inputs rather than treating them as active instructions.

**Given** future refactor stories need sequencing
**When** the roadmap is read
**Then** it shows phase order, exit criteria, and the quality gate required before entering each phase.

### Story R1.3: Move Automation Scripts To Root Structure

As a developer,
I want all automation scripts available from a root `scripts/` structure grouped by concern,
So that commands are discoverable without losing ownership context.

**Acceptance Criteria:**

**Given** scripts currently live under `docker/scripts`, `infisical/scripts`, `apps/web/scripts`, and `apps/mobile/scripts`
**When** scripts are moved
**Then** they are placed under grouped root folders such as `scripts/docker`, `scripts/infisical`, `scripts/web`, `scripts/mobile`, `scripts/db`, and future `scripts/openapi` or `scripts/ci`.
**And** each moved script resolves `PROJECT_ROOT` correctly regardless of current working directory.

**Given** existing script behavior is known
**When** the move is complete
**Then** help output and non-destructive script checks still work.
**And** no application source behavior is changed.

### Story R1.4: Extract Shared Docker Script Utilities

As a developer,
I want Docker scripts to share common shell utilities,
So that preflight, color, timing, and project-root logic are maintained once.

**Acceptance Criteria:**

**Given** Docker scripts duplicate utility functions
**When** `scripts/docker/_common.sh` is introduced
**Then** `up.sh`, `down.sh`, `logs.sh`, and `cleanup.sh` source the common file.
**And** duplicated Docker preflight and color handling are removed from individual scripts.

**Given** a Docker script is run with `--help` or `--ci`
**When** the command executes
**Then** help output remains correct and CI/no-color behavior is preserved.

### Story R1.5: Analyze Flyway Migration Squash Groups

As a developer,
I want Flyway migrations inventoried and grouped before any squash execution,
So that DB lifecycle risk is understood before changing active migrations.

**Acceptance Criteria:**

**Given** the API has current Flyway migrations
**When** the migration analysis script runs
**Then** it lists all migrations in version order and assigns each to a proposed domain group.
**And** it proposes multiple logical baseline files rather than one giant baseline.

**Given** the analysis is complete
**When** the output is reviewed
**Then** it states that no active migration files were modified.
**And** it documents required rollout checks for fresh DB and existing migrated DB.

### Story R1.6: Update Script Documentation And Package Entrypoints

As a developer,
I want docs and package scripts to reference the new root scripts,
So that onboarding and operations commands remain accurate.

**Acceptance Criteria:**

**Given** scripts have moved to root
**When** documentation is updated
**Then** README, development guide, operations runbook, deployment guide, and Infisical docs reference the new paths.
**And** old script paths are absent from active operational docs.

**Given** root `package.json` exposes convenience scripts
**When** a developer reads available commands
**Then** common Docker and DB analysis commands are discoverable from root.

## Epic R2: API Contract Foundation With OpenAPI

Frontend and future mobile development can rely on generated API contracts and controlled shared constants instead of manually duplicated active/planned runtime contracts. The first story must decide generated output ownership and migration design before generating or adopting code.

### Story R2.1: Decide OpenAPI Generated Contract Ownership

As a developer,
I want a documented OpenAPI generation ownership decision,
So that generated API contracts have a stable location and review policy before code is generated.

**Acceptance Criteria:**

**Given** web and future mobile both need API contracts
**When** the decision is documented
**Then** it chooses the generated output location, generation command, commit policy, and consuming packages.
**And** it explains why alternatives were not selected.

**Given** generated output may affect package boundaries
**When** the decision is reviewed
**Then** it covers how `packages/shared`, `apps/web`, and future mobile usage interact.

### Story R2.2: Add OpenAPI Generation Script And Config

As a developer,
I want a repeatable OpenAPI generation command,
So that generated contracts can be recreated consistently.

**Acceptance Criteria:**

**Given** the API exposes OpenAPI docs through springdoc
**When** the generation script is added
**Then** it documents prerequisites, input OpenAPI source, output location, and failure behavior.
**And** it can run without changing unrelated source files.

**Given** generated files are created
**When** a developer reruns generation
**Then** output is deterministic or differences are explainable.

### Story R2.3: Quarantine Dead And Planned Shared Contracts

As a frontend developer,
I want inactive contracts removed from active exports,
So that frontend code does not rely on APIs or enums that do not exist.

**Acceptance Criteria:**

**Given** `DOCUMENTS_*`, document status/type, and subscription constants have no active backend implementation
**When** shared constants are cleaned
**Then** they are removed from active barrel exports or moved to a clearly non-runtime planned area.
**And** build/tests prove active consumers are updated.

**Given** a future feature needs a planned constant
**When** developers look for it
**Then** docs explain planned contracts are not active API guarantees.

### Story R2.4: Add Error Code Compatibility Layer

As a frontend developer,
I want backend semantic error codes mapped to frontend handling,
So that users receive consistent messages while the canonical error-code strategy is decided.

**Acceptance Criteria:**

**Given** backend emits semantic `ApiErrorCode` values
**When** frontend handles API errors
**Then** every backend code has an explicit frontend mapping or documented fallback.
**And** tests fail when a backend code lacks message handling.

**Given** frontend catalog-style codes exist
**When** the compatibility layer is added
**Then** the story does not require changing all backend error codes in the same PR.

### Story R2.5: Modularize Active Shared Constants

As a developer,
I want shared constants split by active concern,
So that API routes, profile values, upload policy, metric sources, and planned items are easier to maintain.

**Acceptance Criteria:**

**Given** constants are currently mixed across large files and barrel exports
**When** constants are modularized
**Then** active constants are organized into focused modules.
**And** barrel exports expose only active runtime contracts by default.

**Given** gender/profile constants have casing inconsistency
**When** the profile constants are clarified
**Then** the active casing contract is documented and tests cover schema usage.

### Story R2.6: Add Contract Drift CI Checks

As a developer,
I want CI to detect stale generated contracts and active contract drift,
So that manual changes do not silently break frontend/API compatibility.

**Acceptance Criteria:**

**Given** OpenAPI generation exists
**When** CI runs
**Then** it can verify generated output is current.
**And** it runs after shared build or an equivalent prerequisite.

**Given** some routes are frontend-only or planned
**When** drift checks run
**Then** exclusions are explicit and documented.

## Epic R3: Backend Feature-First Package Migration

Backend developers can change a domain by working inside a bounded package with internal layers and mirrored tests, starting with lower-risk near-feature modules before core domains.

### Story R3.1: Define Backend Package Boundary Rules

As a backend developer,
I want explicit package boundary rules before moving code,
So that every migration follows the same target architecture.

**Acceptance Criteria:**

**Given** backend currently mixes layer and feature packages
**When** boundary rules are documented
**Then** they define target top-level packages, allowed cross-package dependencies, and what belongs in `common`.
**And** they state that package moves must not rewrite service responsibilities in the same story.

**Given** architecture tests already exist
**When** rules are finalized
**Then** the test strategy for enforcing new package boundaries is documented.

### Story R3.2: Consolidate OCR Package Boundary

As a backend developer,
I want OCR-related code colocated under the OCR feature package,
So that OCR behavior can be changed without scanning layer-based roots.

**Acceptance Criteria:**

**Given** OCR code is split across service, controller, config, entity, repository, dto, and events
**When** OCR package migration is complete
**Then** OCR-related classes are under the OCR bounded package or a documented common package.
**And** package/import-only moves preserve behavior.

**Given** OCR tests exist
**When** tests are updated
**Then** OCR test package structure mirrors the new source structure and relevant tests pass.

### Story R3.3: Consolidate AI And RAG Package Boundary

As a backend developer,
I want AI/chat/embedding/RAG code colocated by AI concerns,
So that prompt, provider, retrieval, and corpus code has clear ownership.

**Acceptance Criteria:**

**Given** AI code is partially feature-based but related config/entities may be elsewhere
**When** migration is complete
**Then** AI-related config, services, entities, repositories, and tests are in the AI package hierarchy or documented common locations.
**And** prompt/resource paths remain valid.

### Story R3.4: Consolidate Activity And Notification Packages

As a backend developer,
I want activity and notification code moved into cohesive feature packages,
So that inbox, preferences, filters, events, and activity analytics are not scattered.

**Acceptance Criteria:**

**Given** activity code is split across constants, service, filter, entity, and repository
**When** migration is complete
**Then** activity code has one feature package with internal layers as needed.
**And** notification inbox/preferences code follows the same feature-first convention.

### Story R3.5: Move Support Domains To Feature Packages

As a backend developer,
I want consent, reminder, deletion, and email support domains colocated,
So that compliance and lifecycle features are easier to reason about.

**Acceptance Criteria:**

**Given** support domains are currently spread across layer packages
**When** each support domain is migrated
**Then** controller/service/entity/repository/scheduler/event files are moved into feature packages where applicable.
**And** cross-cutting pieces are placed in `common` only when reused across domains.

### Story R3.6: Move Core Domains To Feature Packages

As a backend developer,
I want auth, profile, healthrecord, referencedata, and admin code organized by domain,
So that future feature work touches fewer unrelated root packages.

**Acceptance Criteria:**

**Given** core domains are large and high-risk
**When** a core domain is migrated
**Then** each story moves one domain or one clearly bounded subdomain.
**And** tests are moved or updated in the same story.

**Given** migration may require architecture test updates
**When** a domain move is complete
**Then** package boundary tests reflect the new target structure.

### Story R3.7: Clean Resource Directory Ownership

As a backend developer,
I want resources organized by purpose,
So that seed data, prompt templates, fonts, migrations, and templates are unambiguous.

**Acceptance Criteria:**

**Given** `resources/ai` contains both seed data and prompts
**When** resources are reorganized
**Then** prompts and seed data are separated into documented locations.
**And** code loading those resources is updated with tests or startup checks.

## Epic R4: Backend Service Decomposition

Core backend behavior becomes easier to test and change because god services are split by cohesive responsibilities after package boundaries are stable.

### Story R4.1: Split HealthRecordService Responsibilities

As a backend developer,
I want health record query, command, analysis, sharing, and context responsibilities separated,
So that changes to one workflow do not require editing a 1700-line service.

**Acceptance Criteria:**

**Given** package boundaries are stable
**When** `HealthRecordService` is decomposed
**Then** responsibilities are split into cohesive services with clear method ownership.
**And** public controller behavior remains unchanged.

**Given** existing tests cover health record workflows
**When** decomposition is complete
**Then** tests are updated to target new collaborators and existing behavior still passes.

### Story R4.2: Split ReferenceDataAdminService Responsibilities

As an admin feature developer,
I want reference-data import, preview, apply, approval, and audit responsibilities separated,
So that admin reference-data changes are safer to review.

**Acceptance Criteria:**

**Given** `ReferenceDataAdminService` handles multiple workflows
**When** it is decomposed
**Then** import parsing, preview validation, change-set application, and audit concerns have clear service boundaries.
**And** admin reference-data tests remain behavior-focused.

### Story R4.3: Split LlmService Responsibilities

As an AI feature developer,
I want prompt rendering, provider calls, parsing, fallback, and caching separated,
So that LLM behavior can evolve without destabilizing health-record workflows.

**Acceptance Criteria:**

**Given** `LlmService` mixes several AI responsibilities
**When** it is decomposed
**Then** each responsibility has a focused collaborator and clear failure behavior.
**And** tests cover prompt assembly, provider failure, parsing, and fallback paths.

### Story R4.4: Split OcrService Responsibilities

As an OCR feature developer,
I want upload/job/provider/result persistence responsibilities separated,
So that OCR provider changes and job-state changes are isolated.

**Acceptance Criteria:**

**Given** OCR package boundary is consolidated
**When** `OcrService` is decomposed
**Then** upload orchestration, provider invocation, job state, and result persistence are separable units.
**And** OCR error and partial-result recovery tests still pass.

### Story R4.5: Split AdminAuditLogService Where Needed

As an admin feature developer,
I want audit-log query, filter, export, and statistics responsibilities separated when they are independently changing,
So that audit admin changes remain reviewable.

**Acceptance Criteria:**

**Given** audit log behavior is stable
**When** service split is performed
**Then** query/filter/export/statistics responsibilities are separated only where cohesion improves.
**And** audit log export and filtering tests verify behavior is unchanged.

## Epic R5: Web Feature Extraction And Route Shell Cleanup

Web developers can maintain high-risk screens through feature components, hooks, and domain utilities instead of thousand-line route pages, while preserving current UX.

### Story R5.1: Define Web Feature Extraction Pattern

As a frontend developer,
I want a documented extraction pattern for App Router pages,
So that large pages are refactored consistently.

**Acceptance Criteria:**

**Given** web pages mix route shell, data fetching, orchestration, and UI
**When** the pattern is documented
**Then** it defines route shell responsibilities, feature component folders, hook placement, lib placement, and test expectations.
**And** it references the health-record review page as the first application.

### Story R5.2: Extract Health Record Review Presentational Components

As a frontend developer,
I want pure UI sections extracted from the health-record review page,
So that the route page becomes easier to read without behavior changes.

**Acceptance Criteria:**

**Given** the review page currently contains UI sections inline
**When** presentational components are extracted
**Then** header, metric panels, explanation/recommendation sections, status/error/empty states, and action sections are moved into feature components.
**And** route behavior and visible UX remain unchanged.

### Story R5.3: Extract Health Record Review Hook And Domain Utilities

As a frontend developer,
I want review page state, queries, mutations, and mapping logic extracted,
So that data orchestration is testable outside the route file.

**Acceptance Criteria:**

**Given** the review page manages data and UI state inline
**When** hooks/utilities are extracted
**Then** query/mutation orchestration, derived state, and helper mapping logic live under the health-record feature.
**And** unit tests cover non-trivial derived state and error paths.

### Story R5.4: Refactor Admin Reference Data Pages

As an admin frontend developer,
I want reference-data admin pages extracted into feature components and hooks,
So that import, approval, and CRUD workflows are easier to maintain.

**Acceptance Criteria:**

**Given** health-record review extraction pattern is proven
**When** admin reference-data pages are refactored
**Then** page files become route shells and risk-bearing workflows remain explicit.
**And** preview, approval, rejection, publish, and error states remain covered.

### Story R5.5: Refactor Admin Audit And Admin Login Pages

As an admin frontend developer,
I want large admin audit/login pages reduced to shells with focused components,
So that admin workflows are maintainable and consistent.

**Acceptance Criteria:**

**Given** admin audit and login pages are large
**When** they are refactored
**Then** filters, tables, modals, auth form, and session state are moved into focused modules.
**And** existing admin tests or smoke tests still pass.

### Story R5.6: Move Loose Web Lib Files Into Domain Folders

As a frontend developer,
I want loose domain utilities moved into feature/domain folders,
So that imports communicate ownership clearly.

**Acceptance Criteria:**

**Given** `healthRecordHub.ts`, `profileMappings.ts`, and `notify.ts` live directly under `lib`
**When** they are moved
**Then** they land in appropriate domain folders and all imports are updated.
**And** tests verify moved utilities still behave the same.

## Epic R6: Documentation Source Of Truth And Historical Review Cleanup

The team finishes with clear phase docs, updated operational/developer documentation, and historical review files marked as inputs instead of conflicting instructions.

### Story R6.1: Mark Historical Review Files As Superseded Inputs

As a developer,
I want historical review files clearly marked,
So that old recommendations do not conflict with the accepted master roadmap.

**Acceptance Criteria:**

**Given** review files contain useful but stale or conflicting recommendations
**When** historical notes are updated
**Then** each file points to the current master roadmap.
**And** original historical content remains available for traceability.

### Story R6.2: Reorganize Active Documentation By Audience

As a project contributor,
I want docs organized by architecture, guides, operations, reference, and roadmap,
So that I can find current information quickly.

**Acceptance Criteria:**

**Given** docs are currently partly flat and partly grouped
**When** docs are reorganized
**Then** active docs have a documented folder structure and updated index.
**And** relative links are updated and checked.

### Story R6.3: Update Developer And Operations Guides After Refactor Phases

As a developer or operator,
I want guides to reflect the final script paths, generated contract commands, package structure, and test gates,
So that daily work follows the refactored repo.

**Acceptance Criteria:**

**Given** refactor phases have changed commands and structure
**When** docs are updated
**Then** README, development guide, testing guide, deployment guide, operations runbook, and environment reference reflect current commands.
**And** docs do not reference removed paths as active commands.

## Deferred Epic RD1: Mobile Structure Consolidation

Mobile code eventually uses one folder convention, but this is intentionally deferred until mobile work resumes.

### Story RD1.1: Decide Mobile Folder Convention

As a mobile developer,
I want a documented decision on root `app/` plus `src/` ownership,
So that future mobile work does not continue the dual-structure confusion.

**Acceptance Criteria:**

**Given** mobile work is resumed
**When** folder convention is decided
**Then** the decision states whether Expo Router routes stay in root `app/` and where components/hooks/lib/stores live.
**And** the decision includes migration steps and test/lint gates.

### Story RD1.2: Consolidate Mobile Source Structure

As a mobile developer,
I want mobile source folders consolidated into one convention,
So that future mobile features have clear ownership.

**Acceptance Criteria:**

**Given** mobile currently has root-level folders and `src/` folders
**When** consolidation is executed
**Then** components, hooks, lib, stores, and constants follow the approved convention.
**And** Expo start/lint behavior remains valid.

## Final Validation Summary

### Requirement Coverage

- RFR1-RFR23 are covered by the epic list and story set.
- RNFR1-RNFR10 are covered through governance, quality gates, generated contract checks, package migration rules, service decomposition constraints, frontend test expectations, and documentation requirements.
- UXR1-UXR5 are covered in Epic R5 stories for web feature extraction and route shell cleanup.

### Architecture And Dependency Validation

- The refactor backlog intentionally does not create product database tables/entities upfront.
- Flyway squash work is analysis-first and explicitly avoids modifying active migrations until rollout validation exists.
- OpenAPI generation starts with ownership/design before code generation.
- Backend feature packaging happens before service decomposition.
- Web feature extraction starts with a pattern and health-record review page before applying the pattern to admin pages.
- Mobile consolidation remains deferred and does not block active refactor work.

### Story Quality Validation

- Each story is scoped to a single bounded outcome suitable for one focused implementation pass.
- Acceptance criteria use Given/When/Then format and include preservation of behavior or test/validation expectations.
- Stories avoid depending on future stories within the same epic; later stories build on earlier explicit decisions or patterns.
- The existing product backlog file `_bmad-output/planning-artifacts/epics.md` is not overwritten by this refactor backlog.
