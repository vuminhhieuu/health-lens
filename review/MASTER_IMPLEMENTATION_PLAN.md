# 🗺️ HealthLens — Master Implementation Plan

> **Ngày**: 2026-05-28
> **Mục đích**: Plan tổng hợp từ **6 file review** thành lộ trình thực hiện cụ thể, ưu tiên theo ROI và rủi ro.
> **Nguồn dữ liệu**: `project_review.md`, `deep_code_review.md`, `folder_organization_solution.md`, `review_gaps.md`, `repo_hygiene_audit.md`, `final_misc_audit.md`.

---

## 🎯 Nguyên tắc chiến lược

1. **Bug-fix & safety FIRST, refactor LATER.** Stubs trong prod (`AwsTextractClient`), licensing risk (Arial.ttf), cache TTL sai (5s) ưu tiên cao hơn package-by-feature refactor.
2. **Migrate theo feature, không big-bang.** `HealthRecordService` (1,710 dòng) tách dần qua nhiều PR — KHÔNG 1 PR đại tu.
3. **Tooling trước refactor lớn.** Setup Spotless/Prettier/Husky trước khi đụng vào god classes → format-only commits không lẫn vào diff thật.
4. **Mỗi phase ship được riêng lẻ.** Không phase nào block phase khác cứng — cho phép parallel khi possible.

---

## 📅 7-Phase Roadmap (12–14 tuần)

### 🔴 PHASE 0 — Safety Net (Tuần 1, ~12h)

**Mục tiêu**: Đóng các "kill-switch" risk + cleanup nhanh không cần kiến trúc.

| # | Action | Effort | Risk nếu skip | Source |
|---|--------|:-:|---------------|--------|
| 0.1 | Thay `Arial.ttf` (proprietary) → DejaVu Sans / Noto Sans | 1h | 🔴 Licensing lawsuit | review #6 §3.4 |
| 0.2 | Fix `AwsTextractClient` stub HOẶC remove khỏi provider registry | 2h | 🔴 Runtime fail nếu router pick | review_gaps §1.2 |
| 0.3 | Verify `DevController` profile gating — đảm bảo prod KHÔNG chạy `docker` profile | 30m | 🔴 Dev endpoint leak | review_gaps §1.2 |
| 0.4 | Tăng Redis cache TTL từ 5s → 5min, loại bỏ presigned URL khỏi cache key | 30m | 🟡 Waste cache | review #1 §4.2 |
| 0.5 | Fix file size constant mismatch (20MB / 10MB / 30MB) → 1 giá trị | 30m | 🟡 Inconsistent UX | review #1 §3.6 |
| 0.6 | Xóa `apps/web/apps/` (ghost) + `apps/web/packages/` (empty) | 5m | 🟢 Confusion | folder_org §3.1 |
| 0.7 | Xóa 3 file barrel rỗng `apps/mobile/{components,hooks,stores}/index.ts` | 5m | 🟢 Noise | folder_org §4.1 |
| 0.8 | Move `services/ocr-service/test_ssrf_protection.py` ra khỏi `__pycache__/` | 10m | 🟢 Test invisibility | folder_org §5.1 |
| 0.9 | Xóa 5 Next.js template SVGs trong `apps/web/public/` | 5m | 🟢 Clutter | review #6 §3.1 |
| 0.10 | Xóa 6 expo template assets trong `apps/mobile/assets/images/` | 10m | 🟢 Clutter | review #6 §3.3 |
| 0.11 | Xóa `.env.production`, `.env.staging`, `.env.staging.api` (Infisical thay) | 5m | 🟢 Sprawl | review #1 §1.5 |
| 0.12 | Verify `lucide-react@^1.7.0` version trong web (có thể typo `^0.x`) | 15m | 🟡 Install fail | review_gaps §11.2 |
| 0.13 | Backup full repo + tag `pre-refactor-2026-05-28` | 30m | 🔴 Rollback safety | — |

**Deliverable Phase 0**: 1 PR `chore: safety cleanup` + 1 git tag.

---

### 🟡 PHASE 1 — Tooling & Conventions (Tuần 2, ~10h)

**Mục tiêu**: Setup format + lint + hooks **trước khi** refactor lớn → tránh format-noise lẫn vào diff thật.

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 1.1 | Add `.editorconfig` (tab/space, line ending) | 5m | review #6 §1 |
| 1.2 | Add Prettier + `.prettierrc` + `.prettierignore` (web + mobile + shared) | 20m | review #6 §1 |
| 1.3 | Add Husky + lint-staged (pre-commit hook) | 30m | review #6 §1 |
| 1.4 | Add Spotless plugin trong `apps/api/build.gradle.kts` (google-java-format) | 30m | review #6 §1 |
| 1.5 | Add Ruff cho `services/ocr-service/pyproject.toml` | 15m | review #6 §1 |
| 1.6 | Run **format-all** một lần (chấp nhận diff lớn) — commit message rõ "chore: apply formatter" | 1h | — |
| 1.7 | Update CI: add `pnpm format:check` + `./gradlew spotlessCheck` + `ruff check` | 30m | review_gaps §4 |
| 1.8 | Add `.github/PULL_REQUEST_TEMPLATE.md` (checklist: tests, docs, migrations) | 15m | review #6 §2 |
| 1.9 | Add `.github/ISSUE_TEMPLATE/{bug_report,feature_request}.md` | 20m | review #6 §2 |
| 1.10 | Add `.github/CODEOWNERS` (DB migrations, security, billing) | 10m | review #6 §2 |
| 1.11 | Add `.github/dependabot.yml` (pnpm + gradle + pip + docker) | 15m | review #6 §2 |
| 1.12 | Add root standard files: `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`, `.nvmrc` | 1h | repo_hygiene §4 |

**Deliverable Phase 1**: 2 PRs — (1) `chore: add formatters + hooks`, (2) `chore: github conventions`.

---

### 🟢 PHASE 2 — Shared Foundation (Tuần 3, ~12h)

**Mục tiêu**: Single-source-of-truth cho enum, constants, status — **trước khi** refactor backend.

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 2.1 | Tạo Java enum `HealthRecordStatus { PROCESSING, REVIEW_REQUIRED, DONE, OCR_FAILED }` + state machine | 3h | review #2 §3 |
| 2.2 | Sync với `packages/shared/src/domain/health-record-status.ts` (lowercase DB values) | 1h | review #2 §6.2 |
| 2.3 | Refactor 15+ `"processing".equals(record.getStatus())` → enum comparison | 2h | review #1 §4.1 |
| 2.4 | Hợp nhất `Gender` thành `packages/shared/src/domain/gender.ts` (1 source) | 1h | review #1 §3.1 |
| 2.5 | Hợp nhất `Pagination` constants | 30m | review #1 §3.2 |
| 2.6 | Tạo `TtlConstants.java` + `ttl.ts` (Duration hardcoded → extract) | 1h | review_gaps §2.2(c) |
| 2.7 | Sync `CONSENT_VERSION` — API endpoint thay vì hardcode 2 nơi | 1h | review #1 §3.4 |
| 2.8 | Xóa `apps/web/src/lib/api/routes.ts` (re-export wrapper) | 1h | review #1 §3.5 |
| 2.9 | Reorganize `packages/shared/` theo target layout (folder_org §6.2) | 1.5h | folder_org §6 |

**Deliverable Phase 2**: 3 PRs — (1) enum + state machine, (2) shared deduplication, (3) shared reorganization.

---

### 🟢 PHASE 3 — Domain Exceptions & Error Codes (Tuần 4, ~10h)

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 3.1 | Tạo Java exception hierarchy: `HealthLensException`, `EntityNotFoundException`, `DuplicateEntityException`, `InvalidInputException`, `InvalidStateException`, `InfrastructureException` | 2h | review #2 §5.2 |
| 3.2 | Update `GlobalExceptionHandler` map exception → ApiErrorCode → HttpStatus | 2h | review #2 §5.2 |
| 3.3 | Replace 30+ `IllegalArgumentException` thành domain exception cụ thể | 3h | review #2 §5.1 |
| 3.4 | Wire `ErrorCode` system end-to-end (shared → Java → API response → web error handler) | 2h | review #1 §6.1 |
| 3.5 | Tạo i18n message bundle (Vietnamese) — extract 88 hardcoded strings | 1h (phase 1, không xong hết) | review #2 §1.1 |

**Deliverable Phase 3**: 2 PRs — (1) exception hierarchy + ErrorCode wiring, (2) i18n bundle scaffold.

---

### 🔵 PHASE 4 — Backend Package-by-Feature (Tuần 5–7, ~40h)

**Mục tiêu**: Migrate `apps/api` từ hybrid sang package-by-feature theo `folder_organization_solution.md`.

#### Phase 4A — High-leverage feature đầu tiên: `healthrecord/` (Tuần 5)

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 4A.1 | Tạo skeleton `healthrecord/{api,service,domain,repository,dto,mapper,exception}/` | 30m | folder_org §1.2 |
| 4A.2 | Move 4 entities + 4 repos vào `healthrecord/domain/` + `healthrecord/repository/` | 1h | folder_org §2.1 |
| 4A.3 | Move 16 DTOs vào `healthrecord/dto/{request,response}/` | 1h | folder_org §2.1 |
| 4A.4 | Tách `HealthRecordService` (1,710 dòng) → 5 service: `Upload`, `Query`, `Confirm`, `Export`, `AccessControl` | 8h | review #2 §1.1 |
| 4A.5 | Tạo `HealthRecordMapper` (MapStruct) | 2h | review #2 §4 |
| 4A.6 | Update tests theo structure mới, đảm bảo coverage không tụt | 3h | — |

#### Phase 4B — `ocr/` + `llm/` (Tuần 6) — Giải quyết circular dependency

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 4B.1 | Extract `OcrService` inner DTOs (`EasyOcrRequest`, `EasyOcrResponse`, `OcrExtractionResult`) ra file riêng | 1h | review #2 §1.5 |
| 4B.2 | Tách `OcrService` → `OcrPipelineService` + `OcrMetricExtractor` + `OpenRouterLlmClient` | 6h | review #2 §1.1 |
| 4B.3 | Move 4 OCR provider implementations vào `ocr/provider/` (cùng package với interface) | 1h | review #2 §2.1 |
| 4B.4 | Tạo `LlmProvider` interface + `LlmProviderChain` (SpringAi + OpenRouter adapters) | 4h | review #2 §1.2 |
| 4B.5 | Update `OcrProviderRegistry` để dùng provider chain mới | 1h | — |
| 4B.6 | Tests cho provider chain (mock interface) | 2h | review_gaps §6 |

#### Phase 4C — Còn lại: `auth/`, `user/`, `profile/`, `storage/`, `notification/`, `rag/`, `reference/`, `analytics/`, `admin/` (Tuần 7)

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 4C.1 | Migrate từng feature theo `folder_org §2.2` — mỗi feature 1 PR | 12h total | folder_org §2 |
| 4C.2 | Tạo `StoragePort` interface + `MinioStorageAdapter` | 2h | review #2 §1.2 |
| 4C.3 | Tạo `EmailPort` interface (chuẩn bị cho swap SES/SendGrid) | 1h | review #2 §1.2 |
| 4C.4 | Cleanup `common/` chỉ giữ cross-cutting thực sự | 2h | folder_org §2.2 |

**Deliverable Phase 4**: 10+ PRs (1 per feature), commit history rõ ràng.

---

### 🟣 PHASE 5 — Frontend Refactor (Tuần 8–9, ~30h)

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 5.1 | Add Next.js conventions: root `not-found.tsx`, `(dashboard)/loading.tsx`, `(dashboard)/error.tsx` | 2h | review_gaps §10 |
| 5.2 | Add PWA `src/app/manifest.ts` (đã có sẵn icon 192/512) | 30m | review #6 §3.2 |
| 5.3 | Migrate `apps/web/src/` sang Feature-Sliced Design (FSD-lite): `features/`, `shared/`, `layouts/` | 6h | folder_org §3 |
| 5.4 | Tách `ReviewRecordPage` (2,782 dòng) → 7 components + 3 hooks trong `features/health-records/` | 10h | review #2 §6.2 |
| 5.5 | Tách `admin/reference-data/{page,approvals/page}.tsx` (1,025 + 1,098 dòng) | 6h | review #2 §6.1 |
| 5.6 | Tách `(dashboard)/home/page.tsx` (744 dòng) + `admin/login/page.tsx` (691 dòng) | 4h | review #2 §6.1 |
| 5.7 | Extract inline types (7 interfaces trong review/page.tsx) → `features/health-records/types.ts` | 1h | review #1 §2.2 |
| 5.8 | Tạo domain hooks: `useHealthRecordDetail`, `useMetricEditing`, `useFollowUpReminders`, `useReferenceData` | 2h | review #2 §6.3 |
| 5.9 | Email templates: tạo 5 Thymeleaf templates còn thiếu (password-reset, deletion-cancelled, deletion-completed, health-record-invitation, follow-up-reminder) | 4h | review_gaps §5 |

**Deliverable Phase 5**: 5–6 PRs.

---

### 🟠 PHASE 6 — Mobile + OCR Service (Tuần 10, ~14h)

#### Phase 6A — Mobile unify

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 6A.1 | Quyết định: build mobile thật hay drop deps (54 deps cho placeholder?) | (decision) | review_gaps §1 |
| 6A.2 | Nếu giữ: unify `src/` theo `folder_org §4.2` | 3h | folder_org §4 |
| 6A.3 | Nếu giữ: add Jest Expo hoặc Vitest setup + smoke test | 2h | review_gaps §6 |
| 6A.4 | Nếu drop: remove unused deps, giữ Expo shell minimal | 1h | review_gaps §11.1 |

#### Phase 6B — OCR Service refactor

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 6B.1 | Tách `app.py` (455 dòng) thành modules: `src/main.py`, `src/ocr/`, `src/image/`, `src/core/` | 5h | folder_org §5 |
| 6B.2 | Tạo `services/ocr-service/tests/` folder + move test_ssrf_protection.py | 30m | folder_org §5 |
| 6B.3 | Add pytest CI job + Trivy scan Docker image | 1h | review_gaps §13 |
| 6B.4 | Verify CorrelationID propagate từ API → OCR svc | 1h | review_gaps §7 |
| 6B.5 | Convert `requirements.txt` → `pyproject.toml` (modern Python) | 1h | folder_org §5 |

**Deliverable Phase 6**: 3 PRs.

---

### 🔘 PHASE 7 — Observability + CI/CD Hardening (Tuần 11, ~12h)

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 7.1 | Add Micrometer custom metrics: OCR pipeline, request latency, error rate | 2h | review_gaps §7 |
| 7.2 | Expose `/actuator/prometheus` endpoint | 30m | review_gaps §7 |
| 7.3 | Add Sentry SDK (Java + web) cho error tracking | 2h | review_gaps §7 |
| 7.4 | Add JaCoCo coverage report + threshold (60% min) trong CI | 2h | review_gaps §6 |
| 7.5 | Add Trivy Docker image scan trong deploy workflow | 1h | review_gaps §13 |
| 7.6 | Add Lighthouse CI cho web (a11y + perf) | 1h | review_gaps §9 |
| 7.7 | Tách workflow staging-deploy.yml (hiện chỉ có prod) | 1h | review_gaps §13 |
| 7.8 | Add OpenAPI export task + auto-generate TS client → eliminate `routes.ts` manual sync | 2h | review #6 §5.1 |

**Deliverable Phase 7**: 3 PRs.

---

### 🔘 PHASE 8 — Docs + BMAD Cleanup (Tuần 12, ~10h)

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 8.1 | Restructure `_bmad-output/`: move 27 epic về `planning-artifacts/epics/epic-NN-…/` flat | 4h | repo_hygiene §2.5 |
| 8.2 | Tạo `_bmad-output/implementation-artifacts/sprint-status.yaml` (BMAD v6) | 30m | repo_hygiene §2.4 |
| 8.3 | Rename `STAGING_DEPLOYMENT.md` → `staging-deployment.md` | 5m | repo_hygiene §1.2 |
| 8.4 | Merge `project-context.md` + `project-overview.md` (verify trùng) | 30m | repo_hygiene §1.2 |
| 8.5 | Tạo `docs/security.md`, `docs/observability.md`, `docs/database-migrations.md`, `docs/code-style.md` | 4h | repo_hygiene §1.3 |
| 8.6 | Update `docs/source-tree-analysis.md` + `docs/architecture.md` reflect new package-by-feature | 1h | repo_hygiene §1.3 |

**Deliverable Phase 8**: 2 PRs.

---

### 🔘 PHASE 9 — AI Tooling Consolidation (1h, có thể song song)

| # | Action | Effort | Source |
|---|--------|:-:|--------|
| 9.1 | Audit team: dùng IDE nào? Giữ MAX 2 export skills | 15m | repo_hygiene §3.3 |
| 9.2 | Xóa `.opencode/node_modules/` (26 MB local) + verify `.opencode/package.json` cần? | 10m | repo_hygiene §3.3 |
| 9.3 | Disable IDE export không dùng trong `_bmad/_config/ides/` | 30m | repo_hygiene §3.3 |
| 9.4 | Audit `.github/skills/` (66 tracked skill folders) — có thực sự dùng trong GH Copilot? Nếu không, untrack | 30m | repo_hygiene §3.3 |

**Deliverable Phase 9**: 1 PR.

---

## 📊 Tổng effort estimate

| Phase | Tuần | Effort (h) | Cumulative (h) |
|-------|------|:-:|:-:|
| 0 — Safety net | 1 | 12 | 12 |
| 1 — Tooling & conventions | 2 | 10 | 22 |
| 2 — Shared foundation | 3 | 12 | 34 |
| 3 — Exceptions & error codes | 4 | 10 | 44 |
| 4 — Backend package-by-feature | 5–7 | 40 | 84 |
| 5 — Frontend refactor | 8–9 | 30 | 114 |
| 6 — Mobile + OCR svc | 10 | 14 | 128 |
| 7 — Observability + CI/CD | 11 | 12 | 140 |
| 8 — Docs + BMAD | 12 | 10 | 150 |
| 9 — AI tooling | parallel | 1 | 151 |

**Total: ~151h ≈ 19 working days ≈ 4 calendar weeks @ full-time HOẶC 12 calendar weeks @ part-time.**

---

## 🚦 Decision Gates

Mỗi phase có **gate** trước khi proceed:

| Gate | Trước khi sang phase |
|------|----------------------|
| **Tests xanh** | Mọi phase (yêu cầu cứng) |
| **Coverage không tụt** | Phase 4 (refactor lớn) |
| **Smoke E2E pass** | Phase 4 hoàn tất + Phase 5 hoàn tất |
| **Migration dry-run** | Phase 4 (nếu có thay đổi DB) |
| **Stakeholder review** | Phase 0 (safety items có thể block deploy) |

---

## 🎲 Risk Matrix

| Risk | Mitigation |
|------|------------|
| God service split → broken behavior | Phase 4A — chia thành 5 PR, mỗi PR thêm 1 service mới + keep old method delegate, sau khi all migrate → remove old |
| Status enum migration → in-flight records bị stuck | Phase 2.1 — write Flyway migration backfill + dual-read period |
| BMAD restructure → broken cross-references | Phase 8.1 — script auto-update links + grep verify |
| Format sweep → diff khổng lồ trong git blame | Phase 1.6 — commit message rõ `chore: apply formatter`, gen `.git-blame-ignore-revs` file |
| Mobile decision (build vs drop) → wasted effort | Phase 6A.1 — decision gate trước, không proceed nếu chưa decide |
| `_bmad-output/` move → conflict với in-flight stories | Phase 8.1 — pause story creation 1 ngày, batch move trong cùng PR |

---

## 🎯 Quick-win path (nếu chỉ làm được 20% effort)

Nếu chỉ có **30h** thay vì 151h, làm 20% cho 80% giá trị:

1. **Phase 0 toàn bộ** (12h) — safety
2. **Phase 1.1–1.7** (4h) — formatter setup
3. **Phase 2.1–2.3** (6h) — enum + state machine (eliminate 23+ magic strings)
4. **Phase 4A.4** (8h) — chỉ tách `HealthRecordService` (god class lớn nhất)

→ Tổng **30h**, đã giải quyết: licensing, stub, format chaos, status enum, god service lớn nhất.

---

## 📌 Quy ước commit message cho mỗi PR

Theo Conventional Commits:
- `chore(safety):` — Phase 0
- `chore(tooling):` — Phase 1
- `refactor(shared):` — Phase 2
- `feat(exception):` — Phase 3
- `refactor(api):` — Phase 4 (mỗi feature 1 commit type)
- `refactor(web):` — Phase 5
- `refactor(mobile)`, `refactor(ocr):` — Phase 6
- `feat(observability):` — Phase 7
- `docs:` + `chore(bmad):` — Phase 8

---

## ✅ Definition of Done — Toàn bộ refactor

- [ ] Tests xanh, coverage ≥ 70% trên feature đã refactor
- [ ] Lighthouse a11y score ≥ 90 trên 5 trang chính
- [ ] Zero `IllegalArgumentException` cho business errors
- [ ] Zero magic status strings (toàn enum)
- [ ] `HealthRecordService` ≤ 400 dòng (down từ 1,710)
- [ ] `ReviewRecordPage` ≤ 100 dòng shell (down từ 2,782)
- [ ] Mobile có CI test job HOẶC deps tinh giản
- [ ] OCR svc test trong CI
- [ ] Sentry capture errors trong production
- [ ] Custom metrics `/actuator/prometheus` exposed
- [ ] `_bmad-output/` theo BMAD v6 layout
- [ ] Tất cả 17+ docs file updated reflect new structure
- [ ] 0 file Arial.ttf trong repo
- [ ] PWA installable

---

## 📚 6 Review files tham khảo

1. [project_review.md](project_review.md) — Hybrid structure, god classes, constants base
2. [deep_code_review.md](deep_code_review.md) — SOLID, decomposition, adapter pattern, exception hierarchy
3. [folder_organization_solution.md](folder_organization_solution.md) — Package-by-feature chi tiết + FSD web/mobile + ocr-service modules
4. [review_gaps.md](review_gaps.md) — Feature maturity, DB migrations, scripts, CI/CD, observability, security, a11y, deps
5. [repo_hygiene_audit.md](repo_hygiene_audit.md) — docs/, BMAD output, AI tooling, standard repo files
6. [final_misc_audit.md](final_misc_audit.md) — Style/format, GitHub conventions, assets, localization, infra

Sources:
- [Conventional Commits](https://www.conventionalcommits.org/)
- [BMAD-METHOD docs](https://docs.bmad-method.org/tutorials/getting-started/)
- [Spotless Gradle](https://github.com/diffplug/spotless)
- [Next.js App Router](https://nextjs.org/docs/app/api-reference/file-conventions)
