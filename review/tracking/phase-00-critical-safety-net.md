# Phase 0 — Critical Safety Net

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 0](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 14h
> **Goal**: Đóng kill-switch risk + cleanup. Sau phase này, app production-safe về licensing, runtime stub, security headers.

---

## 📊 Status

- **Status**: 🟡 In Progress
- **Branch**: `phase/00-safety-net`
- **Started**: 2026-06-02
- **Done**: —
- **PR(s)**: —

---

## ✅ Pre-flight Checklist

- [x] Đã đọc `INTEGRATED_MASTER_PLAN.md` Phase 0
- [x] Đã đọc `runtime_security_and_correctness_audit.md` §3, §4, §6, §8, §9
- [x] Đã đọc `final_misc_audit.md` §3.4 (Arial.ttf)
- [x] Đã đọc `review_gaps.md` §1.2 (AwsTextract)
- [x] Tests baseline xanh: `apps/api/gradlew test && pnpm -F web test`
- [x] Tag backup: `git tag pre-refactor-2026-06-01`
- [x] Branch: `git checkout -b phase/00-safety-net main`

---

## 🎯 Tasks

### 0.A — Runtime Security Critical (3h)

#### 0.A.1 — HTTP Security Headers (30m)
- [x] Mở `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`
- [x] Thêm `.headers(...)` trước `return http.build()` (sau dòng ~100)
- [x] Apply 6 headers theo [RESOURCES.md § Phase 0.A.1](RESOURCES.md#phase-0a1--http-security-headers)
- [x] Adjust CSP `connect-src` cho domain thật
- [x] Verify: `curl -sI http://localhost:8080/actuator/health` → 6 headers
- [x] Test: `SecurityConfigHeadersIT.java`
- **PR**: —

#### 0.A.2 — Soft Delete Annotation (30m)
- [x] **DECIDE**: explicit repository filters; no global Hibernate restriction — log [ADR-002](DECISIONS.md#adr-002)
- [x] Keep `@SQLDelete` for entity-level soft delete
- [x] Filter 3 leak methods explicitly with `deletedAt IS NULL`
- [x] Test soft delete leak methods không trả deleted rows
- [x] Verify purge/right-to-delete bulk delete still sees soft-deleted rows
- [x] `HealthRecordSoftDeleteIT.java`
- **PR**: —

#### 0.A.3 — Redis Cache Purge (1h)
- [x] Mở `DataDeletionService.java`, inject `RedisTemplate`
- [x] Method `purgeUserCache(UUID userId)`:
  ```java
  scanAndDelete("llm:explanation:" + userId + ":*");
  scanAndDelete("user:session:" + userId + ":*");
  scanAndDelete("rate-limit:*:" + userId);
  ```
- [x] Gọi trong `performDeletion()` sau anonymize user (line ~396)
- [x] Test: tạo user → cache → delete → verify empty
- **PR**: —

#### 0.A.4 — Side Effect khỏi readOnly tx (30m)
- [x] Mở `FollowUpReminderService.java`
- [x] Tách `dispatchDueReminderEmailsIfEnabled` (line 71) ra method riêng
- [x] Method mới: `@Transactional` non-readonly _(REQUIRES_NEW vẫn dùng cho `retryDueReminderEmailsForUser`)_
- [x] Caller mới: scheduler hoặc dedicated endpoint _(`FollowUpReminderController.list` trước khi gọi service.list — `NotificationInboxService` / `UserNotificationPreferenceService` đã có sẵn)_
- [x] Update tests
- **PR**: —

#### 0.A.5 — OcrController DTO (15m)
- [x] Tạo `OcrExtractRequest(@NotBlank @URL String imageUrl)` record
- [x] Update controller (line 51): `@RequestBody @Valid OcrExtractRequest`
- [x] Remove manual null check
- **PR**: —

---

### 0.B — Licensing, Stubs, Config (4.5h)

#### 0.B.1 — Replace Arial.ttf (1h)
- [x] Download DejaVu Sans hoặc Noto Sans (Vietnamese subset)
- [x] Replace `apps/api/src/main/resources/fonts/Arial.ttf`
- [x] Search: `grep -r "Arial.ttf" apps/api/src/main/java/`
- [x] Update font load path trong `HealthRecordPdfService.java`
- [ ] Generate sample PDF, verify Vietnamese render đúng
- **PR**: —

#### 0.B.2 — Fix/Remove AwsTextractClient (2h)
- [x] **DECIDE**: implement vs remove vs flag — [ADR-001](DECISIONS.md#adr-001)
- [x] Nếu remove: `OcrProviderRegistry.java` — xóa Textract case
- [x] Update `application.yml` — remove `ocr.provider.textract.*`
- [x] Remove `AwsTextractClient.java` + `TextractOcrProvider.java`
- [x] Verify no compile error
- **PR**: —

#### 0.B.3 — DevController Profile Gating (30m)
- [x] Verify `@Profile({"docker", "dev"})` → đổi thành `@Profile("dev")` only
- [x] Check `application-docker.yml`, prod KHÔNG include `docker` profile
- [x] Verify production compose sets `SPRING_PROFILES_ACTIVE=production`
- [ ] Test: `curl staging-api/dev/health-debug` → 404
- **PR**: —

#### 0.B.4 — Redis Cache TTL Fix (30m)
- [x] `HealthRecordService.java:322`: `Duration.ofSeconds(5)` → `Duration.ofMinutes(5)`
- [x] **Critical**: Bỏ presigned URL khỏi cached response
- [x] Tách response: metadata (cache) + URL (fresh)
- [x] Test: 2 lần liên tiếp → URL khác nhau
- **PR**: —

#### 0.B.5 — File Size Constant (30m)
- [x] **DECIDE**: chốt 20MB (match upload hiện tại)
- [x] Update `packages/shared/constants/index.ts` → `UPLOAD_MAX_SIZE_BYTES = 20 * 1024 * 1024`
- [x] Update `packages/shared/config/env.ts` → `OCR_CONFIG.MAX_FILE_SIZE` cùng
- [x] Update `services/ocr-service/app.py` → `MAX_IMAGE_SIZE_BYTES = 20 * 1024 * 1024`
- [x] Add comment "SINGLE SOURCE"
- **PR**: —

---

### 0.C — Cleanup (1.5h)

- [x] **0.C.1** Xóa `apps/web/apps/` + `apps/web/packages/` — 5m
- [x] **0.C.2** Remove `apps/mobile` as active workspace module — 5m
- [ ] **0.C.3** Move `test_ssrf_protection.py` → `services/ocr-service/tests/` — 10m _(blocker: file không tồn tại trong worktree)_
- [x] **0.C.4** Xóa 5 Next.js SVGs trong `apps/web/public/` — 5m
- [x] **0.C.5** Xóa expo template assets (`expo-badge*.png`, `react-logo*.png`) — 10m
- [x] **0.C.6** Xóa `.env.{production,staging,staging.api}` — 5m
- [x] **0.C.7** Verify `lucide-react@^1.7.0` version trong `apps/web/package.json` — 15m
- [ ] **0.C.8** `git tag -a pre-refactor-2026-06-01` + push — 30m _(local tag created; push intentionally not run)_
- [x] Remove mobile workspace/CI/docs active references

---

## 🧪 Verification

```bash
# Build + test
./gradlew :apps:api:clean :apps:api:test
pnpm -F @healthlens/web build
cd services/ocr-service && python -m pytest tests/

# Security headers
curl -sI http://localhost:8080/actuator/health | grep -E \
  "Strict-Transport-Security|Content-Security-Policy|X-Frame-Options|X-Content-Type-Options|Referrer-Policy|Permissions-Policy"
# Expect: 6 lines

# Soft delete
psql -c "UPDATE health_records SET deleted_at = NOW() WHERE id = 'test-id'"
curl /api/health-records/test-id  # Expect: 404

# DevController gated
curl staging-api/dev/debug  # Expect: 404
```

Automated results:
- [x] `cd apps/api && ./gradlew clean test`
- [x] `pnpm -F web build` (`@healthlens/web` filter does not match package name)
- [x] `pnpm -F web test`
- [ ] `cd services/ocr-service && python3 -m pytest tests/` _(blocked: `pytest` not installed; `tests/` empty)_

### Manual
- [ ] PDF render đúng Vietnamese (DejaVu)
- [x] `find . -name "Arial.ttf"` → empty
- [ ] Login flow OK
- [ ] Upload flow OK
- [ ] Account deletion → Redis cleared

---

## 🏁 Phase Completion Checklist

- [ ] Tất cả 17 task `[x]` hoặc `🚫 SKIPPED`
- [ ] Tests xanh
- [ ] 6 security headers verified
- [ ] Smoke tests pass
- [ ] PR merged
- [ ] Git tag pushed
- [ ] ADR-001 chốt
- [ ] ADR-002 chốt
- [ ] Update DASHBOARD: Phase 0 → 🟢 Done
- [ ] Stakeholder smoke test staging pass

---

## 📝 Retrospective

| Section | Est | Actual | Variance |
|---------|:-:|:-:|:-:|
| 0.A | 3h | — | — |
| 0.B | 4.5h | — | — |
| 0.C | 1.5h | — | — |
| Buffer | 5h | — | — |
| **Total** | **14h** | — | — |

### Notes
- _TBD_
