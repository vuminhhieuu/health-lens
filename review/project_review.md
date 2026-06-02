# 🔍 HealthLens — Comprehensive Project Review

> **Scope**: Backend API (Spring Boot/Java), Web (Next.js/TS), Mobile (Expo/RN), OCR Service (Python/FastAPI), Shared Package  
> **Date**: 2026-05-28 | **Files scanned**: ~300 Java + ~186 TS/TSX + 1 Python monolith

---

## 1. 📂 Tổ chức File / Folder

### 1.1 API Backend — Hybrid Architecture ⚠️ CRITICAL

**Vấn đề**: Backend đang dùng kiến trúc **hybrid** — vừa layer-based (`service/`, `controller/`, `entity/`) vừa feature-based (sub-packages `admin/`, `ocr/`, `rag/`). Điều này khiến code khó navigate và inconsistent.

```
com.healthlens.api/
├── controller/         ← layer-based (flat)
│   └── admin/          ← feature-based (chỉ admin có sub-package)
├── service/            ← layer-based (flat) — 60+ files
│   ├── admin/          ← feature-based
│   ├── ocr/            ← feature-based
│   └── rag/            ← feature-based
├── entity/             ← flat, 37 files mixed
├── repository/         ← flat
├── dto/request/        ← flat, chỉ request/response
├── dto/response/       ← 50+ response DTOs flat
├── constants/          ← chỉ 2 files
├── audit/              ← feature-based (11 files)
├── events/ocr/ + email/ ← feature-based
└── config/             ← flat, 15 files mixed
```

**Khuyến nghị**: Migrate sang **Package-by-Feature** hoàn toàn:
```
com.healthlens.api/
├── healthrecord/   ← controller, service, repo, dto, entity
├── profile/
├── auth/
├── admin/
├── ocr/
├── notification/
├── audit/
└── shared/         ← config, security, exception, util
```

### 1.2 Web Frontend — Inconsistent Nesting

| Vấn đề | Chi tiết |
|---------|----------|
| `apps/web/apps/` | Ghost directory — web app nested **inside** another `apps/` |
| `apps/web/packages/` | Web có `packages/` riêng ngoài root `packages/` |
| `apps/web/src/lib/` | 12 sub-dirs + 8 loose files — quá phẳng, thiếu grouping |
| `apps/web/src/components/ui/` | Mix generic UI (`SafeImage`) với domain (`HealthMetricCard`) |

### 1.3 Mobile — Dual Source Paths ⚠️

```
apps/mobile/
├── app/          ← Expo Router routes
├── components/   ← TOP-LEVEL components (outside src/)
├── hooks/        ← TOP-LEVEL hooks (outside src/)
├── lib/          ← TOP-LEVEL lib (outside src/)
├── stores/       ← TOP-LEVEL stores (outside src/)
└── src/          ← ALSO has components/, hooks/, constants/
```

Code nằm cả **trong** và **ngoài** `src/`. Cần merge hết vào `src/`.

### 1.4 OCR Service — Single-File Monolith

[app.py](file:///home/vmhieu/Workspace/UIT/IE303/Project/health-lens/services/ocr-service/app.py) = **456 lines** chứa tất cả: models, validation, SSRF protection, image download, OCR logic, endpoints. Cần tách ra modules.

### 1.5 Root Level — Config Sprawl

Root có **6 env files**: `.env`, `.env.example`, `.env.production`, `.env.staging`, `.env.staging.api`, `.infisical.json`. Nên dùng 1 `.env.example` + secret manager.

---

## 2. 🧹 Code Cleanliness

### 2.1 God Classes ⚠️ CRITICAL

| File | Lines | Dependencies | Severity |
|------|-------|-------------|----------|
| [HealthRecordService.java](file:///home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java) | **1,710** | 18 injected deps | 🔴 Critical |
| [review/page.tsx](file:///home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/web/src/app/(dashboard)/health-records/review/%5BrecordId%5D/page.tsx) | **2,783** | 30+ state vars | 🔴 Critical |
| [LlmService.java](file:///home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/api/src/main/java/com/healthlens/api/service/LlmService.java) | **1,446** | — | 🟡 High |
| [ReferenceDataAdminService.java](file:///home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java) | **1,496** | — | 🟡 High |
| [OcrService.java](file:///home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/api/src/main/java/com/healthlens/api/service/OcrService.java) | **1,081** | — | 🟡 High |

**`HealthRecordService`** inject **18 dependencies** — vi phạm SRP rõ ràng. Nên tách thành:
- `HealthRecordUploadService` (upload URL, confirm)
- `HealthRecordQueryService` (getStatus, getDetail, getShared)
- `HealthRecordExplanationService` (metric explanation, RAG citations)
- `HealthRecordRecommendationService` (recommendations)
- `HealthRecordPdfExportService` (PDF generation)

**`ReviewRecordPage`** (2,783 lines) có **30+ useState** — cần tách thành custom hooks + sub-components.

### 2.2 Inline Type Definitions

[review/page.tsx](file:///home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/web/src/app/(dashboard)/health-records/review/%5BrecordId%5D/page.tsx#L72-L178) định nghĩa **7 types inline** (`MetricDto`, `ReviewRecordData`, `RecommendationsData`...) thay vì import từ `src/types/` hoặc `@healthlens/shared`.

### 2.3 Hardcoded Vietnamese Strings in Backend

Exception messages dùng Vietnamese hardcoded:
```java
throw new IllegalArgumentException("Kết quả khám không tồn tại");
throw new IllegalStateException("Chỉ được thử tải lên lại khi OCR thất bại");
throw new ProfileAccessRevokedException("Bạn không có quyền tải lên cho hồ sơ này");
```

Cần extract sang message bundle hoặc error code constants — đặc biệt khi đã có `ErrorCode` system trong shared package nhưng **backend không dùng**.

### 2.4 Deprecated Code Kept in Production

```typescript
// api.ts line 109-114
/** @deprecated Prefer `EXPLANATION`... */
EXPLANATION_PATH: (id, metricName) => ...
```

Deprecated route vẫn export. Cần migration plan rõ ràng hoặc remove.

---

## 3. 🔑 Constants — Scattered & Duplicated

### 3.1 Gender — 3 Definitions ⚠️

| Location | Values | Case |
|----------|--------|------|
| `shared/constants/index.ts:29` | `["male", "female", "other"]` | **lowercase** |
| `shared/constants/status.ts:132` | `MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY` | **UPPER_CASE** |
| Java entity/DTO | Depends on caller | mixed |

**3 nguồn truth cho cùng 1 concept**, với values khác nhau (lowercase vs UPPERCASE, có/không `PREFER_NOT_TO_SAY`).

### 3.2 Pagination — Duplicated

| Location | Constant |
|----------|----------|
| `shared/constants/api.ts:235` | `API_PAGINATION = { DEFAULT_PAGE: 0, DEFAULT_SIZE: 20, MAX_SIZE: 100 }` |
| `shared/config/env.ts:104` | `PAGINATION = { DEFAULT_PAGE: 0, DEFAULT_SIZE: 20, MAX_SIZE: 100 }` |

Exact same values, defined twice.

### 3.3 Health Record Status — Magic Strings ⚠️ CRITICAL

Backend dùng **raw strings** cho status mà không có enum:

```java
// Scattered across 6+ files:
"processing"       // HealthRecordService.java (partially extracted as STATUS_PROCESSING)
"done"             // HealthRecordService, ProfileService, AnalyticsService
"review_required"  // HealthRecordService (15+ occurrences)  
"ocr_failed"       // HealthRecordService, AnalyticsService
"failed"           // ProfileService
```

`HealthRecordService` chỉ extract `STATUS_PROCESSING` thành constant, nhưng `"done"`, `"review_required"`, `"ocr_failed"` vẫn là **magic strings** rải khắp nơi.

Frontend cũng dùng string literals:
```typescript
type ReviewRecordStatus = "processing" | "review_required" | "done" | "ocr_failed";
```

> Shared package đã có `HealthRecordStatus` enum (`DRAFT, PROCESSING, COMPLETED, FAILED, ARCHIVED`) nhưng **không ai dùng** — và values không khớp với actual usage.

### 3.4 Consent Version — Sync Comment Smell

```typescript
// consent.ts
/** Default consent policy version (keep in sync with API `ConsentConstants.ACTIVE_VERSION`). */
export const CONSENT_VERSION = '1.0';
```

```java
// ConsentConstants.java
public static final String ACTIVE_VERSION = "1.0";
```

"Keep in sync" comment = **code smell**. Giá trị này nên lấy từ API, không hardcode ở 2 nơi.

### 3.5 API Routes — Triple Indirection

```
packages/shared/constants/api.ts  ← Source of Truth
   ↓ imported by
apps/web/src/lib/api/routes.ts    ← Re-exports as API_ROUTES (redundant wrapper)
   ↓ some pages import from here, some directly from shared
apps/web/src/app/.../page.tsx     ← Mixed imports
```

`routes.ts` tồn tại "for backward compatibility" nhưng tạo confusion. Nên loại bỏ.

### 3.6 File Size Constants — Mismatch

| Location | Max Size |
|----------|----------|
| `shared/constants/index.ts` | `UPLOAD_MAX_SIZE_BYTES = 20 * 1024 * 1024` (20MB) |
| `shared/config/env.ts` | `OCR_CONFIG.MAX_FILE_SIZE = 10 * 1024 * 1024` (10MB) |
| `services/ocr-service/app.py` | `MAX_IMAGE_SIZE_BYTES = 30 * 1024 * 1024` (30MB) |

Ba giá trị khác nhau (20MB, 10MB, 30MB) cho cùng pipeline upload → OCR.

---

## 4. 🧠 Logic Issues

### 4.1 Status Comparison Without Enum

```java
// HealthRecordService.java:290
if ("review_required".equals(record.getStatus()) 
    || "done".equals(record.getStatus()) 
    || "ocr_failed".equals(record.getStatus())) {
```

Pattern này lặp lại **15+ lần** — typo-prone, không compile-time safe.

### 4.2 Caching Status with 5-Second TTL (Redis)

```java
// HealthRecordService.java:322
redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofSeconds(5));
```

Cache 5 giây cho status response chứa **presigned URL** (1-hour expiry). Cache quá ngắn → gần như vô ích, chỉ tăng complexity.

### 4.3 Exception Swallowing

```java
} catch (Exception e) {
    log.warn("Failed to parse metrics for {}", recordId);  // No re-throw, no fallback
}
```

Lỗi parse metrics bị nuốt — user có thể thấy empty state mà không biết tại sao.

### 4.4 Fully Qualified Names in Method Body

```java
// HealthRecordService.java:288
java.util.List<MetricDto> metricsList = null;
// HealthRecordService.java:405
List<com.healthlens.api.entity.HealthRecordShare> shares = ...
```

Import đã có nhưng vẫn dùng fully-qualified name — code smell do copy-paste.

### 4.5 Frontend — 30+ useState ≈ State Explosion

```typescript
// review/page.tsx:284-329
const [metrics, setMetrics] = useState<MetricDto[]>([]);
const [editingIndex, setEditingIndex] = useState<number | null>(null);
// ... 28 more useState declarations
```

Cần chuyển sang `useReducer` hoặc extract custom hooks.

---

## 5. 🏗 Design Patterns

### 5.1 Missing Patterns

| Pattern cần | Hiện trạng | Nơi áp dụng |
|-------------|-----------|-------------|
| **State Machine** | Magic strings + if/else chains | Health Record status transitions |
| **Strategy** | `if/else` for OCR providers | `OcrService` provider selection |
| **Facade** | `HealthRecordService` does everything | Cần facade cho health record operations |
| **Repository Pattern** (proper) | Repos leak into services directly | Service layer coupling |
| **DTO Mapper** | Manual mapping everywhere | `toOnlineCitationResponses()`, `getDetail()` |

### 5.2 Anti-Patterns Detected

| Anti-pattern | Location | Impact |
|-------------|----------|--------|
| **God Object** | `HealthRecordService` (18 deps) | Untestable, unmaintainable |
| **God Component** | `ReviewRecordPage` (2783 lines) | Unreadable, can't be code-reviewed |
| **Primitive Obsession** | Status as `String` instead of enum | Runtime errors |
| **Feature Envy** | Service builds context for another service | `buildRetrievalContext()` → `MetricExplanationRetrievalService` |
| **Dead Code Carrier** | `@deprecated EXPLANATION_PATH` still exported | Confusion |
| **Shotgun Surgery** | Changing status values requires editing 6+ files | Fragile |

### 5.3 Good Patterns Already Present ✅

- **Event-driven architecture** (`OcrJobEvent`, `EmailConsumer`) — well structured
- **Audit system** (`AuditEventRecorder`, `UnifiedAuditCoordinator`) — clean separation
- **Shared package** for cross-platform constants — good foundation
- **Rate limiter** abstraction (`PublicEndpointRateLimiter`)
- **Provider registry** for OCR (`OcrProviderRegistry`)

---

## 6. 🌐 Cross-Cutting Concerns

### 6.1 Error Handling Gap

Shared package defines `ErrorCode` system (231 lines, well-structured) but:
- **Java backend doesn't import/use it** — throws raw exceptions with Vietnamese strings
- **Web frontend partially uses it** — some pages check error codes, others don't

### 6.2 No Shared Status Enum

`packages/shared/constants/status.ts` defines `HealthRecordStatus = {DRAFT, PROCESSING, COMPLETED, FAILED, ARCHIVED}` but actual values in code are `"processing", "done", "review_required", "ocr_failed"` — **completely different set**.

### 6.3 API Version Hardcoded

`API_VERSION = 'v1'` is in shared package, but Java backend has it in `application.yml`. No mechanism to keep in sync.

---

## 📋 Priority Action Plan

### 🔴 P0 — Critical (làm ngay)

1. **Extract Health Record Status Enum** — tạo Java enum + sync với TS shared constants
2. **Split `HealthRecordService`** — tách thành 4-5 focused services
3. **Split `ReviewRecordPage`** — extract hooks + sub-components

### 🟡 P1 — High (sprint tiếp)

4. **Resolve Gender/Pagination duplication** — single source of truth
5. **Fix file size constant mismatch** (20MB vs 10MB vs 30MB)
6. **Remove `routes.ts` wrapper** — import directly from shared
7. **Consolidate mobile `src/` structure**

### 🟢 P2 — Medium (backlog)

8. **Migrate to Package-by-Feature** for Java backend
9. **Split OCR service** `app.py` into modules
10. **Implement DTO mapper layer** (MapStruct)
11. **Clean up root env files**
12. **Wire `ErrorCode` system** end-to-end
