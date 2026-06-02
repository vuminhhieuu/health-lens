# Phase 3 — Shared Foundation

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 3](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 12h
> **Goal**: Single-source-of-truth cho enum, constants, status. **Eliminate 23+ magic strings** trước refactor backend.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/03-shared-foundation`
- **PR(s)**: —

---

## ✅ Pre-flight Checklist

- [ ] Phase 2 `🟢 Done` (formatters active)
- [ ] Đã đọc `project_review.md` §3 (Constants)
- [ ] Đã đọc `deep_code_review.md` §3 (Status enum)
- [ ] Đã đọc `folder_organization_solution.md` §6 (Shared layout)
- [ ] Branch: `git checkout -b phase/03-shared-foundation main`

---

## 🎯 Tasks

### 3.1 HealthRecordStatus Enum + State Machine (3h)
- [ ] Tạo `apps/api/src/main/java/com/healthlens/api/healthrecord/domain/HealthRecordStatus.java`
- [ ] Dùng snippet [RESOURCES.md § Phase 3.1](RESOURCES.md#phase-31--healthrecordstatus-enum)
- [ ] 4 values + state machine `canTransitionTo()`
- [ ] JPA converter cho enum ↔ DB string
- [ ] Update `HealthRecord` entity dùng enum type
- [ ] Unit tests `HealthRecordStatusTest.java`:
  - Valid transitions
  - Invalid transition throws

### 3.2 Sync với TS Shared (1h)
- [ ] Tạo `packages/shared/src/domain/health-record-status.ts`:
  ```typescript
  export const HEALTH_RECORD_STATUS = {
    PROCESSING: 'processing',
    REVIEW_REQUIRED: 'review_required',
    DONE: 'done',
    OCR_FAILED: 'ocr_failed',
  } as const;
  export type HealthRecordStatus = typeof HEALTH_RECORD_STATUS[keyof typeof HEALTH_RECORD_STATUS];
  ```
- [ ] Delete old enum trong `packages/shared/constants/status.ts`
- [ ] Add comment "SYNCED — keep in sync" cả Java + TS

### 3.3 Refactor Magic Strings → Enum (2h)
- [ ] Search: `grep -rn '"processing"\|"done"\|"review_required"\|"ocr_failed"' apps/api/src/main/java/`
- [ ] Replace từng chỗ thành enum comparison
- [ ] Update FE `ReviewRecordStatus` type import từ shared
- [ ] Add Flyway `V052__verify_status_values.sql` (verify-only)

### 3.4 Unify Gender (1h)
- [ ] **DECIDE**: lowercase recommended (matches DB)
- [ ] Delete duplicate trong `shared/constants/status.ts:132`
- [ ] Canonical → `packages/shared/src/domain/gender.ts`
- [ ] Update Java entity cùng values

### 3.5 Unify Pagination (30m)
- [ ] Delete `PAGINATION` trong `packages/shared/config/env.ts:104`
- [ ] Keep + rename `API_PAGINATION` → `PAGINATION` trong `constants/api.ts:235`
- [ ] Update imports

### 3.6 TtlConstants (1h)
- [ ] Tạo `common/constants/TtlConstants.java`:
  ```java
  public static final Duration STATUS_CACHE = Duration.ofMinutes(5);
  public static final Duration PRESIGNED_UPLOAD = Duration.ofHours(1);
  public static final Duration PRESIGNED_DOWNLOAD = Duration.ofMinutes(15);
  public static final Duration LLM_EXPLANATION_CACHE = Duration.ofDays(7);
  ```
- [ ] Search & replace `Duration.ofSeconds/Minutes/Hours` inline
- [ ] Mirror `packages/shared/src/api/ttl.ts`

### 3.7 CONSENT_VERSION từ API (1h)
- [ ] Add endpoint `GET /api/consent/active-version` → `{ version: "1.0" }`
- [ ] Web fetch từ endpoint
- [ ] Remove hardcode trong `packages/shared/constants/consent.ts`
- [ ] `ConsentConstants.java` thành single source

### 3.8 Delete routes.ts wrapper (1h)
- [ ] Audit: `grep -rn "from.*lib/api/routes" apps/web/src/`
- [ ] Replace imports: `from '@/lib/api/routes'` → `from '@healthlens/shared/constants/api'`
- [ ] Delete `apps/web/src/lib/api/routes.ts`
- [ ] Verify build pass

### 3.9 Reorganize packages/shared (1.5h)
- [ ] Tạo `packages/shared/src/`
- [ ] Move: `constants/*` → `src/api/` + `src/domain/`, `schemas/*` → `src/schemas/`, `config/*` → `src/config/`, `types/*` → `src/types/`
- [ ] Update `package.json` exports + `tsconfig.json` rootDir
- [ ] Verify build cả web + shared

---

## 🧪 Verification

```bash
./gradlew :apps:api:test --tests "*HealthRecordStatus*"

# No magic strings
grep -rn '"processing"\|"review_required"\|"done"\|"ocr_failed"' apps/api/src/main/java/
# Expect: chỉ trong JPA converter + tests

# TS build
pnpm -F @healthlens/shared build
pnpm -F @healthlens/web build

# routes.ts gone
ls apps/web/src/lib/api/routes.ts  # Expect: No such file
```

---

## 🏁 Phase Completion Checklist

- [ ] 9 task `[x]`
- [ ] Zero magic status strings (grep verify)
- [ ] `HealthRecordStatus` enum used everywhere
- [ ] Java enum + TS const cùng values
- [ ] `Gender` 1 nguồn
- [ ] `PAGINATION` 1 nguồn
- [ ] `TtlConstants` extracted
- [ ] `CONSENT_VERSION` fetch từ API
- [ ] `routes.ts` deleted
- [ ] `packages/shared/src/` layout đúng
- [ ] Build pass: API + web + shared
- [ ] Update DASHBOARD

---

## 📝 Retrospective

| | Before | After |
|--|:-:|:-:|
| Magic strings | 23 | — |

### Effort
| Task | Est | Actual |
|------|:-:|:-:|
| 3.1 Enum + state | 3h | — |
| 3.2 TS sync | 1h | — |
| 3.3 Replace | 2h | — |
| 3.4 Gender | 1h | — |
| 3.5 Pagination | 30m | — |
| 3.6 TTL | 1h | — |
| 3.7 Consent | 1h | — |
| 3.8 routes.ts | 1h | — |
| 3.9 Reorganize | 1.5h | — |
| **Total** | **12h** | — |
