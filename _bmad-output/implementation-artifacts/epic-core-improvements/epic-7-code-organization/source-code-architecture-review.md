# Source Code Architecture Review For Epic 7

Status: review  
Date: 2026-05-20  
Scope: source tree organization, event-driven consistency, async task boundaries, module ownership, contracts, and refactor risks.

Consolidated backlog synthesis: `epic-7-issues-and-proposed-stories.md`

## Executive Summary

Epic 7 should not be treated as only "move files and split large pages". The source code now contains several architectural patterns that are only partially applied:

- Event-driven behavior exists, but only OCR and verification email meaningfully use Redis Streams.
- Email delivery is split between stream-based async, after-commit direct calls, and in-transaction direct calls.
- Backend package structure is still layer-first, while newer domains need bounded-context boundaries.
- Web source organization has started feature folders, but high-complexity route pages still own data fetching, mutations, type definitions, UI, formatting, and workflow state.
- Shared contracts exist for routes and some schemas, but high-churn response/request types remain local and easy to drift.

This means Epic 7 needs two tracks:

1. Safe refactor stories that preserve behavior.
2. Architecture confirmation items that decide what pattern future refactors must align to.

## Findings

### 1. Event-driven architecture is not consistently applied

Evidence:

- `HealthRecordService.confirmUpload` publishes OCR jobs to Redis Stream after commit.
- `OcrJobConsumer` consumes `ocr.events`, handles pending/new entries, retries, DLQ, and correlation context.
- `AuthService.register` publishes verification email event to `email.events` after commit.
- `EmailConsumer` only handles `eventType = "verification"`.
- Password reset, deletion emails, profile invitations, health record invitations, and follow-up reminder emails still call `EmailService` directly.

Impact:

The project does not yet have one agreed event-driven architecture. It has a hybrid model. That is acceptable if intentional, but dangerous if future stories assume "all background work is event-driven".

Decision needed:

Confirm whether HealthLens standard is:

- Event-driven for all side effects that can happen after commit.
- Event-driven only for heavy/background processing like OCR.
- Hybrid, with explicit rules for which flows stay direct.

Recommended direction:

Use event-driven for durable async tasks: OCR, all user-facing emails, owner notifications, product analytics events, and potentially future mobile sync notifications. Keep direct service calls for synchronous domain invariants only.

### 2. Email architecture is incomplete relative to earlier story intent

Evidence:

- Story `2-4-email-system-improvements` described event-driven email with multiple event types such as verification and password reset.
- Current `EmailEvent` contains a generic `eventType`, `email`, and `data`, but `toStreamMap()` only emits `eventType`, `userId`, `email`, and `token`.
- `EmailConsumer.handleRecord()` ignores anything except `"verification"`.
- `AuthService.forgotPassword`, `DataDeletionService`, `ProfileShareService`, `HealthRecordShareService`, and `FollowUpReminderService` still call `EmailService` directly.

Impact:

The code shape suggests event-driven email, but runtime behavior is only partially event-driven. This creates inconsistent latency, retry, and failure semantics.

Recommended backlog:

Add a story before or near `7-4`: "Unify Email Delivery Events And Consumer Handling".

Suggested scope:

- Define typed email event kinds.
- Move password reset, deletion, invitation, and reminder email dispatch behind one event publisher.
- Keep template rendering in `EmailService` or an `EmailTemplateService`.
- Add dead-letter or retry strategy for email stream failures.

### 3. Redis Streams are used as a queue, but there is no shared queue abstraction

Evidence:

- `HealthRecordService` publishes OCR events directly with `redisTemplate.opsForStream().add(...)`.
- `AuthService` publishes email events directly with `redisTemplate.opsForStream().add(...)`.
- `OcrJobStateService` re-enqueues OCR retries directly with `redisTemplate.opsForStream().add(...)`.
- `OcrJobConsumer` and `EmailConsumer` each implement their own stream group bootstrap/read/ack logic.

Impact:

Queue behavior is duplicated and hidden inside domain services. Package refactor alone will not clarify ownership of stream publishing, retry policy, payload shape, or consumer-group lifecycle.

Recommended backlog:

Add a story: "Introduce Application Event Publisher Boundary".

Suggested target:

```text
com.healthlens.api.events
com.healthlens.api.events.email
com.healthlens.api.events.ocr
com.healthlens.api.events.stream
```

This should not be a generic framework. Keep it small: publisher interfaces, payload records, stream names, and shared bootstrap helpers.

### 4. OCR pipeline is event-driven, but package ownership is blurred

Evidence:

- OCR queue state lives in `OcrJobStateService`.
- OCR consumer lives in generic `service`.
- OCR provider interfaces are under `service/ocr`.
- OCR provider adapters still live in generic `service`.
- OCR result parsing is inside `OcrService`.
- Health record persistence callbacks are in `HealthRecordService`.

Impact:

The OCR flow is behaviorally solid, but source ownership is split across generic service classes. This is exactly why `7-1` matters, but `7-1` should also define the boundary between OCR processing and health-record persistence.

Decision needed:

Confirm whether OCR owns only extraction, routing, retries, and DLQ, while Health Record owns record state transitions.

Recommended boundary:

- OCR package owns queue payload, provider routing, retry/DLQ, raw extraction, parse result.
- Health Record package owns `processing -> review_required|ocr_failed|done`, profile timestamps, sharing-aware access.
- The bridge should be a small port/service, not arbitrary cross-calls from large services.

### 5. HealthRecordService is the biggest domain boundary violation

Evidence:

`HealthRecordService.java` is around 1,686 lines and owns:

- upload URL reservation.
- shared-editor upload authorization.
- Redis upload reservation storage.
- OCR event publishing.
- status polling/cache.
- detail/history response assembly.
- shared-record access.
- OCR completion/failure state transitions.
- record confirmation/manual edit.
- delete/purge.
- PDF/recommendation orchestration.
- audit calls.

Impact:

Any future refactor of OCR, sharing, PDF, or history can accidentally affect unrelated behavior. This is the highest-risk backend source organization issue outside the AI/OCR package move.

Recommended backlog:

Keep proposed `7-7`, but split it into two stories if implementation risk feels high:

- `7-7a`: Extract health record access policy and upload reservation/publisher.
- `7-7b`: Extract history/detail assemblers and OCR state transition collaborator.

### 6. ReferenceDataAdminService is another large boundary, but less urgent than health records

Evidence:

`ReferenceDataAdminService.java` is around 1,496 lines and mixes CRUD, import preview/session, validation, change-set lifecycle, publish/approve/reject logic, and internal records.

Impact:

Admin reference data is complex enough to deserve its own backend package later. But it is less entangled with event-driven architecture than HealthRecord/OCR.

Recommended handling:

Do not include this in `7-1`. Track as a later backend organization story after admin frontend decomposition.

### 7. Backend source tree has empty or misleading package roots

Evidence:

- `apps/api/src/main/java/com/healthlens/api/event` exists but has no source files.
- `apps/api/src/main/java/com/healthlens/api/model` exists but has no source files.
- `dto/event/EmailEvent.java` exists, but the actual event publishing/consuming behavior lives in `service`.

Impact:

The source tree hints at an event architecture, but the code does not use that package boundary. Empty packages also confuse future agents and developers during navigation.

Recommended cleanup:

Either remove empty package directories or turn them into real event boundary packages as part of the event publisher story.

### 8. Backend package strategy needs an explicit rule: layer-first with extracted bounded contexts

Current state:

- Most backend packages are layer-first: `controller`, `service`, `repository`, `entity`, `dto`.
- Admin has started partial subpackages: `controller/admin`, `dto/admin`, `service/admin`.
- OCR/RAG has started partial subpackages: `service/ocr`, `service/rag`.

Risk:

A full feature-first migration now would create churn without enough payoff. But continuing to put everything under generic `service` will keep the project hard to navigate.

Recommended rule:

Adopt "layer-first baseline, bounded-context extraction for volatile domains".

Initial extracted domains:

- `ai.*`
- `ocr.*`
- `events.*`
- `admin.reference`
- `healthrecord.*`
- `sharing.*` only if profile and health-record sharing continue to grow.

### 9. Web source tree has feature folders, but route pages still own too much

Evidence:

Largest route pages:

- `health-records/review/[recordId]/page.tsx`: around 2,446 lines.
- `admin/audit-log/page.tsx`: around 1,813 lines.
- `admin/reference-data/approvals/page.tsx`: around 1,098 lines.
- `admin/reference-data/page.tsx`: around 1,035 lines.

Impact:

The web app has a reasonable folder skeleton, but route pages are acting as feature modules. This makes changes hard to review and test.

Recommended rule:

Route files should own route-level composition only:

- params/search params.
- page-level loading/error shells.
- layout composition.
- passing data to feature components.

Feature modules should own:

- query hooks.
- mutation hooks.
- form state.
- transformation helpers.
- modals/tables/panels.
- local types where not shared.

### 10. Web admin code needs its own source organization track

Evidence:

Admin pages are large and have separate API client/session behavior. They also use custom route constants through both `API_ROUTES` and `ApiPaths`.

Impact:

Admin is becoming a separate app surface inside `apps/web`. Keeping all admin logic under `app/admin/**/page.tsx` will make future audit/reference-data changes brittle.

Recommended structure:

```text
apps/web/src/components/admin/audit-log/
apps/web/src/components/admin/reference-data/
apps/web/src/hooks/admin/
apps/web/src/lib/admin/auditLog.ts
apps/web/src/lib/admin/referenceData.ts
apps/web/src/types/admin/
```

This supports proposed `7-5` and `7-6`.

### 11. Shared contracts are route-centric, not domain-contract-centric

Evidence:

- `packages/shared/constants/api.ts` is widely used and is useful.
- Shared Zod schemas exist for auth/profile/user.
- Newer domain response types for audit logs, reference data, health record review, and sharing are mostly page-local TypeScript types.
- Backend DTOs are Java records/classes with no generated or checked frontend counterpart.

Impact:

Route drift is partly managed, but payload drift is still manual. The largest pages duplicate many response shapes locally.

Recommended decision:

Confirm one of these:

- Manual shared TypeScript contracts for high-churn features.
- OpenAPI generation for frontend client/types.
- Keep local types, but require page extraction to central `apps/web/src/types/<feature>.ts`.

Recommended near-term choice:

Manual shared or app-local domain types first. Do not introduce codegen inside Epic 7 unless architecture explicitly approves it.

### 12. Mobile source tree has duplicate-looking roots

Evidence:

- `apps/mobile/app/index.tsx` exists.
- `apps/mobile/src/app/index.tsx` and `apps/mobile/src/app/_layout.tsx` also exist.
- Root-level `components`, `hooks`, `lib`, and `stores` exist as index placeholders while real implementation appears under `src`.

Impact:

Mobile is lighter, but the source tree is ambiguous. Expo Router can support `app/` at root; the project also has `src/app`. Future agents may edit the wrong entry point.

Decision needed:

Confirm whether mobile canonical source root is:

- root `app/`, `components/`, `hooks/`, etc.
- or `src/app`, `src/components`, `src/hooks`, etc.

Recommended backlog:

Add a low-priority cleanup story before mobile Phase 2: "Normalize Mobile Source Root And Remove Placeholder Duplicates".

### 13. Docs/source-tree-analysis is now slightly stale

Evidence:

- `docs/source-tree-analysis.md` mentions Flyway migrations `V001` through `V027`, while resources now include through `V042`.
- It does not document newer event/stream flows, audit/correlation spine, online RAG citation admin, or current Epic 7 package targets.

Impact:

Agents relying on docs may under-read important newer systems.

Recommended backlog:

After Epic 7 refactors, update `docs/source-tree-analysis.md` and `docs/architecture.md` with:

- event-driven decisions.
- package boundaries.
- stream names and event payload ownership.
- current migration range.

### 14. Current event reliability semantics are uneven

Evidence:

- OCR has DB job state, retry, DLQ, idempotency key, and audit events.
- Email verification stream has no visible DLQ and only logs failed records without acking.
- Direct emails are best-effort in some places and blocking in others.
- Follow-up reminders use claim/release/timeout in DB, not Redis Streams.

Impact:

Operational behavior differs per task. This may be fine, but it must be explicit.

Decision needed:

For each async category, confirm required delivery semantics:

- OCR: at-least-once with idempotent state transitions and DLQ.
- Email: at-least-once, best-effort, or fire-and-forget?
- Reminders: DB-claimed scheduled job or stream event?
- Audit: synchronous write, after-commit write, or outbox?
- Analytics/product events: synchronous counters, DB events, or stream?

### 15. Audit/correlation is stronger than older docs imply, but source boundaries could be clearer

Evidence:

- `correlation/CorrelationContext` and `CorrelationIdFilter` exist.
- `audit/UnifiedAuditCoordinator`, `UnifiedAuditLogWriter`, and `AuditEventRecorder` exist.
- OCR consumer ensures correlation context for jobs.
- Audit log admin pages consume broader audit and online RAG citation data.

Impact:

This is a useful cross-cutting spine. But because many domain services call audit directly, future extraction must preserve audit context carefully.

Recommended rule:

When extracting services, preserve actor/correlation/resource ID at the boundary. Do not hide audit inside low-level helpers unless the helper owns the domain action.

## Architecture Decisions To Confirm Before More Refactor

1. Are Redis Streams the standard event bus for all async domain side effects, or only OCR/email?
2. Should all user-facing emails move to `email.events`, or are some direct sends intentionally synchronous?
3. What delivery guarantee is required for email: best-effort, retry-until-success, or DLQ after N failures?
4. Should event publication use an outbox table instead of direct Redis publish after commit for critical events?
5. Should OCR package own only extraction/retry/DLQ, while Health Record owns state transitions?
6. Should backend remain layer-first with bounded-context extraction, or migrate toward feature-first packages?
7. Should frontend feature modules live under `components/features`, `features/<domain>`, or current mixed structure?
8. Should high-churn API payload types move to `packages/shared`, `apps/web/src/types`, or be generated from OpenAPI?
9. Which mobile source root is canonical: root `app/` or `src/app/`?
10. Should docs be updated before implementing additional Epic 7 stories, or after refactors settle?

## Recommended New/Adjusted Stories

### Add 7.5: Split Admin Audit Log Page Into Feature Modules

Keep from previous assessment. It is still valid.

### Add 7.6: Split Admin Reference Data Pages Into Feature Modules

Keep from previous assessment. It is still valid.

### Add 7.7: Backend Health Record Service Responsibility Split

Keep, but consider splitting into `7-7a` and `7-7b`.

### Add 7.8: Shared Frontend API Contract Types For High-Churn Features

Keep, but do not introduce code generation unless explicitly approved.

### Add 7.9: Confirm And Document Event-Driven Architecture

Priority: P1 before broad backend refactor  
Area: Architecture decision, backend events

Acceptance criteria:

- Decision recorded for Redis Streams vs direct service calls vs DB scheduled jobs.
- Email, OCR, reminders, audit, analytics, and notifications each have assigned delivery semantics.
- Package target for event publisher/consumer code is documented.
- Existing stories updated to avoid contradicting the decision.

### Add 7.10: Unify Email Event Delivery

Priority: P2, after 7.9  
Area: Backend email async architecture

Acceptance criteria:

- Email event kinds cover verification, password reset, deletion, cancellation, profile invitation, health-record invitation, and reminder if approved.
- `EmailConsumer` routes all supported event types.
- Direct `EmailService` calls are removed from domain services where event-driven behavior is approved.
- Retry/DLQ or explicit best-effort behavior is documented and tested.

### Add 7.11: Introduce Application Stream/Event Boundary

Priority: P2  
Area: Backend event source organization

Acceptance criteria:

- Domain services no longer call `redisTemplate.opsForStream().add(...)` directly.
- OCR and email publish through small typed publisher classes.
- Consumer group bootstrap/read/ack behavior is centralized where practical.
- No behavior change to existing OCR retry/idempotency semantics.

### Add 7.12: Normalize Mobile Source Root

Priority: P3, before mobile Phase 2  
Area: Mobile source organization

Acceptance criteria:

- Canonical Expo Router root is documented.
- Duplicate placeholder roots are removed or intentionally documented.
- Imports and README reflect the chosen source root.

### Add 7.13: Refresh Source Tree And Architecture Docs

Priority: P3, after package/event decisions  
Area: Documentation for future agents

Acceptance criteria:

- `docs/source-tree-analysis.md` reflects current migrations, package roots, and generated folders.
- `docs/architecture.md` documents event-driven/async decisions.
- Epic 7 package map is linked from docs or project context.

## Recommended Epic 7 Order After This Review

1. `7.9` Confirm and document event-driven architecture.
2. `7.1` Backend AI/OCR/RAG package boundary refactor.
3. `7.11` Introduce application stream/event boundary.
4. `7.10` Unify email event delivery.
5. `7.2` Split health record review page.
6. `7.3` Extract shared profile sharing hook.
7. `7.4` Email template cleanup.
8. `7.5` Admin audit log page decomposition.
9. `7.6` Admin reference data page decomposition.
10. `7.7a/7.7b` Health record service responsibility split.
11. `7.8` Shared frontend API contract types.
12. `7.12` Mobile source root cleanup.
13. `7.13` Refresh docs.

## Immediate Recommendation

Do not start `7-1` yet if the goal is a clean architecture refactor, because package targets depend on whether `events.*`, `ocr.*`, and `ai.*` become first-class packages. First, confirm `7.9` decisions. Then update the story files so implementation agents do not move code into a package structure that will be changed again one story later.

