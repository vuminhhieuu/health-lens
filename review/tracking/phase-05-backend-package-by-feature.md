# Phase 5 — Backend Package-by-Feature

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 5](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 40h (tuần 6–8)
> **Goal**: Migrate `apps/api` từ hybrid → package-by-feature. **Mỗi feature 1 PR**.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/05-package-by-feature/{feature}`
- **PR(s)**: — (≥10 PRs)

---

## ✅ Pre-flight Checklist

- [ ] Phase 4 `🟢 Done`
- [ ] Đã đọc `folder_organization_solution.md` §1, §2
- [ ] Đã đọc `deep_code_review.md` §1.1 (split plan)
- [ ] Coverage baseline measured: `./gradlew jacocoTestReport` → ghi % vào DASHBOARD
- [ ] **Strategy**: keep old method delegate trong service cũ → migrate caller dần → remove old

---

## 🎯 Phase 5A — `healthrecord/` (Tuần 6, 14h)

### 5A.1 Skeleton (30m)
- [ ] Tạo `apps/api/src/main/java/com/healthlens/api/healthrecord/`:
  - `api/`, `service/`, `domain/`, `repository/`, `dto/{request,response}/`, `mapper/`, `exception/`

### 5A.2 Move entities + repos (1h)
- [ ] Move 4 entities → `domain/`
- [ ] Move 4 repos → `repository/`
- [ ] Update imports (IDE refactor)

### 5A.3 Move DTOs (1h)
- [ ] 4 request DTOs → `dto/request/`
- [ ] 12 response DTOs → `dto/response/`

### 5A.4 Split HealthRecordService (8h) ⚠️ CRITICAL
- [ ] Sub-step 1: `HealthRecordUploadService` skeleton (copy methods)
- [ ] Sub-step 2: `HealthRecordQueryService`
- [ ] Sub-step 3: `HealthRecordConfirmService`
- [ ] Sub-step 4: `HealthRecordExportService`
- [ ] Sub-step 5: `HealthRecordAccessControl`
- [ ] Sub-step 6: Old `HealthRecordService` → facade gọi 5 service mới
- [ ] Sub-step 7: Migrate caller dùng service mới
- [ ] Sub-step 8: Delete facade
- **Each sub-step = own PR** — total 8 PRs

### 5A.5 MapStruct mapper (2h)
- [ ] Add deps
- [ ] Tạo `HealthRecordMapper.java`
- [ ] Replace manual mapping

### 5A.6 Fix soft-delete leak methods (30m)
- [ ] Verify line 34, 36, 38 tự động filter (sau Phase 0 `@SQLDelete` + `@Where`)
- [ ] Nếu vẫn leak (raw query), explicit filter

### 5A.7 Update tests (3h)
- [ ] Mirror structure
- [ ] Coverage không tụt > 5%

---

## 🎯 Phase 5B — `ocr/` + `llm/` (Tuần 7, 14h)

### 5B.1 Extract OCR inner DTOs (1h)
- [ ] Move `EasyOcrRequest`, `EasyOcrResponse`, `OcrExtractionResult` → `ocr/dto/{request,response}/`
- [ ] **Fix dependency reversal**: `EasyOcrProviderAdapter` không còn import `OcrService.EasyOcrRequest`

### 5B.2 Split OcrService (6h)
- [ ] `OcrPipelineService` (pipeline orchestration)
- [ ] `OcrMetricExtractor` (LLM parsing)
- [ ] `OpenRouterLlmClient` (HTTP adapter)
- [ ] Keep facade, migrate, remove

### 5B.3 Move OCR providers (1h)
- [ ] Move 4 provider implementations từ `service/` → `ocr/provider/`

### 5B.4 LlmProvider interface (4h)
- [ ] `llm/provider/LlmProvider.java` interface
- [ ] `SpringAiLlmProvider`, `OpenRouterLlmProvider` adapters
- [ ] `LlmProviderChain` (priority-ordered fallback)
- [ ] Update `LlmService` dùng chain

### 5B.5 Update registry (1h)
- [ ] `OcrProviderRegistry` dùng provider chain

### 5B.6 Tests cho chain (1h)
- [ ] Mock interface, test fallback

---

## 🎯 Phase 5C — Other features (Tuần 8, 12h)

### 5C.1 Migrate per feature (8h)
- [ ] `auth/` (1h)
- [ ] `user/` (1h)
- [ ] `profile/` (1h)
- [ ] `notification/` (1h)
- [ ] `rag/` (1.5h)
- [ ] `reference/` (1h)
- [ ] `analytics/` (30m)
- [ ] `admin/` (1h) — sub-features: `admin/{auth,analytics,audit,reference,rag}/`

### 5C.2 StoragePort interface (2h)
- [ ] `storage/StoragePort.java` interface
- [ ] `MinioStorageAdapter` default
- [ ] (Optional) `AwsS3StorageAdapter`

### 5C.3 EmailPort interface (1h)
- [ ] `notification/port/EmailPort.java`
- [ ] `SmtpEmailAdapter`

### 5C.4 Cleanup common/ (1h)
- [ ] `common/` chỉ chứa cross-cutting
- [ ] Move feature-specific code

---

## 🧪 Verification (sau mỗi feature)

```bash
./gradlew :apps:api:build
./gradlew :apps:api:test :apps:api:jacocoTestReport
# Compare vs baseline

# No circular
./gradlew :apps:api:classes

# Smoke E2E (sau toàn bộ)
pnpm run test:e2e:smoke
```

---

## 🏁 Phase Completion Checklist

- [ ] Phase 5A — `healthrecord/` migrated (10+ PRs)
- [ ] `HealthRecordService` ≤ 400 dòng (từ 1,710)
- [ ] Phase 5B — `ocr/` + `llm/` migrated
- [ ] Circular dep `EasyOcrProviderAdapter` resolved
- [ ] LlmProvider interface used
- [ ] Phase 5C — 8 features migrated
- [ ] StoragePort + EmailPort defined
- [ ] `common/` cleaned
- [ ] Coverage ≥ Phase 4 baseline (allow -5%)
- [ ] Smoke E2E pass
- [ ] Decision gate "Coverage không tụt > 5%" ✅
- [ ] Update DASHBOARD

---

## 📝 Retrospective

### Metrics
| | Before | After |
|--|:-:|:-:|
| Files in `service/` flat | 60+ | — |
| HealthRecordService LoC | 1,710 | — |
| Service interfaces | 1 | 5+ |

### Effort
| Sub-phase | Est | Actual |
|-----------|:-:|:-:|
| 5A healthrecord | 14h | — |
| 5B ocr + llm | 14h | — |
| 5C others | 12h | — |
| **Total** | **40h** | — |
