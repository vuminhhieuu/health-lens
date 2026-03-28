---
stepsCompleted: [step-01-init, step-02-checklist, step-03-proposals, step-04-document, step-05-approval]
workflowType: 'sprint-change-proposal'
project_name: health-lens
user_name: ie303
date: '2026-03-28'
changeType: 'strategic-reorganization'
scope: 'moderate'
status: 'approved'
---

# Sprint Change Proposal

**Date:** 2026-03-28
**Project:** health-lens
**Requested By:** ie303
**Change Type:** Strategic Platform Reorganization

---

## 1. Issue Summary

### Problem Statement

Epics và stories hiện tại được thiết kế cho cả Web + Mobile song song, dẫn đến:

1. **Stories mobile-specific** (camera capture, offline sync) nằm xen kẽ trong các epics
2. **Không có ranh giới rõ ràng** giữa Web MVP và Mobile enhancement
3. **Khó ưu tiên và track tiến độ** khi implement

### Trigger Context

- **Source:** Strategic decision sau khi hoàn thành UI design trên Stitch MCP
- **Discovery:** Planning phase - trước khi bắt đầu implementation
- **Rationale:** Tập trung Web-first để ship MVP nhanh hơn, Mobile features là enhancement

### Evidence

| Story | FR | Platform | Current Epic | Issue |
|-------|-----|----------|--------------|-------|
| Story 3.2 | FR11 | Mobile-only | Epic 3 | Camera capture không applicable cho Web |
| Story 5.4 | FR27 | Mobile-only | Epic 5 | Offline read-only là mobile feature |
| Story 5.5 | FR28 | Mobile-only | Epic 5 | Auto-sync là mobile feature |

---

## 2. Impact Analysis

### Epic Impact

| Epic | Current Stories | After Change | Impact |
|------|-----------------|--------------|--------|
| Epic 1 | 6 | 6 | No change |
| Epic 2 | 3 | 3 | No change |
| Epic 3 | 6 | 5 | -1 story (3.2 moved) |
| Epic 4 | 5 | 5 | No change |
| Epic 5 | 5 | 3 | -2 stories (5.4, 5.5 moved) |
| Epic 6 | 4 | 4 | No change |
| Epic 7 | 5 | 5 | No change |
| Epic 8 | 3 | 3 | No change |
| **Epic 9 (NEW)** | 0 | 3 | +3 stories (from Epic 3, 5) |

**Total:** 37 stories → 37 stories (reorganized, not removed)

### Story Impact

**Stories Moving to Epic 9:**

1. **Story 3.2 → Story 9.1:** Chụp ảnh kết quả khám trên mobile với camera UX
2. **Story 5.4 → Story 9.2:** Offline read-only cho lịch sử trên mobile
3. **Story 5.5 → Story 9.3:** Tự động đồng bộ khi kết nối được khôi phục

### Artifact Conflicts

| Artifact | Conflict Level | Action Required |
|----------|----------------|-----------------|
| PRD | Minor | Update P1 scope clarification |
| Architecture | None | No changes needed |
| UX Design | None | Already supports both platforms |
| Epics | Major | Reorganize stories, add Epic 9 |
| Story Files | Moderate | Move 3 files to epic-9 folder |

### Technical Impact

- **Code:** None - no implementation started
- **Infrastructure:** None - architecture unchanged
- **Deployment:** None - CI/CD unchanged

---

## 3. Recommended Approach

### Selected Path: Direct Adjustment

**Rationale:**

| Factor | Assessment |
|--------|------------|
| Implementation effort | **Low** - Document reorganization only |
| Timeline impact | **None** - No MVP delay |
| Technical risk | **None** - No architecture changes |
| Team morale | **Positive** - Clearer focus |
| Business value | **Maintained** - Core value intact |

### Why Not Other Options?

- **Rollback:** Not applicable - no code to rollback
- **MVP Review:** Not needed - core value unchanged, only mobile features deferred

### Trade-offs Considered

| Trade-off | Decision |
|-----------|----------|
| Mobile users can't use camera in MVP | Acceptable - can still upload from library |
| Offline mode not in MVP | Acceptable - requires network anyway for OCR/LLM |
| Longer time to full mobile experience | Acceptable - Web MVP ships faster |

---

## 4. Detailed Change Proposals

### 4.1 PRD Update

**File:** `_bmad-output/planning-artifacts/prd.md`
**Section:** Product Scope → P1 — Core (Launch)

```diff
### P1 — Core (Launch)

-- Upload PDF/ảnh + chụp camera (mobile) → OCR → hiển thị chỉ số kèm giải thích LLM
++ Upload PDF/ảnh từ thư viện (Web + Mobile) → OCR → hiển thị chỉ số kèm giải thích LLM
++ **Note:** Mobile camera capture (FR11), offline mode (FR27), auto-sync (FR28) deferred to Phase 2
```

### 4.2 Epic 3 Update

**File:** `_bmad-output/planning-artifacts/epics.md`
**Section:** Epic 3

```diff
## Epic 3: Thu Thập Kết Quả Khám và Khôi Phục OCR
Goal: Hoàn thiện end-to-end upload + OCR + manual recovery, tránh dead-end khi OCR không chính xác.
++ **Platform:** Web (Phase 1) | **Stories:** 5
**FRs covered:** FR9, FR10, FR12, FR13, FR14, FR15, FR16, FR17.
-- **FRs covered:** FR9, FR10, FR11, FR12, FR13, FR14, FR15, FR16, FR17.

-- ### Story 3.2: Chụp ảnh kết quả khám trên mobile với camera UX
-- [entire story content removed - moved to Epic 9]
```

### 4.3 Epic 5 Update

**File:** `_bmad-output/planning-artifacts/epics.md`
**Section:** Epic 5

```diff
## Epic 5: Lịch Sử Khám và Đồng Bộ Đa Trạng Thái
-- Goal: Cho phép truy cập lịch sử liên tục trên mobile/web kể cả khi mạng gián đoạn.
++ Goal: Cho phép truy cập lịch sử trên web, xem lại chi tiết và xóa bản ghi.
++ **Platform:** Web (Phase 1) | **Stories:** 3
-- **FRs covered:** FR24, FR25, FR26, FR27, FR28.
++ **FRs covered:** FR24, FR25, FR26.

-- ### Story 5.4: Offline read-only cho lịch sử trên mobile
-- [entire story content removed - moved to Epic 9]

-- ### Story 5.5: Tự động đồng bộ khi kết nối được khôi phục
-- [entire story content removed - moved to Epic 9]
```

### 4.4 Add Epic 9

**File:** `_bmad-output/planning-artifacts/epics.md`
**Section:** New Epic (after Epic 8)

```markdown
## Epic 9: Nâng Cao Trải Nghiệm Mobile (Phase 2)

Goal: Cung cấp trải nghiệm mobile native với camera capture trực tiếp, offline access và đồng bộ tự động.

**Platform:** Mobile (Phase 2) | **Stories:** 3
**FRs covered:** FR11, FR27, FR28.
**Dependencies:** Epic 1-5 completed (backend APIs ready)

### Story 9.1: Chụp ảnh kết quả khám trên mobile với camera UX

As a người dùng mobile,
I want chụp giấy khám với guide frame,
So that ảnh đủ rõ cho OCR.

**Acceptance Criteria:**

**Given** người dùng mở camera capture
**When** căn giấy vào khung và chụp ảnh
**Then** hệ thống hiển thị preview với lựa chọn "Dùng ảnh này" hoặc "Chụp lại"
**And** hỗ trợ overlay căn giấy và edge highlight theo UX-DR3.

**Original:** Story 3.2

### Story 9.2: Offline read-only cho lịch sử trên mobile

As a người dùng mobile,
I want xem dữ liệu lịch sử khi không có mạng,
So that tôi vẫn tra cứu thông tin đã đồng bộ trước đó.

**Acceptance Criteria:**

**Given** app đã cache dữ liệu trên thiết bị
**When** thiết bị mất kết nối mạng
**Then** timeline và chi tiết vẫn xem được ở chế độ read-only
**And** UI hiển thị trạng thái offline + thời điểm sync cuối.

**Original:** Story 5.4

### Story 9.3: Tự động đồng bộ khi kết nối được khôi phục

As a người dùng,
I want dữ liệu tự sync lại khi có mạng,
So that lịch sử luôn cập nhật mà không cần thao tác thủ công.

**Acceptance Criteria:**

**Given** app đang offline rồi chuyển online
**When** mạng ổn định trở lại
**Then** app tự trigger đồng bộ dữ liệu nền
**And** hiển thị trạng thái đồng bộ thành công hoặc lỗi retry rõ ràng.

**Original:** Story 5.5
```

### 4.5 Update FR Coverage Map

**File:** `_bmad-output/planning-artifacts/epics.md`
**Section:** FR Coverage Map

```diff
FR11: Epic 3 - Chụp ảnh bằng camera mobile
++ FR11: Epic 9 - Chụp ảnh bằng camera mobile (Phase 2)

FR27: Epic 5 - Mobile offline read-only lịch sử
++ FR27: Epic 9 - Mobile offline read-only lịch sử (Phase 2)

FR28: Epic 5 - Tự động đồng bộ khi có mạng
++ FR28: Epic 9 - Tự động đồng bộ khi có mạng (Phase 2)
```

### 4.6 Update Epic List Summary

**File:** `_bmad-output/planning-artifacts/epics.md`
**Section:** Epic List

```diff
## Epic List

++ ### Phase 1 - Web MVP (Epic 1-8)

### Epic 1: Truy Cập An Toàn và Quyền Riêng Tư Dữ Liệu
...

### Epic 8: Dashboard Vận Hành và Chỉ Số Sử Dụng
...

++ ### Phase 2 - Mobile Enhancement (Epic 9)

++ ### Epic 9: Nâng Cao Trải Nghiệm Mobile
++ Người dùng mobile có thể chụp ảnh trực tiếp, xem dữ liệu offline và tự động đồng bộ khi có mạng.
++ **FRs covered:** FR11, FR27, FR28.
```

---

## 5. Implementation Handoff

### Change Scope Classification: **MODERATE**

Requires backlog reorganization and PO/SM coordination.

### Handoff Recipients

| Role | Responsibility |
|------|----------------|
| **Scrum Master (Bob)** | Update epics.md, run sprint planning |
| **Product Manager (John)** | Approve PRD update, validate scope |
| **Development Team** | No action until sprint planning complete |

### Action Items

| # | Action | Owner | Priority |
|---|--------|-------|----------|
| 1 | Apply PRD update (Section 4.1) | SM | High |
| 2 | Apply Epic 3 update (Section 4.2) | SM | High |
| 3 | Apply Epic 5 update (Section 4.3) | SM | High |
| 4 | Add Epic 9 (Section 4.4) | SM | High |
| 5 | Update FR Coverage Map (Section 4.5) | SM | High |
| 6 | Update Epic List (Section 4.6) | SM | High |
| 7 | Move story files to epic-9 folder | SM | Medium |
| 8 | Run `bmad-bmm-sprint-planning` | SM | High |
| 9 | Regenerate implementation readiness report | Architect | Low |

### Success Criteria

- [ ] PRD P1 scope updated with mobile defer note
- [ ] Epic 3 has 5 stories (3.2 removed)
- [ ] Epic 5 has 3 stories (5.4, 5.5 removed)
- [ ] Epic 9 exists with 3 stories
- [ ] FR Coverage Map updated
- [ ] Sprint plan generated for Web MVP
- [ ] All 38 FRs still covered (35 in Phase 1, 3 in Phase 2)

---

## 6. Approval

### Approval Status: APPROVED (2026-03-28)

**Requested Changes:**
- Web-first MVP with full features
- Mobile stories separated into Epic 9 (Phase 2)
- Stitch UI screens to be linked in stories during sprint planning

**Approver:** ie303
**Date:** 2026-03-28

---

_Generated by BMad Correct Course Workflow_
_Assessor: Scrum Master Agent_
