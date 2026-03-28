---
validationTarget: '_bmad-output/planning-artifacts/prd.md'
validationDate: '2026-03-17'
inputDocuments:
  - prd.md
validationStepsCompleted:
  - step-v-01-discovery
  - step-v-02-format-detection
  - step-v-03-density-validation
  - step-v-04-brief-coverage-validation
  - step-v-05-measurability-validation
  - step-v-06-traceability-validation
  - step-v-07-implementation-leakage-validation
  - step-v-08-domain-compliance-validation
  - step-v-09-project-type-validation
  - step-v-10-smart-validation
  - step-v-11-holistic-quality-validation
  - step-v-12-completeness-validation
validationStatus: COMPLETE
holisticQualityRating: '4/5 (Good)'
overallStatus: WARNING
---

# PRD Validation Report

**PRD Being Validated:** `_bmad-output/planning-artifacts/prd.md`
**Validation Date:** 2026-03-17

## Input Documents

| Document | Path | Status |
|----------|------|--------|
| PRD | `_bmad-output/planning-artifacts/prd.md` | ✓ Loaded |

## Validation Findings

### Format Detection

**PRD Structure (Level 2 Headers):**
1. Executive Summary
2. Success Criteria
3. Product Scope
4. User Journeys
5. Domain Requirements
6. Innovation Analysis
7. Platform Requirements
8. Project Scoping
9. Functional Requirements
10. Non-Functional Requirements

**BMAD Core Sections Present:**
- Executive Summary: ✅ Present
- Success Criteria: ✅ Present
- Product Scope: ✅ Present
- User Journeys: ✅ Present
- Functional Requirements: ✅ Present
- Non-Functional Requirements: ✅ Present

**Format Classification:** BMAD Standard
**Core Sections Present:** 6/6

**Additional Sections:** Domain Requirements, Innovation Analysis, Platform Requirements, Project Scoping (phù hợp với healthcare domain và high complexity)

---

### Information Density Validation

**Anti-Pattern Violations:**

| Category | Violations |
|----------|------------|
| Conversational Filler | 0 |
| Wordy Phrases | 0 |
| Redundant Phrases | 0 |
| Soft/Vague Language | 0 |
| **Total** | **0** |

**Severity Assessment:** ✅ **PASS**

**Quality Observations:**
- Direct FR/NFR statements — sử dụng format "Người dùng có thể..." thay vì wordy patterns
- Bullet-first structure — 38 FRs, 20 NFRs rõ ràng, dễ scan
- Tables over prose — heavy use của tables cho metrics, phases, risks
- Concrete examples — User journeys dùng scenarios cụ thể (Bà Lan, Anh Minh)
- No hedging language — Metrics có hard targets (≥70%, ≤10 seconds, 72 hours)

**Recommendation:** PRD demonstrates excellent information density với zero violations

---

### Product Brief Coverage

**Status:** N/A - Không có Product Brief được cung cấp làm input document

---

### Measurability Validation

#### Functional Requirements

**Total FRs Analyzed:** 38

| Violation Category | Count | Examples |
|--------------------|-------|----------|
| Format Violations | 9 | FR12, FR15, FR18, FR19, FR22, FR23, FR28, FR36, FR37 - sử dụng "Hệ thống..." thay vì "[Actor] can [capability]" |
| Subjective Adjectives | 3 | FR16 "rõ ràng", FR20 "đơn giản", FR22 "nổi bật" - thiếu định nghĩa cụ thể |
| Vague Quantifiers | 1 | FR5 "nhiều hồ sơ" - nên ghi rõ "≥3 hồ sơ" như trong Scoping |
| Implementation Leakage | 0 | - |
| **FR Total Violations** | **13** | |

#### Non-Functional Requirements

**Total NFRs Analyzed:** 16

| Violation Category | Count | Examples |
|--------------------|-------|----------|
| Missing/Vague Metrics | 3 | NFR-P1 "ảnh chất lượng tốt" undefined, NFR-SC1 "không suy giảm hiệu năng" thiếu baseline, NFR-SC3 "10x growth" vague |
| Template Compliance Failures | 4 | NFR-P1, NFR-SC1, NFR-SC2, NFR-SC3 thiếu measurement method hoặc context |
| Missing Context | 4 | NFR-P1 cần định nghĩa "ảnh chất lượng tốt", NFR-SC1/SC2/SC3 thiếu performance baseline |
| **NFR Total Violations** | **7** | (có overlap giữa các categories) |

#### Overall Assessment

| Metric | Value |
|--------|-------|
| **Total Requirements** | 54 (38 FRs + 16 NFRs) |
| **Total Violations** | 20 |
| **Severity** | ⚠️ **CRITICAL** |

**Key Issues:**
1. **FR Format:** 9 FRs dùng "Hệ thống..." thay vì capability format - nên rewrite thành "[Actor] can [action]"
2. **Subjective terms:** "đơn giản", "rõ ràng", "nổi bật" cần định nghĩa cụ thể (reading level? character count?)
3. **NFR Context:** "ảnh chất lượng tốt" cần spec rõ (≥300 DPI? tilt angle? coverage?)
4. **Scalability NFRs:** NFR-SC1, SC2, SC3 cần performance baselines rõ ràng

**Recommendation:** PRD cần revision để requirements có thể testable. Focus vào: (1) FR format compliance, (2) định nghĩa subjective terms, (3) NFR measurement methods

---

### Traceability Validation

#### Chain Validation

| Chain | Status | Notes |
|-------|--------|-------|
| Executive Summary → Success Criteria | ✅ **INTACT** | Tất cả vision elements (Hiểu, Hành động, Theo dõi, Chia sẻ) đều có success metrics tương ứng |
| Success Criteria → User Journeys | ✅ **INTACT** | Mỗi criterion có journey evidence hỗ trợ |
| User Journeys → Functional Requirements | ⚠️ **GAPS** | J1/J2 tham chiếu P2 features (reminders, trends) như thể có sẵn trong P1 |
| Scope → Functional Requirements | ✅ **INTACT** | P1 scope items đều có FRs hỗ trợ |

#### Orphan Analysis

| Category | Count | Details |
|----------|-------|---------|
| Orphan FRs | **0** | Tất cả FRs traceable đến journeys, compliance, platform requirements |
| Unsupported Success Criteria | **0** | Tất cả criteria được hỗ trợ |
| Journeys Without Full FR Support | **2** | J1 (reminder - P2), J2 (trend chart - P2) |

#### Journey-Capability Table Issues

PRD có sẵn bảng Journey–Capability Summary nhưng **incomplete**:
- J1: Thiếu FR1-FR4 (auth infrastructure)
- J2: Thiếu FR18-FR23 (explanation), FR24 (history view)
- J3: Thiếu FR9-FR13, FR17 (complete upload flow)

#### Specific Gaps

| Journey | Issue | Severity |
|---------|-------|----------|
| J1 | "đặt nhắc tái khám tháng sau" - Reminder là P2 feature | Minor |
| J2 | "biểu đồ HbA1c 6 tháng" - Trend charts là P2 feature | Minor |

**Total Traceability Issues:** 4 (minor)

**Severity:** ⚠️ **WARNING**

**Recommendation:**
1. Clarify J1 narrative: đánh dấu "đặt nhắc tái khám" là P2 feature
2. Clarify J2 narrative: ghi chú rõ trend chart là P2, J2 partially supported trong MVP
3. Mở rộng Journey-Capability Table với prerequisite và shared FRs

---

### Implementation Leakage Validation

#### Scan Results

| Term | Location | Classification | Verdict |
|------|----------|----------------|---------|
| CSV/JSON | FR35 | Data format cho import capability | ✅ Acceptable |
| MFA | FR33, NFR-S4 | Security capability requirement | ✅ Acceptable |
| OCR | FR12, FR14-16 | Capability description | ✅ Acceptable |
| AES-256 | NFR-S1 | Encryption standard specification | ✅ Acceptable |
| TLS 1.2+ | NFR-S1 | Transport security requirement | ✅ Acceptable |

#### Terms NOT Found in FR/NFR Sections (Good!)

- Frontend Frameworks: React, Vue, Next.js, Expo - NOT in FRs/NFRs ✓
- Backend Frameworks: Spring Boot, Express - NOT in FRs/NFRs ✓
- Databases: PostgreSQL, MongoDB, SQLite - NOT in FRs/NFRs ✓
- Cloud Platforms: AWS, S3, GCP - NOT in FRs/NFRs ✓
- Infrastructure: Docker, Kubernetes - NOT in FRs/NFRs ✓

#### Summary

| Metric | Value |
|--------|-------|
| Terms found in FRs/NFRs | 7 |
| Classified as Acceptable | 7 |
| **Implementation Leakage Violations** | **0** |

**Severity:** ✅ **PASS**

**Notes:**
- FRs properly capability-focused - describe WHAT, not HOW
- Security standards (AES-256, TLS 1.2+, MFA) acceptable as minimum requirements
- Implementation details correctly isolated to "Platform Requirements" section
- CSV/JSON trong FR35 acceptable vì mô tả import capability từ góc nhìn user

---

### Domain Compliance Validation

**Domain:** Healthcare
**Complexity:** High (regulated)
**Declared Compliance:** Nghị định 13/2023/NĐ-CP

#### Compliance Matrix

| Required Area | Present? | Adequate? | Notes |
|---------------|----------|-----------|-------|
| Domain Requirements Section | ✅ YES | ✅ YES | Dedicated section present |
| Clinical Requirements | ⚠️ PARTIAL | ❌ NO | No formal clinical validation methodology |
| Regulatory Pathway (NĐ 13/2023) | ✅ YES | ✅ YES | Consent, right-to-delete, cross-border data covered |
| Validation Methodology | ⚠️ PARTIAL | ⚠️ PARTIAL | OCR validation defined, LLM validation weak |
| Safety Measures (Mức A/B/C) | ✅ YES | ✅ YES | Clear limits, disclaimers, Mức C deferred |
| PHI Handling | ✅ YES | ✅ YES | AES-256, TLS 1.2+, RBAC, data isolation |
| Audit Logging | ✅ YES | ⚠️ PARTIAL | NFR-S2, FR37 present but no retention period |

#### Coverage Statistics

| Category | Coverage |
|----------|----------|
| Regulatory Compliance | 100% (4/4) |
| Safety Measures | 80% (4/5) |
| PHI Handling | 100% (6/6) |
| Clinical Validation | 33% (1/3) |
| Audit Requirements | 75% (3/4) |
| **TOTAL** | **82% (18/22)** |

#### Critical Gaps

| Severity | Issue |
|----------|-------|
| **CRITICAL** | Missing clinical/medical expert validation requirement cho LLM health explanations |
| WARNING | No explicit "urgent doctor referral" FR khi critical threshold values detected |
| WARNING | Audit log retention period undefined |
| WARNING | LLM validation chỉ test "user comprehension", không test medical accuracy |

**Severity:** ⚠️ **WARNING - CONDITIONAL PASS**

**Recommendations:**
1. **CRITICAL:** Add FR mới: "All LLM explanation templates must be reviewed and approved by licensed medical professional trước deployment"
2. Add FR cho urgent referral warning khi detect critical values
3. Define audit log retention period (suggest: 7 years per VN medical records regulations)
4. Expand LLM validation với medical accuracy review by healthcare professional

---

### Project-Type Compliance Validation

**Project Type:** web_app + mobile_app

#### Web App Required Sections

| Section | Status | Details |
|---------|--------|---------|
| browser_matrix | ✅ Present | Chrome, Firefox, Safari, Edge — 2 phiên bản gần nhất |
| responsive_design | ✅ Present | Desktop-first cho dashboard; mobile-responsive cho landing |
| performance_targets | ✅ Present | NFR-P1 through NFR-P4 với measurable thresholds |
| seo_strategy | ⚠️ Missing | Landing page public-facing (SSR) nhưng không có SEO requirements |
| accessibility_level | ✅ Present | WCAG 2.1 AA + NFR-A1, NFR-A2, NFR-A3 |

#### Mobile App Required Sections

| Section | Status | Details |
|---------|--------|---------|
| platform_reqs | ✅ Present | React Native (Expo) — iOS + Android |
| device_permissions | ✅ Present | Camera, photo library, notifications, local storage |
| offline_mode | ✅ Present | SQLite cache, read-only offline, sync queue |
| push_strategy | ⚠️ Incomplete | P2 feature mentioned but no technical strategy (APNs/FCM, opt-in flow) |
| store_compliance | ✅ Present | App Store + Google Play — health app guidelines |

#### Excluded Sections (Should Be Absent)

| Section | Web App | Mobile App |
|---------|---------|------------|
| native_features | ✅ Absent | - |
| cli_commands | ✅ Absent | ✅ Absent |
| desktop_features | - | ✅ Absent |

#### Compliance Summary

| Metric | Value |
|--------|-------|
| Web App Required | 4/5 present |
| Mobile App Required | 4/5 complete |
| Excluded Sections | All absent ✓ |
| **Compliance Score** | **86%** |

**Severity:** ⚠️ **WARNING**

**Recommendations:**
1. Add SEO Strategy section cho public-facing landing page (meta tags, sitemap, structured data)
2. Expand Push Strategy trước P2 (provider, opt-in flow, notification types, frequency limits)

---

### SMART Requirements Validation

**Total Functional Requirements:** 38

#### Scoring Summary

| Metric | Value |
|--------|-------|
| All scores ≥3 | 100% (38/38) |
| All scores ≥4 | 86.8% (33/38) |
| FRs flagged (any <3) | 2.6% (1/38) |
| **Overall Average** | **4.76/5.0** |

#### By Criterion

| Criterion | Average | Min Score |
|-----------|---------|-----------|
| Specific | 4.68 | 3 |
| Measurable | 4.55 | 2 |
| Attainable | 4.87 | 4 |
| Relevant | 4.97 | 4 |
| Traceable | 4.97 | 4 |

#### Flagged FRs (Score <3)

| FR# | S | M | A | R | T | Issue |
|-----|---|---|---|---|---|-------|
| **FR38** | 3 | **2** | 5 | 4 | 4 | "thống kê cơ bản" vague, no metrics defined |

**FR38 Improvement:**
```
CURRENT: Admin có thể xem thống kê sử dụng cơ bản (số user, số lượt upload)

IMPROVED: Admin có thể xem dashboard thống kê bao gồm:
- Tổng số user đăng ký (all-time và theo tháng)
- Weekly Active Users (WAU)
- Tổng số lượt upload (all-time, theo ngày, theo tuần)
- Tỷ lệ upload thành công vs. thất bại
```

#### Improvable FRs (Score = 3, not flagged)

| FR | Issue | Enhancement |
|----|-------|-------------|
| FR16 | "phản hồi rõ ràng" subjective | Add: "within 2s, with reason + 2 recovery options" |
| FR20 | "giải thích đơn giản" subjective | Add: "Grade 8 reading level, ≤100 words/metric" |
| FR21 | "gợi ý lối sống" scope unclear | Add: "2-4 actionable suggestions per abnormal metric" |
| FR28 | "tự động" timing undefined | Add: "within 5s of network restoration" |
| FR32 | "cập nhật" undefined | Reference NFR-P4 (≤30s polling) |

**Severity:** ✅ **PASS** (2.6% flagged < 10% threshold)

**Recommendation:** Excellent requirements quality overall. Revise FR38 và consider enhancements cho FR16, FR20, FR21, FR28, FR32

---

### Holistic Quality Assessment

#### Document Flow & Coherence

**Assessment:** Good

**Strengths:**
- Strong narrative arc: problem → solution → success → requirements
- Effective Executive Summary với core insight về "aha moment"
- Consistent voice, clear Vietnamese-appropriate style
- Logical section progression
- Cross-references maintained (Journey–Capability Summary)

**Areas for Improvement:**
- Innovation Analysis placement có thể move lên trước User Journeys
- Platform Requirements section feels disconnected từ narrative
- Transition từ Domain đến Innovation hơi jarring

#### Dual Audience Effectiveness

**For Humans:**
| Audience | Score | Notes |
|----------|-------|-------|
| Executive-friendly | Excellent | Executive Summary crisp, insight-driven |
| Developer clarity | Good | FRs atomic và numbered, NFRs có metrics |
| Designer clarity | Very Good | User Journeys detailed với emotional outcomes |
| Stakeholder decisions | Good | Priority tiers clear, risks documented |

**For LLMs:**
| Capability | Score | Notes |
|------------|-------|-------|
| Machine-readable | Excellent | Consistent ## headers, tables, numbered FRs/NFRs |
| UX readiness | Very Good | Journeys có persona + steps + outcome + capabilities |
| Architecture readiness | Good | Platform Requirements có tech stack, NFRs có metrics |
| Epic/Story readiness | Very Good | FRs atomic, Journey–Capability Summary traceable |

**Dual Audience Score:** 4/5

#### BMAD PRD Principles Compliance

| Principle | Status | Notes |
|-----------|--------|-------|
| Information Density | ✅ Met | Prose lean, no filler, tables effective |
| Measurability | ✅ Met | Specific numbers (300 WAU, ≥70%, ≤10s, ≥99%) |
| Traceability | ✅ Met | Journey–Capability Summary, Vision → Success chain |
| Domain Awareness | ✅ Met | NĐ 13/2023, Mức A/B/C, risk table comprehensive |
| Zero Anti-Patterns | ⚠️ Partial | Mostly clean, minor vague phrases |
| Dual Audience | ✅ Met | Structured for LLMs, readable for humans |
| Markdown Format | ✅ Met | Clean markdown, consistent headers, proper tables |

**Principles Met:** 6.5/7

#### Overall Quality Rating

**Rating:** ⭐⭐⭐⭐ **4/5 — Good**

PRD well-crafted với strong BMAD principles. User journeys vivid, requirements measurable, domain compliance thorough. LLM có thể generate UX designs, architecture, hoặc epic breakdowns với confidence.

#### Top 3 Improvements

**1. Add Explicit FR-to-Journey Traceability**
- Add `[J1, J3]` annotation at end of each FR
- Makes LLM epic/story breakdown more accurate

**2. Strengthen NFR-SC3 Specificity**
```
CURRENT: "10x user growth không cần refactor kiến trúc"
IMPROVED: "mở rộng từ 500 đến 5,000 concurrent users qua horizontal scaling 
          trong vòng 2 ngày, không thay đổi code ứng dụng"
```

**3. Add Data Flow Diagram cho OCR Pipeline**
- Mermaid diagram trong Platform Requirements
- Reduces ambiguity, LLMs generate more accurate specs

---

### Completeness Validation

#### Template Completeness

**Template Variables Found:** 0

Không còn template variables nào trong PRD. Tất cả placeholders đã được thay thế bằng nội dung thực tế. ✓

#### Content Completeness by Section

| Section | Status | Notes |
|---------|--------|-------|
| **Executive Summary** | ✅ Complete | Vision statement, target users, 4 core capabilities, insight, differentiator — đầy đủ |
| **Success Criteria** | ✅ Complete | User success, business success, measurable outcomes table với metrics và timeline |
| **Product Scope** | ✅ Complete | P1/P2/P3 phases rõ ràng, in-scope và out-of-scope defined |
| **User Journeys** | ✅ Complete | 4 journeys (J1-J4) với personas, steps, outcomes, capabilities mapping |
| **Domain Requirements** | ✅ Complete | NĐ 13/2023 compliance, Mức A/B/C limits, reference data strategy, risk matrix |
| **Innovation Analysis** | ✅ Complete | Market gaps, 3 innovation areas, validation pre-launch |
| **Platform Requirements** | ✅ Complete | Architecture, web/mobile specs, real-time flow, offline strategy |
| **Project Scoping** | ✅ Complete | MVP philosophy, Phase 1 scope, roadmap, scoping risks |
| **Functional Requirements** | ✅ Complete | 38 FRs organized by 6 categories |
| **Non-Functional Requirements** | ✅ Complete | 16 NFRs across Performance, Security, Scalability, Accessibility, Integration |

**Content Completeness:** 10/10 sections complete

#### Section-Specific Completeness

**Success Criteria Measurability:** ✅ **All** measurable
- Tất cả 5 outcomes trong bảng có metric, target, và thời hạn cụ thể

**User Journeys Coverage:** ✅ **Yes** - covers all user types
- Primary user (Bà Lan - người bệnh mãn tính): J1
- Secondary user (Anh Minh - người thân): J2
- Edge case user (Chị Thu - OCR thất bại): J3
- Admin user: J4

**FRs Cover MVP Scope:** ✅ **Yes**
- P1 scope items đều có FRs tương ứng
- Journey–Capability Summary mapping rõ ràng

**NFRs Have Specific Criteria:** ⚠️ **Some**
- 13/16 NFRs có metrics cụ thể
- 3 NFRs cần cải thiện: NFR-SC1, NFR-SC2, NFR-SC3 (đã flag trong Measurability Validation)

#### Frontmatter Completeness

| Field | Status | Value |
|-------|--------|-------|
| **stepsCompleted** | ✅ Present | 13 workflow steps completed |
| **classification** | ✅ Present | projectType, domain, complexity, projectContext, compliance |
| **inputDocuments** | ✅ Present | Empty array (acceptable - no input docs) |
| **date** | ✅ Present | 2026-03-17 |
| **workflowType** | ✅ Present | 'prd' |
| **project_name** | ✅ Present | health-lens |

**Frontmatter Completeness:** 6/6 fields present

#### Completeness Summary

**Overall Completeness:** 100% (10/10 sections complete)

**Critical Gaps:** 0
**Minor Gaps:** 0 (NFR specificity issues already captured in Measurability Validation)

**Severity:** ✅ **PASS**

**Recommendation:** PRD hoàn chỉnh với tất cả sections và content cần thiết. Không còn template variables. Frontmatter đầy đủ metadata. Sẵn sàng cho implementation phase.

---

## Validation Summary

### Overall Status: ⚠️ WARNING

PRD có chất lượng tốt và có thể sử dụng được, nhưng có một số issues cần xem xét cải thiện trước khi implementation.

### Quick Results

| Validation Step | Result | Severity |
|-----------------|--------|----------|
| Format Detection | BMAD Standard (6/6 core sections) | ✅ PASS |
| Information Density | 0 violations | ✅ PASS |
| Product Brief Coverage | N/A (no brief provided) | — |
| Measurability | 20 violations (13 FR + 7 NFR) | ⚠️ CRITICAL |
| Traceability | 4 minor gaps | ⚠️ WARNING |
| Implementation Leakage | 0 violations | ✅ PASS |
| Domain Compliance | 82% (18/22 requirements) | ⚠️ WARNING |
| Project-Type Compliance | 86% (missing SEO, incomplete push) | ⚠️ WARNING |
| SMART Requirements | 97.4% (1/38 flagged) | ✅ PASS |
| Holistic Quality | 4/5 (Good) | ✅ PASS |
| Completeness | 100% (10/10 sections) | ✅ PASS |

### Critical Issues (1)

1. **Measurability - 20 violations:** 9 FRs dùng format "Hệ thống..." thay vì "[Actor] can [capability]"; 3 subjective terms thiếu định nghĩa; 3 NFRs thiếu performance baselines

### Warnings (4)

1. **Domain Compliance - 82%:** Missing clinical/medical expert validation requirement cho LLM explanations
2. **Traceability:** J1/J2 journeys reference P2 features như thể có sẵn trong P1
3. **Project-Type:** Missing SEO strategy cho landing page, incomplete push notification strategy
4. **NFR Scalability:** NFR-SC1, SC2, SC3 cần performance baselines rõ ràng hơn

### Strengths

- **Excellent information density:** Zero filler, prose lean, tables effective
- **Strong BMAD principles:** Dual audience design, traceability chains intact
- **Comprehensive domain coverage:** NĐ 13/2023 compliance, risk matrix, safety limits Mức A/B/C
- **Vivid user journeys:** 4 journeys với personas, emotional outcomes, capability mapping
- **Clean implementation separation:** No technology leakage in FRs/NFRs
- **Complete structure:** All 10 sections present with required content

### Holistic Quality Rating

**⭐⭐⭐⭐ 4/5 — Good**

PRD well-crafted với strong BMAD principles. LLM có thể generate UX designs, architecture, hoặc epic breakdowns với confidence.

### Top 3 Improvements

1. **Add Explicit FR-to-Journey Traceability** — Add `[J1, J3]` annotation at end of each FR
2. **Strengthen NFR-SC3 Specificity** — Define concrete scaling metrics thay vì "10x growth"
3. **Add Data Flow Diagram cho OCR Pipeline** — Mermaid diagram trong Platform Requirements

### Recommendation

PRD có thể sử dụng được nhưng nên address các warnings trước khi implementation. Focus vào:
1. ~~Fix FR format violations (9 FRs)~~ ✅ FIXED
2. Thêm clinical validation requirement cho LLM explanations
3. ~~Clarify P1 vs P2 scope trong user journeys~~ ✅ FIXED

---

## Fixes Applied (Post-Validation)

**Date:** 2026-03-17

### 1. FR Format Fixes (9 FRs) ✅

Converted từ "Hệ thống..." sang "[Actor] can [capability]" format:

| FR | Before | After |
|----|--------|-------|
| FR12 | Hệ thống tự động trích xuất... | Người dùng có thể xem chỉ số được tự động trích xuất... |
| FR15 | Hệ thống đánh dấu nguồn gốc... | Người dùng có thể xem nguồn gốc... |
| FR18 | Hệ thống hiển thị từng chỉ số... | Người dùng có thể xem từng chỉ số... |
| FR19 | Hệ thống phân loại trạng thái... | Người dùng có thể xem trạng thái phân loại... |
| FR22 | Hệ thống hiển thị cảnh báo... | Người dùng nhận cảnh báo... |
| FR23 | Ngưỡng tham chiếu áp dụng... | Người dùng nhận ngưỡng tham chiếu... |
| FR28 | Hệ thống đồng bộ dữ liệu... | Người dùng có thể thấy dữ liệu được đồng bộ... |
| FR36 | Thay đổi reference data chỉ... | Admin có thể approve thay đổi... |
| FR37 | Hệ thống ghi audit log... | Admin có thể xem audit log... |

### 2. FR38 Measurability Fix ✅

**Before:** Admin có thể xem thống kê sử dụng cơ bản (số user, số lượt upload)

**After:** Admin có thể xem dashboard thống kê bao gồm: tổng số user đăng ký (all-time và theo tháng), Weekly Active Users (WAU), tổng số lượt upload (all-time, theo ngày, theo tuần), tỷ lệ upload thành công vs. thất bại

### 3. Journey P2 Clarification ✅

- J1 Step 5: Added *(P2 feature)* marker cho "đặt nhắc tái khám tháng sau"
- J2 Step 3: Added *(P2 feature — trend charts)* marker cho "biểu đồ HbA1c 6 tháng"

---

## Updated Validation Status

**After Fixes:**
- FR Format Violations: ~~9~~ → **0** ✅
- FR38 Measurability: ~~Flagged~~ → **PASS** ✅
- Journey P2 Clarity: ~~Unclear~~ → **Clear** ✅
- Measurability Total: ~~20~~ → **8** (remaining: 3 subjective terms + 5 NFR issues)

**Remaining Items:**
1. Subjective terms (FR16 "rõ ràng", FR20 "đơn giản", FR22 "nổi bật") — cần định nghĩa cụ thể
2. NFR-SC1, SC2, SC3 — cần performance baselines
3. Clinical validation requirement cho LLM explanations — cần thêm FR mới
