# 🔍 HealthLens — Deep Dive: Constants, Scripts & Remaining Areas

**Date:** 2025-05-21  
**Supplement to:** `api-package-structure-analysis.md` + `full-project-structure-analysis.md`

---

## Part 1: Constants — Cross-Stack Sync Analysis

### 1.1 Constants Map: Ai ở đâu?

| Concern | Backend (Java) | Frontend (TS) | Sync Status |
|---|---|---|---|
| **API Routes** | `constants/ApiRoutes.java` (190 lines) | `shared/constants/api.ts` (241 lines) | ⚠️ Risky |
| **Error Codes** | `exception/ApiErrorCode.java` (15 codes) | `shared/constants/error-codes.ts` (58 codes) | 🔴 Drift |
| **Status Enums** | `entity/AccountStatus.java`, `entity/OcrJobState.java`, `entity/UserRole.java`, etc. (scattered in entities) | `shared/constants/status.ts` (10 enums, 197 lines) | 🟡 Partial |
| **Consent Version** | `constants/ConsentConstants.java` (1 field) | `shared/constants/consent.ts` (1 field) | ✅ Synced |
| **Security Cookie Names** | `constants/SecurityConstants.java` (4 fields) | Not shared | ✅ Backend-only |
| **Stream Names** | `events/ApplicationStreamNames.java` (3 fields) | Not shared | ✅ Backend-only |
| **Audit Actions** | `audit/AuditActions.java` (41 constants) | Not shared | ✅ Backend-only |
| **Audit Resource Types** | `audit/AuditResourceTypes.java` (9 constants) | Not shared | ✅ Backend-only |
| **Activity Event Types** | `activity/UserActivityEventType.java` (2 constants) | Not shared | ✅ Backend-only |
| **File Upload Limits** | Implicit in OcrService/config | `shared/constants/index.ts` (UPLOAD_MAX_SIZE, ALLOWED_FILE_TYPES) | 🟡 No sync |
| **Pagination Defaults** | Implicit in service code | `shared/constants/api.ts` (API_PAGINATION) | 🟡 No sync |
| **Retry Config** | Implicit in service code | `shared/constants/api.ts` (API_RETRY) | 🟡 No sync |
| **Timeout Config** | Implicit in service code | `shared/constants/api.ts` (API_TIMEOUT) | ✅ Frontend-only |

---

### 🔴 Issue C-1: Error Code Drift (Nghiêm trọng)

Backend `ApiErrorCode.java` có **15 error codes** (generic):
```java
ACCOUNT_LOCKED, RATE_LIMITED, ACCOUNT_PENDING_DELETION,
INVALID_CREDENTIALS, EMAIL_ALREADY_EXISTS, VALIDATION_ERROR,
BUSINESS_ERROR, INVALID_STATE, NOT_FOUND, FORBIDDEN,
CONSENT_REQUIRED, PROFILE_ACCESS_REVOKED,
DELETION_CANCEL_TOKEN_INVALID, DATABASE_ERROR, BAD_REQUEST
```

Frontend `error-codes.ts` có **58 error codes** (granular, RFC 7807):
```typescript
AUTH_001...AUTH_013, VAL_001...VAL_011, RES_001...RES_040,
BIZ_001...BIZ_008, EXT_001...EXT_053, SYS_001...SYS_004
```

**Vấn đề:** Hai hệ thống error code **hoàn toàn khác nhau**:
- Backend gửi `ACCOUNT_LOCKED`, frontend map theo `AUTH_008`
- Frontend có `ErrorMessage` map tiếng Việt cho codes mà backend **không bao giờ gửi**
- Frontend có `ErrorHttpStatus` map chi tiết, nhưng backend quyết định HTTP status riêng

**Đề xuất:**
- **Option A (Recommended):** Backend adopt shared error code format (`AUTH_001`, `VAL_001`, etc.) → frontend `ErrorCode` trở thành single source of truth
- **Option B:** Tạo mapping layer: backend `ApiErrorCode` → frontend `ErrorCode` trong `GlobalExceptionHandler`
- Xóa unused error codes trong frontend (ví dụ: `SUBSCRIPTION_REQUIRED`, `TRIAL_EXPIRED` — features chưa tồn tại)

### ⚠️ Issue C-2: API Routes Sync Risk

Backend `ApiRoutes.java` (190 lines) và frontend `api.ts` (241 lines) **phải sync thủ công**.

**Các risk phát hiện:**

| Backend Route | Frontend Route | Mismatch? |
|---|---|---|
| `DOCUMENTS_BASE`, `DOCUMENT_BY_ID`, `DOCUMENTS_UPLOAD`, `DOCUMENT_DOWNLOAD` | `ApiPaths.DOCUMENTS.*` | ⚠️ Backend có routes cho Document entity nhưng **không có DocumentController** — ghost routes! |
| Không có `HEALTH_RECORD_DOWNLOAD_PDF` | `ApiPaths.HEALTH_RECORDS.DOWNLOAD_PDF(id)` | 🟡 Frontend có, backend thiếu constant |
| Không có `HEALTH_RECORD_UPLOAD_IMAGE` | `ApiPaths.HEALTH_RECORDS.UPLOAD_IMAGE(id)` | 🟡 Frontend có, backend thiếu constant |
| `HEALTH_RECORD_METRIC_EXPLANATION` (path param) | `EXPLANATION` (query param) + deprecated `EXPLANATION_PATH` | ✅ Handled, nhưng deprecated route vẫn tồn tại |

**Đề xuất:**
- Xóa `DOCUMENTS_*` routes nếu Document entity chưa implement (dead code)
- Thêm constants cho PDF download, image upload vào backend `ApiRoutes.java`
- Tạo CI check (simple script) so sánh route paths giữa `ApiRoutes.java` và `api.ts`

### 🟡 Issue C-3: Status Enum Drift

Frontend `status.ts` định nghĩa 10 enums:

| Frontend Enum | Backend Equivalent | Sync? |
|---|---|---|
| `UserStatus` (5 values) | `AccountStatus.java` (enum) | ⚠️ Cần verify match |
| `HealthRecordStatus` (5 values) | Implicit in `HealthRecord.java` | ⚠️ No explicit enum |
| `Gender` (4 values) | Implicit in `Profile.java` | ⚠️ No explicit enum |
| `BloodType` (9 values) | Implicit in `Profile.java` | ⚠️ No explicit enum |
| `DocumentType` (7 values) | **No backend equivalent** | 🔴 Dead code (Documents chưa implement) |
| `DocumentStatus` (4 values) | **No backend equivalent** | 🔴 Dead code |
| `SubscriptionTier` (4 values) | **No backend equivalent** | 🔴 Dead code (Subscription chưa implement) |
| `RelationshipType` (7 values) | Implicit in `Profile.java` | ⚠️ No explicit enum |
| `ProfileStatus` (3 values) | Implicit | 🟡 |
| `AnalysisStatus` (5 values) | Implicit | 🟡 |

**Vấn đề:** 3 enums hoàn toàn dead code (`DocumentType`, `DocumentStatus`, `SubscriptionTier`). Các status khác không có explicit Java enum, chỉ lưu dạng String trong entity.

**Đề xuất:**
- Xóa `DocumentType`, `DocumentStatus`, `SubscriptionTier` (hoặc comment as `@planned`)
- Tạo explicit Java enums cho `Gender`, `BloodType`, `RelationshipType` để type-safe

### 🟡 Issue C-4: Constants trong `index.ts` Barrel — Inline Definitions

```typescript
// packages/shared/constants/index.ts
export const METRIC_SOURCE = { OCR: "ocr", MANUAL: "manual" } as const;
export const RECORD_SOURCE_TYPE = { OCR: "ocr", MANUAL: "manual", MIXED: "mixed" } as const;
export const GENDER_OPTIONS = ["male", "female", "other"] as const;
export const UPLOAD_MAX_SIZE_BYTES = 20 * 1024 * 1024;
export const ALLOWED_FILE_TYPES = ["application/pdf", "image/jpeg", "image/png"] as const;
```

**Vấn đề:**
- Constants inline trong barrel file thay vì file riêng
- `GENDER_OPTIONS` dùng `["male", "female", "other"]` (lowercase) nhưng `Gender` enum dùng `["MALE", "FEMALE", "OTHER"]` (uppercase) → **inconsistency trong cùng package!**

**Đề xuất:**
- Di chuyển vào file riêng: `shared/constants/upload.ts`, `shared/constants/metrics.ts`
- Thống nhất casing: dùng uppercase enum cho Gender, hoặc lowercase cho cả hai

---

## Part 2: Scripts Analysis

### 2.1 Script Inventory

| Location | Script | Lines | Purpose | Quality |
|---|---|---|---|---|
| `docker/scripts/up.sh` | 273 | Start dev environment | ✅ Excellent |
| `docker/scripts/down.sh` | 161 | Stop dev environment | ✅ Excellent |
| `docker/scripts/logs.sh` | 296 | View/filter Docker logs | ✅ Excellent |
| `docker/scripts/cleanup.sh` | 526 | Disk space cleanup | ✅ Excellent |
| `infisical/scripts/infisical.sh` | 247 | Secret management push/pull | ✅ Excellent |
| `apps/web/scripts/smoke-sitemap-urls.mjs` | 64 | Smoke test sitemap URLs | ✅ Good |
| `apps/mobile/scripts/reset-project.js` | 1 file | Reset Expo project | ✅ Good (scaffold) |

### ✅ Điểm Rất Tốt Của Scripts

| Aspect | Chi tiết |
|---|---|
| **Consistent structure** | Tất cả bash scripts dùng cùng pattern: `set -euo pipefail`, color constants, preflight checks |
| **CI mode support** | Tất cả scripts hỗ trợ `--ci` flag để disable ANSI colors |
| **Help documentation** | Mỗi script có `--help` flag với usage docs chi tiết |
| **Error handling** | Preflight Docker checks, file existence checks, confirmation prompts |
| **Portable** | `logs.sh` explicitly documents Bash 3.2+ compatibility |

### 🟡 Issue SC-1: Duplicated Utility Code Across Docker Scripts

Mỗi script trong `docker/scripts/` **duplicate** cùng một block ~30 lines:

```bash
# Duplicated in up.sh, down.sh, logs.sh, cleanup.sh:
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo_info() { echo -e "${GREEN}[OK]${NC} $1"; }
echo_warn() { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
echo_error() { echo -e "${RED}[ERR]${NC} $1" >&2; }

preflight_docker() {
    if ! command -v docker >/dev/null 2>&1; then ...
```

**Đề xuất:** Extract thành `docker/scripts/_common.sh`:

```bash
# docker/scripts/_common.sh
# shellcheck shell=bash
# Shared utilities for HealthLens Docker scripts

RED='\033[0;31m'
GREEN='\033[0;32m'
# ... colors + echo_* functions + preflight_docker + format_duration
```

Rồi mỗi script chỉ cần:
```bash
# shellcheck source=_common.sh
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"
```

Giảm ~120 lines duplicate code tổng cộng.

### ✅ Issue SC-2: Script Location Analysis

Hiện tại scripts được phân bổ:

```text
docker/scripts/         ← Docker lifecycle (4 scripts)     ✅ Hợp lý
infisical/scripts/      ← Secret management (1 script)     ✅ Hợp lý
apps/web/scripts/       ← Web smoke test (1 script)        ✅ Hợp lý
apps/mobile/scripts/    ← Mobile reset (1 script)          ✅ Hợp lý
```

**Nhận xét:** Vị trí scripts đã tốt — gần với concern của chúng. Không cần centralize vào một `scripts/` root folder vì:
- Docker scripts cần compose files path (tương đối)
- Infisical scripts cần `.infisical.json` 
- App scripts thuộc về app workspace

**Không có issue.** Tổ chức hiện tại đúng best practice.

---

## Part 3: Additional Areas Not Covered Previously

### 🟡 Issue X-1: `apps/web/src/lib/` Loose Files — Content Deep Dive

Đã flag ở report trước, giờ xem nội dung:

| File | Lines | Content | Thuộc Feature |
|---|---|---|---|
| `healthRecordHub.ts` | ~150 | Health record upload/confirm/analyze orchestration | `health-records` |
| `profileMappings.ts` | ~200 | Gender/blood-type/relationship-type label mappings + i18n | `profiles` |
| `notify.ts` | ~60 | Toast notification wrapper | `common/notifications` |

**Đề xuất cụ thể:**
```text
lib/healthRecordHub.ts    → lib/health-records/uploadOrchestrator.ts
lib/profileMappings.ts    → lib/profiles/mappings.ts
lib/notify.ts             → lib/notifications/toast.ts
```

### 🟡 Issue X-2: `apps/web/src/lib/api/routes.ts` — Unnecessary Re-export Layer

File này:
```typescript
import { ApiPaths, API_TIMEOUT } from '@healthlens/shared/constants';
export { ApiPaths, API_TIMEOUT };

// Then also creates API_ROUTES and FRONTEND_ROUTES
```

**Vấn đề:** 
- `API_ROUTES` chỉ là **partial re-export** của `ApiPaths` — thiếu `DOCUMENTS`, `INVITATIONS`, `HEALTH_RECORD_INVITATIONS`, `SHARED_PROFILES`, `REFERENCE_DATA`, `ADMIN`
- Consumers có thể import từ `@healthlens/shared/constants` trực tiếp

**Đề xuất:**
- Giữ `FRONTEND_ROUTES` (valuable — client-side route paths)
- Xóa `API_ROUTES` partial re-export, import `ApiPaths` trực tiếp từ shared
- Hoặc: nếu giữ, đảm bảo `API_ROUTES` cover tất cả `ApiPaths` sections

### 🔴 Issue X-3: Dead API Routes — `DOCUMENTS_*`

Backend `ApiRoutes.java` khai báo 4 `DOCUMENTS_*` routes:
```java
public static final String DOCUMENTS_BASE = API_V1 + "/documents";
public static final String DOCUMENT_BY_ID = DOCUMENTS_BASE + "/{id}";
public static final String DOCUMENTS_UPLOAD = DOCUMENTS_BASE + "/upload";
public static final String DOCUMENT_DOWNLOAD = DOCUMENTS_BASE + "/{id}/download";
```

Frontend `api.ts` cũng có `ApiPaths.DOCUMENTS.*` (6 routes).

**Nhưng:**
- Không có `DocumentController.java`
- Không có `DocumentService.java`
- Không có `Document.java` entity
- Không có Flyway migration cho documents table

→ **Dead code!** Có thể là planned feature chưa implement.

**Đề xuất:** Comment hoặc remove `DOCUMENTS` section từ cả `ApiRoutes.java` và `api.ts`, hoặc annotate `@planned` nếu đang trong roadmap.

### 🟡 Issue X-4: Frontend Dead Status Enums

Tương tự Documents, frontend `status.ts` khai báo:
- `SubscriptionTier` (FREE, BASIC, PREMIUM, ENTERPRISE)
- `DocumentType` (PRESCRIPTION, LAB_RESULT, ...)
- `DocumentStatus` (UPLOADED, PROCESSING, ...)

Không có backend implementation nào cho subscription hoặc documents.

**Đề xuất:** Di chuyển vào `shared/constants/_planned.ts` hoặc xóa.

### 🟡 Issue X-5: `activity/` Package — Orphaned Constants

```text
Backend:
├── activity/UserActivityEventType.java    ← 2 constants
├── service/UserActivityService.java       ← Logic
├── security/UserActivityRecordingFilter.java ← Filter
├── entity/UserActivityEvent.java          ← Entity
└── repository/UserActivityEventRepository.java ← Repository
```

Activity code bị **phân tán 5 nơi**. Package `activity/` chỉ chứa 1 file constants.

**Đề xuất (cho Feature-first migration):** Consolidate:
```text
activity/
├── UserActivityEventType.java     ← (giữ)
├── UserActivityService.java       ← (move từ service/)
├── UserActivityRecordingFilter.java ← (move từ security/)
├── entity/UserActivityEvent.java  ← (move từ entity/)
└── repository/UserActivityEventRepository.java ← (move từ repository/)
```

### ✅ Issue X-6: CI Workflow — Solid

`ci.yml` structure:
```
lint → web-build (parallel)
api-test → api-build (sequential)
```

- ✅ Proper caching (Gradle + pnpm)
- ✅ `concurrency` with cancel-in-progress
- ✅ Path-filtered triggers
- ✅ Test report upload
- ⚠️ Missing: web unit tests not in CI (only lint + type-check)

**Minor suggestion:** Add `pnpm --filter web test` step after lint.

### ✅ Issue X-7: `infisical/` Module — Well-Organized

```text
infisical/
├── ONBOARDING.md        ← Team onboarding guide (Vietnamese, 132 lines)
├── README.md            ← Quick reference
└── scripts/
    └── infisical.sh     ← Push/pull secrets (247 lines)
```

Xuất sắc:
- Self-contained module
- Detailed onboarding docs
- Script supports all environments (dev/staging-web/staging-api/prod)
- Smart empty-value filtering for push-dev
- Bootstrap command for staging folders

**Không có issue.** Tổ chức hoàn hảo cho một DevOps utility module.

---

## Tổng Hợp Issues Mới

| ID | Area | Issue | Severity | Effort |
|---|---|---|---|---|
| **C-1** | Constants | Error code systems completely diverged | 🔴 High | Medium |
| **X-3** | Constants | Dead `DOCUMENTS_*` routes (no implementation) | 🔴 High | Small |
| **C-2** | Constants | API Routes sync risk (manual, no CI check) | ⚠️ Medium | Small |
| **C-3** | Constants | Status enum drift + dead enums | 🟡 Medium | Small |
| **C-4** | Constants | Inline constants in barrel file + Gender casing inconsistency | 🟡 Medium | Small |
| **X-4** | Constants | Dead frontend enums (Subscription, Document) | 🟡 Medium | Trivial |
| **SC-1** | Scripts | ~120 lines duplicated utility code across 4 Docker scripts | 🟡 Medium | Small |
| **X-1** | Web | lib/ loose files need feature folders | 🟡 Low | Small |
| **X-2** | Web | routes.ts partial re-export layer | 🟡 Low | Small |
| **X-5** | API | activity/ package orphaned (code in 5 places) | 🟡 Low | Small |
| X-6 | CI | Missing web unit tests in CI pipeline | 🟢 Low | Trivial |

---

## Consolidated Priority Roadmap (All Reports)

### 🚨 Immediate (ngay)
1. **X-3:** Xóa/comment dead `DOCUMENTS_*` routes
2. **X-4:** Xóa dead status enums (Document, Subscription)

### 📋 Sprint này
3. **C-1:** Adopt shared error code format — unify backend `ApiErrorCode` ↔ frontend `ErrorCode`
4. **C-2:** Tạo CI script verify route sync giữa `ApiRoutes.java` ↔ `api.ts`
5. **C-4:** Fix Gender casing inconsistency, extract inline constants
6. **SC-1:** Extract `_common.sh` cho Docker scripts
7. **B-1:** Begin API package-by-feature migration (từ report 1)

### 📅 Sprint tiếp
8. **C-3:** Tạo explicit Java enums cho status values
9. **X-5:** Consolidate activity/ package (part of B-1 migration)
10. **B-2:** Tách God services (từ report 1)
11. **W-1:** Tách God page components (từ report 2)

### ♻️ Ongoing
12. **X-1, X-2:** Web lib/ cleanup
13. **X-6:** Add web unit tests to CI
