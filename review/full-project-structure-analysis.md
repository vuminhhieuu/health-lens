# 🏗️ HealthLens — Full Project Structure Audit

**Date:** 2025-05-21  
**Scope:** Entire monorepo (excluding `_bmad/`, `_bmad-output/`, `.agent/`, `.agents/`, `.cursor/`, `.opencode/`, `.codex/`, IDE configs)

---

## Tổng Quan Monorepo

```text
health-lens/                          ← pnpm workspace + Gradle composite
├── apps/
│   ├── api/                          ← Spring Boot 4 · Java 21 · 291 files · 72 tests
│   ├── web/                          ← Next.js App Router · 121 TS/TSX files
│   └── mobile/                       ← Expo Router · scaffold stage
├── packages/
│   └── shared/                       ← TS constants, Zod schemas, types, env config
├── services/
│   └── ocr-service/                  ← FastAPI · single-file app (app.py)
├── docker/                           ← Compose files + helper scripts
├── docs/                             ← 18 markdown docs
├── infisical/                        ← Secret management scripts
├── .github/workflows/                ← CI (ci.yml) + Deploy (deploy.yml)
├── package.json                      ← pnpm workspace root
├── pnpm-workspace.yaml               ← workspace config
└── settings.gradle.kts               ← Gradle composite build
```

### Stats

| Workspace | Files | Lines (approx) | Tech |
|---|---|---|---|
| `apps/api` | 291 Java + 72 tests = **363** | ~25K | Spring Boot 4, JPA, Redis, Qdrant |
| `apps/web` | **121** TS/TSX | ~23K | Next.js, Zustand, TanStack Query |
| `apps/mobile` | ~15 | ~1K | Expo, React Native |
| `packages/shared` | 12 | ~1.5K | TypeScript |
| `services/ocr-service` | 1 | ~400 | FastAPI, EasyOCR |
| `docker/` | 3 compose + 4 scripts | ~1.5K | Docker Compose |
| `docs/` | 18 markdown | ~5K | Documentation |

---

## Part A: Monorepo Root Level

### ✅ Điểm Tốt

| Aspect | Chi tiết |
|---|---|
| **Workspace layout** | Rõ ràng: `apps/` cho applications, `packages/` cho shared, `services/` cho microservices |
| **pnpm workspace** | Đúng chuẩn monorepo TS |
| **Gradle composite** | API được include đúng cách trong `settings.gradle.kts` |
| **Docker scripts** | Tách compose files theo environment (base, dev, prod) |
| **CI/CD** | 2 workflow files rõ ràng: `ci.yml` và `deploy.yml` |

### 🔴 Issue R-1: Env File Sprawl (Nghiêm trọng)

```text
.env                    ← local dev (SHOULD NOT be in git)
.env.example            ← ✅ template
.env.production         ← ⚠️ production secrets at root!
.env.staging            ← ⚠️ staging secrets at root!
.env.staging.api        ← ⚠️ staging API-specific secrets!
```

**Vấn đề:** 5 env files ở root, trong đó `.env.production` và `.env.staging*` **có thể chứa secrets** và nằm ngay root. `.gitignore` có `.env.*` nhưng lại explicit excludes `.env.example` — cần verify `.env.production` có bị commit không.

**Đề xuất:**
- Verify: `git ls-files .env.production .env.staging .env.staging.api` — nếu tracked thì cần xóa khỏi git history ngay
- Chuyển sang secret management (Infisical đã setup nhưng có vẻ chưa dùng fully?)
- Nếu cần example files: `.env.staging.example`, `.env.production.example`

### 🟡 Issue R-2: Ghost `build/` Directory

```text
build/
└── reports/    ← empty
```

Root `build/` chỉ chứa empty `reports/`. Có thể là Gradle output leak. Nên verify và gitignore.

### 🟡 Issue R-3: `apps/web/apps/` và `apps/web/packages/` Ghost Directories

```text
apps/web/apps/web/node_modules/      ← empty phantom
apps/web/packages/shared/            ← empty phantom
```

Đây là leftover từ workspace resolution hoặc symlink issues. Chỉ chứa empty node_modules. Nên xóa clean.

---

## Part B: `apps/api` — Spring Boot Backend

> [!NOTE]
> Đã phân tích chi tiết trong report trước. Tóm tắt các issues ở đây, tham khảo `api-package-structure-analysis.md` cho solution.

### ✅ Điểm Tốt

| Aspect | Chi tiết |
|---|---|
| `ocr/` package | Feature-based, có sub-packages (provider/), rất tốt |
| `ai/` package | Clean bounded context: chat/, embedding/, rag/ |
| `audit/` package | Self-contained audit infrastructure |
| `events/` package | Tách event infrastructure + domain events |
| Architecture test | `PackageBoundaryTest.java` enforces AI/OCR boundaries |
| Test coverage | 72 test files, good coverage trên critical modules |

### 🔴 Issue B-1: Hybrid Layer/Feature Inconsistency

Đã phân tích ở report trước. `controller/`, `service/`, `entity/`, `repository/`, `dto/` vẫn theo layer-based, trong khi `ocr/`, `ai/`, `audit/` theo feature-based.

**→ Solution: Migrate to Feature-first** (xem `api-package-structure-analysis.md`)

### 🔴 Issue B-2: God Service Files

| File | Size | Lines (est.) |
|---|---|---|
| `HealthRecordService.java` | 77 KB | ~2000 |
| `LlmService.java` | 67 KB | ~1700 |
| `ReferenceDataAdminService.java` | 66 KB | ~1700 |
| `OcrService.java` | 47 KB | ~1200 |
| `AdminAuditLogService.java` | 36 KB | ~900 |

### 🟡 Issue B-3: Resources Directory Naming Confusion

```text
src/main/resources/
├── ai/                              ← seed data (metric-explanations.vi.json)
│   └── prompts/                     ← LLM prompt templates
├── llm/                             ← EMPTY directory!
├── static/                          ← EMPTY directory!
├── fonts/                           ← Arial.ttf (for PDF generation)
├── db/migration/                    ← 43 Flyway migrations
├── templates/email/                 ← 3 email templates
└── META-INF/
```

**Vấn đề:**
- `ai/` chứa cả seed data JSON + LLM prompts → nên tách
- `llm/` empty, có thể là intended nhưng chưa dùng
- `static/` empty
- `fonts/` chỉ 1 file, có thể move vào domain (pdf)

**Đề xuất:**
```text
src/main/resources/
├── db/migration/                    ← giữ nguyên
├── templates/email/                 ← giữ nguyên
├── prompts/                         ← LLM prompt templates (move từ ai/prompts/)
├── seed/                            ← seed data (move từ ai/)
├── fonts/                           ← giữ nguyên (dùng cho PDF)
└── META-INF/
```
Xóa `llm/` và `static/` empty.

### 🟡 Issue B-4: Test Structure Partially Fragmented

Tests mirror source structure **một phần**:
- ✅ `test/.../ocr/` mirrors `main/.../ocr/`
- ✅ `test/.../ai/` mirrors `main/.../ai/`
- ✅ `test/.../audit/` mirrors `main/.../audit/`
- ⚠️ `test/.../service/` still flat (20+ test files)
- ⚠️ `test/.../controller/` flat
- ✅ `test/.../architecture/PackageBoundaryTest.java` — great practice!
- ✅ `test/.../support/` for test utilities

Tests cần migrate song song khi refactor source.

---

## Part C: `apps/web` — Next.js Frontend

### ✅ Điểm Tốt

| Aspect | Chi tiết |
|---|---|
| **Route groups** | `(auth)`, `(dashboard)`, `(marketing)` — chuẩn Next.js App Router |
| **Components organization** | `features/`, `ui/`, `layout/`, `admin/`, `marketing/`, `notifications/` |
| **Lib organization** | Feature-oriented: `api/`, `auth/`, `consent/`, `admin/`, `seo/`, `sharing/`, `browser/`, `i18n/`, `marketing/`, `utils/` |
| **State management** | Clean: `stores/authStore.ts` |
| **Testing** | Route policy tests, SEO tests, unit tests co-located |
| **SEO** | `robots.ts`, `sitemap.ts`, `seo/metadata.ts` |

### 🟡 Issue W-1: God Page Components

| File | Lines | Bytes |
|---|---|---|
| `(dashboard)/health-records/review/[recordId]/page.tsx` | **2,446** | ~90KB |
| `admin/audit-log/page.tsx` | **1,813** | ~65KB |
| `admin/reference-data/approvals/page.tsx` | **1,098** | ~40KB |
| `admin/reference-data/page.tsx` | **1,035** | ~37KB |
| `(dashboard)/home/page.tsx` | **741** | 28KB |

> [!WARNING]
> Page files > 300 lines nên được tách thành components. `review/[recordId]/page.tsx` (2446 dòng!) là anti-pattern nghiêm trọng.

**Đề xuất:**
```text
# Trước
(dashboard)/health-records/review/[recordId]/
└── page.tsx                    (2446 lines!)

# Sau
(dashboard)/health-records/review/[recordId]/
├── page.tsx                    (~50 lines, composition only)
├── _components/
│   ├── ReviewHeader.tsx
│   ├── MetricResultsPanel.tsx
│   ├── AIExplanationSection.tsx
│   ├── RecommendationsPanel.tsx
│   └── ReviewActions.tsx
└── _hooks/
    └── useHealthRecordReview.ts
```

### 🟡 Issue W-2: `components/features/` Sparse

```text
components/features/
├── consent/          → 1 file (ConsentModal.tsx)
├── health-records/   → 1 file (DeleteRecordModal.tsx)
├── profiles/         → 4 files
├── upload/           → 2 files
└── index.ts
```

Trong khi most feature logic nằm inline trong page files (xem Issue W-1). Nên di chuyển page-level components vào `features/`.

### 🟡 Issue W-3: `lib/` Has Loose Files

```text
lib/
├── admin/                    ← directory ✅
├── api/                      ← directory ✅
├── auth/                     ← directory ✅
├── browser/                  ← directory ✅
├── consent/                  ← directory ✅
├── i18n/                     ← directory ✅
├── marketing/                ← directory ✅
├── seo/                      ← directory ✅
├── sharing/                  ← directory ✅
├── utils/                    ← directory ✅
├── healthRecordHub.ts        ← ⚠️ loose file
├── notify.ts                 ← ⚠️ loose file
└── profileMappings.ts        ← ⚠️ loose file (8KB!)
```

**Đề xuất:**
- `healthRecordHub.ts` → `lib/health-records/hub.ts`
- `profileMappings.ts` → `lib/profiles/mappings.ts`
- `notify.ts` → `lib/notifications/notify.ts`

### ✅ Issue W-4: `components/ui/` Minimal

Chỉ có 3 files (`HealthMetricCard.tsx`, `StateComponents.tsx`, `index.ts`). Không có shared UI primitives (Button, Modal, Input, etc.). Có thể đang dùng library (Chakra/MUI?) hoặc inline styles — cần verify.

---

## Part D: `apps/mobile` — Expo App

### 🟡 Issue M-1: Dual Source Structure (Confusion)

```text
apps/mobile/
├── app/                 ← Expo Router entry (1 file: index.tsx)
├── components/          ← Root-level components (11 files)
├── hooks/               ← Root-level hooks (3 files)
├── lib/                 ← Root-level lib (api/)
├── stores/              ← Root-level stores (1 file)
└── src/                 ← ALSO has app/, components/, constants/, hooks/
    ├── app/             ← 3 files (_layout, index, explore)
    ├── components/      ← 10 files + ui/
    ├── constants/       ← 1 file (theme.ts)
    └── hooks/           ← 3 files
```

**Vấn đề:** Code bị **phân tán ở 2 nơi**: root-level (`components/`, `hooks/`, `lib/`, `stores/`) và `src/` subdirectory. Đây là do Expo default scaffold tạo trong `src/` nhưng project thêm code ở root.

**Đề xuất:** Chọn MỘT convention và consolidate:
```text
apps/mobile/
├── app/                 ← Expo Router (keep at root per Expo convention)
└── src/
    ├── components/
    ├── hooks/
    ├── lib/
    ├── stores/
    └── constants/
```

### 🟡 Issue M-2: Nhiều Empty Barrel Files

```text
components/index.ts     → "export {};"
hooks/index.ts          → "export {};"
stores/index.ts         → "export {};"
```

Placeholder files, no actual exports. Mobile app đang ở scaffold stage.

---

## Part E: `packages/shared` — Shared Package

### ✅ Tổ chức Tốt

```text
packages/shared/
├── config/
│   ├── env.ts              ← Zod env validation
│   └── index.ts
├── constants/
│   ├── api.ts              ← Route constants (10KB!) — synced with ApiRoutes.java
│   ├── consent.ts
│   ├── error-codes.ts      ← Error code registry (8.5KB)
│   ├── status.ts
│   └── index.ts
├── schemas/
│   ├── auth.ts
│   ├── profile.ts
│   ├── user.ts
│   └── index.ts
├── types/
│   └── index.ts
└── index.ts
```

**Nhận xét:** Clean, đúng mục đích. Duy nhất `api.ts` (10KB) hơi lớn, nhưng vẫn hợp lý vì nó là single source of truth cho routes.

### 🟡 Issue S-1: Thiếu Health Record Schemas

`schemas/` có `auth.ts`, `profile.ts`, `user.ts` nhưng **thiếu health record schemas** — domain chính của app. Có thể validation đang inline trong web pages.

---

## Part F: `services/ocr-service` — OCR Microservice

### ✅ Tổ chức Phù Hợp

```text
services/ocr-service/
├── app.py                  ← Single-file FastAPI app (15KB, ~400 lines)
├── requirements.txt
├── Dockerfile
└── README.md
```

Cho một microservice đơn giản, single-file là hợp lý. Nếu service phát triển lớn hơn, nên tách:

```text
services/ocr-service/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── routes.py
│   ├── ocr_engine.py
│   └── models.py
├── requirements.txt
└── Dockerfile
```

Nhưng **hiện tại 400 dòng, chưa cần tách**.

---

## Part G: `docker/` — Infrastructure

### ✅ Tổ chức Tốt

```text
docker/
├── compose.yml             ← Base services (Redis, MinIO)
├── compose.dev.yml         ← Dev overrides (API, Web, Postgres, Mailhog, OCR)
├── compose.prod.yml        ← Production overrides
└── scripts/
    ├── up.sh               ← Start stack
    ├── down.sh             ← Stop stack
    ├── logs.sh             ← View logs
    └── cleanup.sh          ← Clean volumes/images
```

Chuẩn Docker Compose multi-environment pattern. Không có vấn đề.

---

## Part H: `docs/` — Documentation

### ✅ Nội dung phong phú, 18 files

```text
docs/
├── index.md                         ← Master index
├── project-overview.md
├── project-context.md
├── architecture.md
├── source-tree-analysis.md
├── component-inventory.md
├── data-models.md
├── api-contracts.md
├── event-driven-architecture.md
├── backend-ai-ocr-rag-package-map.md
├── llm-prompt-templates.md
├── environment-reference.md
├── development-guide.md
├── testing-guide.md
├── deployment-guide.md
├── operations-runbook.md
├── provider-switching-runbook.md
└── STAGING_DEPLOYMENT.md
```

### 🟡 Issue D-1: Thiếu Cấu Trúc Phân Loại

18 files flat trong 1 directory. Với volume này, nên nhóm:

**Đề xuất:**
```text
docs/
├── index.md
├── architecture/
│   ├── overview.md              (← architecture.md)
│   ├── data-models.md
│   ├── event-driven.md          (← event-driven-architecture.md)
│   ├── api-contracts.md
│   └── source-tree.md           (← source-tree-analysis.md)
├── guides/
│   ├── development.md           (← development-guide.md)
│   ├── testing.md               (← testing-guide.md)
│   └── deployment.md            (← deployment-guide.md)
├── operations/
│   ├── runbook.md               (← operations-runbook.md)
│   ├── provider-switching.md    (← provider-switching-runbook.md)
│   ├── staging.md               (← STAGING_DEPLOYMENT.md)
│   └── environment-reference.md
└── reference/
    ├── component-inventory.md
    ├── backend-package-map.md   (← backend-ai-ocr-rag-package-map.md)
    ├── llm-prompt-templates.md
    ├── project-context.md
    └── project-overview.md
```

**Tuy nhiên**, flat docs cũng chấp nhận được ở scale 18 files. Đây là **optional improvement**, không urgent.

---

## Tổng Hợp Issues — Severity Matrix

| ID | Area | Issue | Severity | Effort |
|---|---|---|---|---|
| **B-1** | API | Hybrid Layer/Feature inconsistency | 🔴 High | Large |
| **B-2** | API | God Service files (77KB, 67KB, 66KB) | 🔴 High | Medium |
| **R-1** | Root | Env file sprawl / possible secret leak | 🔴 High | Small |
| **W-1** | Web | God Page components (2446 lines) | 🟡 Medium | Medium |
| **M-1** | Mobile | Dual source structure confusion | 🟡 Medium | Small |
| **B-3** | API | Resources directory naming confusion | 🟡 Medium | Small |
| **W-2** | Web | `components/features/` sparse vs inline pages | 🟡 Medium | Medium |
| **W-3** | Web | `lib/` loose files | 🟡 Low | Small |
| **B-4** | API | Test structure partially fragmented | 🟡 Low | Medium |
| **R-2** | Root | Ghost `build/` directory | 🟢 Low | Trivial |
| **R-3** | Root | Ghost `apps/web/apps/` and `apps/web/packages/` | 🟢 Low | Trivial |
| **M-2** | Mobile | Empty barrel files | 🟢 Low | Trivial |
| **S-1** | Shared | Missing health record schemas | 🟢 Low | Small |
| **D-1** | Docs | Flat directory (18 files) | 🟢 Low | Small |

---

## Phần Kết Luận: API Solution Đã Best Practice Chưa?

> [!IMPORTANT]
> **Có, solution API package-by-feature đã đúng best practice.** Cụ thể:

| Tiêu chí | Đánh giá |
|---|---|
| Feature-first packaging | ✅ Đúng theo consensus 2025 |
| Layer bên trong feature | ✅ `controller/`, `service/`, `entity/`, `repository/` trong mỗi feature |
| Cross-cutting → `common/` | ✅ Audit, correlation, security, exception, config |
| Domain isolation | ✅ Mỗi feature là bounded context |
| Tách God services | ✅ HealthRecordService → Query + Command + Share + Pdf |
| Config co-location | ✅ OcrServiceConfig → `ocr/config/`, AiChatConfig → `ai/chat/config/` |
| Architecture test guard | ✅ PackageBoundaryTest already enforces boundaries |

**Bổ sung từ full-project audit:**
- Solution API cần **kết hợp** với việc fix các issues khác (W-1 God pages, R-1 env files, M-1 mobile structure) để đạt overall project health.

---

## Lộ Trình Ưu Tiên

### 🚨 Immediate (ngày mai)
1. **R-1:** Verify `.env.production`, `.env.staging*` có bị git track không → xử lý secret leak
2. **R-2, R-3:** Xóa ghost directories

### 📋 Sprint tiếp theo
3. **B-1:** Bắt đầu API package-by-feature migration (Phase 1-2 từ report trước)
4. **B-3:** Cleanup resources directory (xóa empty dirs, rename)
5. **W-3:** Di chuyển lib loose files

### 📅 Upcoming sprints
6. **W-1:** Tách God page components (health-records/review > priority)
7. **B-2:** Tách God services (HealthRecordService > priority)
8. **M-1:** Consolidate mobile structure (khi bắt đầu phát triển mobile seriously)
9. **W-2:** Consolidate features components

### ♻️ Ongoing
10. **B-4:** Migrate tests song song với source refactor
11. **S-1:** Add shared schemas khi cần
12. **D-1:** Reorganize docs khi thêm nhiều docs mới
