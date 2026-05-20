---
stepsCompleted: [step-01-init, step-02-checklist, step-03-proposals, step-04-document, step-05-approval]
workflowType: 'sprint-change-proposal'
project_name: health-lens
user_name: ie303
date: '2026-05-20'
changeType: 'epic-7-scope-expansion-and-resequencing'
scope: 'moderate'
status: 'approved'
---

# Sprint Change Proposal

**Date:** 2026-05-20  
**Project:** health-lens  
**Requested By:** ie303  
**Change Type:** Epic 7 scope expansion and resequencing

---

## 1. Issue Summary

### Problem Statement

Epic 7 hiện đang được sprint tracking như một epic gồm 4 story refactor:

- `7-1` Backend AI/OCR/RAG package boundary refactor
- `7-2` Split health record review page
- `7-3` Extract shared profile sharing hook
- `7-4` Email template cleanup

Sau đợt rà soát source code và kiến trúc, phạm vi này được xác định là **chưa đủ** để xử lý toàn bộ debt tổ chức code đang ảnh hưởng trực tiếp đến maintainability của hệ thống.

Vấn đề cốt lõi không chỉ là split file hoặc move package. Epic 7 thực tế còn đang thiếu:

- quyết định kiến trúc event-driven cho async tasks
- chuẩn hóa ranh giới `ai.*`, `ocr.*`, `events.*`
- chuẩn hóa route/constants hygiene
- tách tiếp các frontend admin hotspots
- xác nhận provider-switch readiness cho OCR/LLM/RAG
- cleanup mobile source root và docs source-tree

### Trigger Context

- **Trigger story:** `core-7-1-backend-ai-ocr-rag-package-boundary-refactor`
- **Discovery source:** architecture review và code organization review của Epic 7
- **Why now:** nếu bắt đầu dev `7-1` ngay, team có nguy cơ move package theo một structure sẽ phải đổi lại sau khi chốt event-driven boundary và provider seams

### Evidence

1. OCR đang event-driven khá rõ, nhưng email vẫn hybrid giữa stream-based và direct service calls.
2. Backend package structure đang nửa layer-first, nửa bounded-context extraction.
3. `HealthRecordService`, `AdminAuditLog` page, `ReferenceData` admin pages vẫn là hotspots lớn.
4. Route constants đã có, nhưng usage chưa được normalize hoàn toàn giữa `ApiPaths`, `API_ROUTES`, `ApiRoutes`, và literal child paths.
5. Provider switching có runbook và một số test, nhưng chưa có story riêng để chứng minh switch readiness end-to-end.

---

## 2. Impact Analysis

### Epic Impact

**Epic bị ảnh hưởng trực tiếp:** Epic 7 - Code Organization & Maintainability

**Thay đổi đề xuất:**

- Giữ nguyên `7-1` đến `7-4`
- Thêm `7-5` đến `7-13` vào Epic 7 theo backlog đã tổng hợp
- Ưu tiên lại thứ tự story để `7-9` đi trước `7-1`

**Backlog Epic 7 sau change proposal:**

1. `7-1` Backend AI/OCR/RAG package boundary refactor
2. `7-2` Split health record review page into feature components and hooks
3. `7-3` Extract shared profile sharing hook
4. `7-4` Email template cleanup
5. `7-5` Split admin audit log page into feature modules
6. `7-6` Split admin reference-data pages into feature modules
7. `7-7` Backend health record service responsibility split
8. `7-8` Shared frontend API contract types for high-churn features
9. `7-9` Confirm and document event-driven architecture
10. `7-10` Unify email event delivery
11. `7-11` Introduce application stream/event boundary
12. `7-12` Normalize mobile source root
13. `7-13` Refresh source tree and architecture docs

### Story Impact

**Stories needing resequencing:**

- `7-9` should be created and completed before broad backend refactor execution
- `7-1` should remain in backlog but should not be the first implementation story chosen
- `7-10` and `7-11` depend on `7-9`

**Stories needing scope clarification:**

- `7-1` should stay package-boundary focused, not absorb service decomposition
- `7-2` should stay organization-only, not redesign behavior
- `7-4` should coordinate with later email delivery unification

### Artifact Conflicts

| Artifact | Conflict Level | Action Required |
|----------|----------------|-----------------|
| `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` | Major | Expand Epic 7 stories beyond `7-1` to `7-4` |
| `_bmad-output/implementation-artifacts/sprint-status.yaml` | Major | Reflect approved new stories and resequence work selection |
| Epic 7 implementation artifacts | Moderate | Keep review docs and new story files aligned |
| `architecture.md` | Minor | No immediate architecture rewrite required before approval, but later docs should reflect event-driven decision |
| `prd.md` | None/Minor | No product-scope change; this is engineering organization debt, not MVP feature drift |
| `ux-design-specification.md` | None | No new UX scope implied by the Epic 7 expansion |

### Technical Impact

- **Code:** No behavior change required immediately; this proposal is backlog and sequencing work
- **Architecture:** Event-driven and package-boundary decisions need explicit confirmation before backend refactor execution
- **Testing:** Later stories should preserve existing behavior and add focused verification where seams are fragile
- **Delivery risk:** Starting `7-1` too early increases rework risk

---

## 3. Recommended Approach

### Selected Path: Direct Adjustment + Backlog Expansion

**Approach type:** Hybrid of Option 1 (Direct Adjustment) with moderate backlog reorganization

### Rationale

| Factor | Assessment |
|--------|------------|
| Implementation effort | **Medium** - needs backlog/story updates, not product redesign |
| Timeline impact | **Low/Medium** - short-term planning overhead prevents backend rework later |
| Technical risk | **Lower than current plan** - because architecture assumptions are surfaced earlier |
| Team momentum | **Positive** - reduces ambiguity before dev starts |
| Maintainability value | **High** - aligns Epic 7 to the real debt in the codebase |

### Why Not Other Options?

- **Rollback:** not appropriate because the problem is planning scope, not bad shipped code
- **PRD/MVP reduction:** not required because user-facing product scope is unchanged

### Trade-offs

- Sprint planning becomes a bit heavier now
- But that cost is smaller than doing `7-1` and then refactoring package/event boundaries again

---

## 4. Detailed Change Proposals

### 4.1 Update Epic 7 Scope In Planning Backlog

**File:** `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md`  
**Section:** `## Epic 7: Code Organization & Maintainability`

**OLD**

```markdown
Epic 7 currently contains only Stories 7.1 through 7.4.
Goal: Giảm technical debt đang tăng: tách bounded context AI/OCR/RAG/provider, split page lớn, gom logic lặp và email templates.
```

**NEW**

```markdown
Epic 7 contains Stories 7.1 through 7.13.
Goal: Giảm technical debt đang tăng trên backend, frontend, async boundaries, route/constants usage, mobile source organization, và documentation drift.

Important sequencing note:
- Story 7.9 must be completed before broad execution of Story 7.1.
- Story 7.10 and 7.11 depend on Story 7.9.
```

**Rationale:** phạm vi Epic 7 hiện tại không còn phản ánh đầy đủ debt đã được review.

### 4.2 Add New Stories 7.5 Through 7.13 To Planning Backlog

**File:** `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md`  
**Section:** append under Epic 7

**Stories to add**

- `7.5` Split Admin Audit Log Page Into Feature Modules
- `7.6` Split Admin Reference Data Pages Into Feature Modules
- `7.7` Backend Health Record Service Responsibility Split
- `7.8` Shared Frontend API Contract Types For High-Churn Features
- `7.9` Confirm And Document Event-Driven Architecture
- `7.10` Unify Email Event Delivery
- `7.11` Introduce Application Stream/Event Boundary
- `7.12` Normalize Mobile Source Root
- `7.13` Refresh Source Tree And Architecture Docs

**Rationale:** đây là các work items đã được biến thành story files và phản ánh các vấn đề thật trong codebase hiện tại.

### 4.3 Strengthen Story 7.1 Positioning

**File:** `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md`  
**Section:** Story 7.1 scope/acceptance notes

**OLD**

```markdown
- Introduce packages such as:
  - com.healthlens.api.ai.chat
  - com.healthlens.api.ai.embedding
  - com.healthlens.api.ai.rag
  - com.healthlens.api.ocr
  - com.healthlens.api.provider
```

**NEW**

```markdown
- Introduce packages such as:
  - com.healthlens.api.ai.chat
  - com.healthlens.api.ai.embedding
  - com.healthlens.api.ai.rag
  - com.healthlens.api.ai.prompt
  - com.healthlens.api.ocr
  - com.healthlens.api.ocr.provider
  - com.healthlens.api.ocr.job
- Keep Story 7.1 as a package-boundary refactor only.
- Do not absorb HealthRecordService decomposition, event-stream boundary work, or email delivery unification into this story.
- Execute only after Story 7.9 confirms event/package ownership rules.
```

**Rationale:** tránh biến `7-1` thành một “mega refactor” khó kiểm soát.

### 4.4 Resequence Sprint Execution Guidance

**File:** `_bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/epic-7-issues-and-proposed-stories.md`

**OLD**

```markdown
1. 7-1
2. 7-2
3. 7-3
4. 7-4
```

**NEW**

```markdown
1. 7-9 Confirm and document event-driven architecture
2. 7-1 Backend AI/OCR/RAG package boundary refactor
3. 7-11 Introduce application stream/event boundary
4. 7-10 Unify email event delivery
5. 7-2 Split health record review page
6. 7-5 Split admin audit log page
7. 7-6 Split admin reference-data pages
8. 7-3 Extract shared profile sharing hook
9. 7-4 Email template cleanup
10. 7-7 Backend health record service responsibility split
11. 7-8 Shared frontend API contract types
12. 7-12 Normalize mobile source root
13. 7-13 Refresh docs
```

**Rationale:** thứ tự này giảm rủi ro rework ở backend và vẫn giữ frontend refactor track rõ ràng.

### 4.5 Sprint Status Update Proposal

**File:** `_bmad-output/implementation-artifacts/sprint-status.yaml`

**Recommended change after approval**

```diff
  core-7-5-split-admin-audit-log-page-into-feature-modules: backlog
  core-7-6-split-admin-reference-data-pages-into-feature-modules: backlog
  core-7-7-backend-health-record-service-responsibility-split: backlog
  core-7-8-shared-frontend-api-contract-types-for-high-churn-features: backlog
  core-7-9-confirm-and-document-event-driven-architecture: backlog
  core-7-10-unify-email-event-delivery: backlog
  core-7-11-introduce-application-stream-event-boundary: backlog
  core-7-12-normalize-mobile-source-root: backlog
  core-7-13-refresh-source-tree-and-architecture-docs: backlog
```

**Additional sequencing note**

- Keep `core-7-1-backend-ai-ocr-rag-package-boundary-refactor` in the backlog pool, but do not execute before `core-7-9`.
- If sprint-status vocabulary allows only one “ready” lane, prefer moving `7-9` to `ready-for-dev` first and leave `7-1` as `backlog`.

**Rationale:** sprint tracking should reflect the approved order, not only the old 4-story snapshot.

---

## 5. Implementation Handoff

### Scope Classification

**Moderate**

Lý do:

- không cần đổi PRD hoặc product scope
- nhưng cần backlog reorganization chính thức
- cần cập nhật planning artifact và sprint status
- cần tạo nhịp handoff rõ ràng trước khi dev

### Handoff Recipients

- **Product Owner / Scrum Master workflow**
  - approve Epic 7 scope expansion
  - approve story resequencing
  - update sprint tracking
- **Story creation / context workflow**
  - ensure each added story has a dedicated implementation artifact
  - validate the highest-priority new story first (`7-9`)
- **Development workflow**
  - start with `7-9` after approval, not `7-1`

### Success Criteria

1. Epic 7 planning backlog includes stories `7.5` through `7.13`.
2. Sprint status reflects the approved expanded backlog.
3. `7-9` is recognized as the first story to prepare/validate before backend package refactor.
4. Team does not start `7-1` under the old assumption that Epic 7 is only a safe package cleanup.

---

## 6. Checklist Summary

### Section 1: Understand The Trigger And Context

- `[x]` 1.1 Triggering story identified: `7-1`
- `[x]` 1.2 Core problem defined: Epic 7 under-scoped after architecture/code review
- `[x]` 1.3 Evidence collected from source review, architecture review, and code organization assessment

### Section 2: Epic Impact Assessment

- `[x]` 2.1 Current epic can continue, but only with expanded scope
- `[x]` 2.2 Epic-level changes identified: add stories, resequence work
- `[x]` 2.3 Remaining epics reviewed: no mandatory renumbering outside Epic 7
- `[x]` 2.4 New stories needed: yes, inside existing Epic 7
- `[x]` 2.5 Epic/story priority should change: yes

### Section 3: Artifact Conflict And Impact Analysis

- `[x]` 3.1 PRD conflict: no product-scope conflict
- `[x]` 3.2 Architecture conflict: yes, current execution order conflicts with unresolved event/package decisions
- `[x]` 3.3 UI/UX conflict: none requiring UX redesign
- `[x]` 3.4 Other artifacts impacted: sprint status, docs, test strategy, route/constants guidance

### Section 4: Path Forward Evaluation

- `[x]` 4.1 Direct Adjustment: viable
- `[N/A]` 4.2 Rollback: not applicable
- `[N/A]` 4.3 PRD MVP Review: not required
- `[x]` 4.4 Recommended path selected: Direct Adjustment + Backlog Expansion

### Section 5: Sprint Change Proposal Components

- `[x]` 5.1 Issue summary created
- `[x]` 5.2 Epic and artifact impact documented
- `[x]` 5.3 Recommended path and rationale documented
- `[x]` 5.4 High-level action plan defined
- `[x]` 5.5 Agent handoff plan defined

### Section 6: Final Review And Handoff

- `[x]` 6.1 Checklist completed
- `[x]` 6.2 Proposal reviewed for consistency
- `[!]` 6.3 Explicit user approval still required
- `[!]` 6.4 Sprint status update should happen only after approval

---

## 7. Immediate Next Step

If approved, the next ordered actions should be:

1. Update sprint tracking for the approved new Epic 7 stories.
2. Normalize/create the highest-priority implementation artifact path around `7-9`.
3. Run `bmad-create-story` or story-validation flow for `7-9` first.

