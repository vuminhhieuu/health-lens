# 🚀 HealthLens — Integrated Master Implementation Plan v2

> **Ngày**: 2026-06-01
> **Plan tích hợp 7 review** thành lộ trình duy nhất. Bổ sung từ runtime security audit.
> **Replaces**: `MASTER_IMPLEMENTATION_PLAN.md` (v1).

---

## 🎯 Nguyên tắc

1. **Runtime safety FIRST** — prompt injection, multi-pod duplication, security headers TRƯỚC refactor lớn.
2. **Verify trước fix** — mọi finding đã đối chiếu code tại commit `2026-06-01`.
3. **PR < 500 LoC** — god service tách dần qua nhiều PR.
4. **Tooling trước refactor lớn** — format/lint setup TRƯỚC khi đụng god classes.
5. **Mỗi phase ship được riêng lẻ**.

---

## 📊 11 Phase Overview (~173h, 13–14 tuần)

| Phase | Tên | Tuần | Effort | Phụ thuộc |
|:-:|------|:-:|:-:|----------|
| **0** | Critical Safety Net | 1 | 14h | — |
| **1** | Runtime Hardening | 2 | 10h | Phase 0 |
| **2** | Tooling & Conventions | 3 | 10h | — |
| **3** | Shared Foundation | 4 | 12h | Phase 2 |
| **4** | Domain Exceptions & Error Codes | 5 | 10h | Phase 3 |
| **5** | Backend Package-by-Feature | 6–8 | 40h | Phase 3, 4 |
| **6** | Frontend + Validation Wiring | 9–10 | 38h | Phase 3 |
| **7** | Mobile + OCR Service | 11 | 14h | — |
| **8** | Observability + CI/CD | 12 | 14h | Phase 2 |
| **9** | Docs + BMAD Cleanup | 13 | 10h | Phase 5 |
| **10** | AI Tooling | parallel | 1h | — |

---

## 🔴 PHASE 0 — Critical Safety Net (Tuần 1, 14h)

### 0.A — Runtime Security Critical (3h)

| # | Action | Effort | File |
|:-:|--------|:-:|------|
| 0.A.1 | HTTP security headers (CSP, HSTS, X-Frame-Options...) | 30m | `SecurityConfig.java:63–107` |
| 0.A.2 | Soft delete: `@SQLDelete` + `@Where` | 30m | `HealthRecord.java:77–78` |
| 0.A.3 | Redis cache purge trong DataDeletionService | 1h | `DataDeletionService.java:303` |
| 0.A.4 | Side effect khỏi `@Transactional(readOnly)` | 30m | `FollowUpReminderService.java:69` |
| 0.A.5 | OcrController typed DTO + `@Valid` | 15m | `OcrController.java:51` |

### 0.B — Licensing, Stubs, Config (4.5h)

| # | Action | Effort |
|:-:|--------|:-:|
| 0.B.1 | Thay `Arial.ttf` → DejaVu/Noto Sans | 1h |
| 0.B.2 | Fix `AwsTextractClient` stub HOẶC remove khỏi registry | 2h |
| 0.B.3 | Verify `DevController` profile gating (prod KHÔNG có `docker` profile) | 30m |
| 0.B.4 | Tăng Redis cache TTL 5s → 5min, bỏ presigned URL khỏi cache key | 30m |
| 0.B.5 | Hợp nhất file size constant (20/10/30MB → 1 giá trị) | 30m |

### 0.C — Cleanup (1.5h)

| # | Action |
|:-:|--------|
| 0.C.1 | Xóa `apps/web/apps/` + `apps/web/packages/` |
| 0.C.2 | Xóa 3 file barrel rỗng `apps/mobile/{components,hooks,stores}/index.ts` |
| 0.C.3 | Move `test_ssrf_protection.py` khỏi `__pycache__/` |
| 0.C.4 | Xóa 5 Next.js template SVGs |
| 0.C.5 | Xóa 6 expo template assets |
| 0.C.6 | Xóa `.env.{production,staging,staging.api}` (Infisical thay) |
| 0.C.7 | Verify `lucide-react@^1.7.0` version |
| 0.C.8 | Tag `pre-refactor-2026-06-01` |

**Decision Gate**: Smoke test staging — `curl -I` headers, upload flow, deletion flow.

---

## 🟠 PHASE 1 — Runtime Hardening (Tuần 2, 10h)

### 1.1 LLM Prompt Injection (4h)

| # | Action | Effort |
|:-:|--------|:-:|
| 1.1.1 | Tạo `PromptSanitizer` utility | 1h |
| 1.1.2 | Wrap user content bằng XML delimiter trong template files | 1h |
| 1.1.3 | Rename `safeMetric`/`safeValue` → `prepareMetricForPrompt`/`prepareValueForPrompt` + apply sanitize | 1h |
| 1.1.4 | Sanitize `ocrText` ở `OcrService.parseMetrics()` | 30m |
| 1.1.5 | Output token cap (800 tokens) | 30m |

### 1.2 @Scheduled ShedLock (2.5h)

| # | Action | Effort |
|:-:|--------|:-:|
| 1.2.1 | Add ShedLock deps (5.16.0) | 30m |
| 1.2.2 | Flyway V051 — shedlock table | 15m |
| 1.2.3 | Annotate `FollowUpReminderScheduler` | 15m |
| 1.2.4 | Annotate `DeletionScheduler` | 15m |
| 1.2.5 | `MetricExplanationIngestionJob` leader-election | 30m |
| 1.2.6 | Multi-pod integration test | 30m |

### 1.3 Magic-Byte MIME (3.5h)

| # | Action | Effort |
|:-:|--------|:-:|
| 1.3.1 | Add Apache Tika 2.9.2 | 15m |
| 1.3.2 | Tạo `FileTypeVerifier` | 1h |
| 1.3.3 | Wire vào `confirmUpload` | 1h |
| 1.3.4 | Mirror ở OCR service Python (`python-magic`) | 1h |

**Decision Gate**: Manual security test — prompt injection payload bị `[REDACTED]`, multi-pod scheduler chỉ fire 1 lần.

---

## 🟡 PHASE 2 — Tooling & Conventions (Tuần 3, 10h)

- 2.A Format & Lint (3h): `.editorconfig`, Prettier, Husky, Spotless, Ruff
- 2.B GitHub Conventions (1h): PR/Issue templates, CODEOWNERS, Dependabot
- 2.C Standard Files (1h): LICENSE, CONTRIBUTING, SECURITY, .nvmrc
- Format sweep + buffer (5h)

---

## 🟢 PHASE 3 — Shared Foundation (Tuần 4, 12h)

1. Tạo Java enum `HealthRecordStatus` + state machine — 3h
2. Sync với TS shared `health-record-status.ts` — 1h
3. Refactor 15+ magic strings → enum — 2h
4. Unify Gender (1 nguồn truth) — 1h
5. Unify Pagination constants — 30m
6. Extract `TtlConstants` — 1h
7. Sync `CONSENT_VERSION` từ API — 1h
8. Delete `routes.ts` wrapper — 1h
9. Reorganize `packages/shared/src/` layout — 1.5h

---

## 🟢 PHASE 4 — Domain Exceptions & Error Codes (Tuần 5, 10h)

1. Exception hierarchy (`HealthLensException`, `EntityNotFoundException`...) — 2h
2. Update `GlobalExceptionHandler` map exception → HttpStatus — 2h
3. Replace 30+ `IllegalArgumentException` — 3h
4. Wire `ErrorCode` end-to-end — 2h
5. i18n message bundle scaffold — 1h

---

## 🔵 PHASE 5 — Backend Package-by-Feature (Tuần 6–8, 40h)

### 5A — `healthrecord/` (14h)
Skeleton + move entities/repos/DTOs + **tách HealthRecordService 1,710→5 service** + MapStruct + soft-delete fix verify + tests.

### 5B — `ocr/` + `llm/` (14h)
Extract OCR inner DTOs + split OcrService → 3 service + move providers + `LlmProvider` interface + chain + tests.

### 5C — Other features (12h)
Migrate `auth/`, `user/`, `profile/`, `notification/`, `rag/`, `reference/`, `analytics/`, `admin/` + `StoragePort`/`EmailPort` + cleanup `common/`.

**Decision Gate**: Coverage ≥ Phase 4 baseline (allow -5%). Smoke E2E pass.

---

## 🟣 PHASE 6 — Frontend + Validation Wiring (Tuần 9–10, 38h)

1. Next.js conventions (loading/error/not-found) — 2h
2. PWA manifest — 30m
3. FSD-lite migration — 6h
4. **Split ReviewRecordPage 2,783→shell+components+hooks** — 10h
5. Split admin/reference-data pages — 6h
6. Split home + admin/login — 4h
7. Extract inline types — 1h
8. Domain hooks — 2h
9. Email templates (5 file Thymeleaf còn thiếu) — 4h
10. **Wire Zod schemas** vào API client + forms — 6h
11. **`/users/me/export`** endpoint — 4h

---

## 🟠 PHASE 7 — Mobile + OCR Service (Tuần 11, 14h)

### 7A Mobile (decision-gated): build full vs drop deps vs defer
### 7B OCR Service: modularize app.py → modules + tests/ folder + pytest CI + Trivy + CorrelationID + pyproject.toml

---

## 🔘 PHASE 8 — Observability + CI/CD (Tuần 12, 14h)

Micrometer metrics + Prometheus + Sentry + JaCoCo + Trivy + Lighthouse CI + staging-deploy workflow + OpenAPI codegen + **ApiResponse envelope** + **Data retention decision**.

---

## 🔘 PHASE 9 — Docs + BMAD (Tuần 13, 10h)

BMAD restructure (27 epic flat) + sprint-status.yaml + rename SCREAMING_SNAKE + merge duplicate docs + add 5 missing docs + update outdated.

---

## 🔘 PHASE 10 — AI Tooling Cleanup (1h, parallel)

Team audit → keep max 2 IDE export + cleanup `.opencode/node_modules` + audit `.github/skills/`.

---

## 🚦 Decision Gates

| Gate | Phase |
|------|:-:|
| Tests xanh | Mọi phase |
| Smoke security test | 0, 1 |
| Multi-pod scheduler test | 1.2 |
| Coverage không tụt > 5% | 5 |
| Smoke E2E pass | 5, 6 |
| Mobile build vs drop | 7A |
| Retention policy | 8.10 |

---

## 🎯 Quick-Win Path (~22h)

1. Phase 0 toàn bộ (14h)
2. Phase 1.1 + 1.2 (6.5h) — Prompt sanitizer + ShedLock
3. Phase 3.1–3.3 (6h overlap) — Status enum

→ Cover 8 critical issues với 13% effort của full plan.

---

## ✅ Definition of Done

### Runtime
- [ ] HTTP security headers trên mọi response
- [ ] LLM prompts wrap user content + PromptSanitizer apply
- [ ] @Scheduled jobs có @SchedulerLock
- [ ] HealthRecord có @SQLDelete + @Where (0 leak)
- [ ] Magic-byte MIME validation active
- [ ] DataDeletionService purge Redis + S3
- [ ] `/users/me/export` hoạt động
- [ ] 0 side effect trong @Transactional(readOnly)
- [ ] All controllers typed DTO + @Valid
- [ ] Zod schemas wired ≥ 80%

### Code Quality
- [ ] Coverage ≥ 70% trên feature refactor
- [ ] Lighthouse a11y ≥ 90
- [ ] Zero IllegalArgumentException cho business
- [ ] Zero magic status strings
- [ ] HealthRecordService ≤ 400 dòng
- [ ] ReviewRecordPage ≤ 100 dòng shell
- [ ] Sentry production
- [ ] /actuator/prometheus exposed
- [ ] _bmad-output/ BMAD v6 layout
- [ ] Zero Arial.ttf
- [ ] PWA installable

---

## 📚 7 Review Files

| # | File | Cover |
|:-:|------|-------|
| 1 | [project_review.md](project_review.md) | Hybrid structure, god classes, constants base |
| 2 | [deep_code_review.md](deep_code_review.md) | SOLID, decomposition, exception hierarchy |
| 3 | [folder_organization_solution.md](folder_organization_solution.md) | Package-by-feature + FSD + OCR modules |
| 4 | [review_gaps.md](review_gaps.md) | Feature maturity, DB, scripts, CI/CD, observability |
| 5 | [repo_hygiene_audit.md](repo_hygiene_audit.md) | docs/, BMAD, AI tooling, standard files |
| 6 | [final_misc_audit.md](final_misc_audit.md) | Style, GitHub conventions, assets, localization |
| 7 | [runtime_security_and_correctness_audit.md](runtime_security_and_correctness_audit.md) | **NEW** — Runtime correctness |
