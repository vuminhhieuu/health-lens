# Phase 0 — Critical Safety Net

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 0](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 14h
> **Goal**: Đóng kill-switch risk + cleanup. Sau phase này, app production-safe về licensing, runtime stub, security headers.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/00-safety-net`
- **Started**: —
- **Done**: —
- **PR(s)**: —

---

## ✅ Pre-flight Checklist

- [ ] Đã đọc `INTEGRATED_MASTER_PLAN.md` Phase 0
- [ ] Đã đọc `runtime_security_and_correctness_audit.md` §3, §4, §6, §8, §9
- [ ] Đã đọc `final_misc_audit.md` §3.4 (Arial.ttf)
- [ ] Đã đọc `review_gaps.md` §1.2 (AwsTextract)
- [ ] Tests baseline xanh: `./gradlew :apps:api:test && pnpm -F web test`
- [ ] Tag backup: `git tag pre-refactor-2026-06-01`
- [ ] Branch: `git checkout -b phase/00-safety-net main`

---

## 🎯 Tasks

### 0.A — Runtime Security Critical (3h)

#### 0.A.1 — HTTP Security Headers (30m)
- [ ] Mở `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`
- [ ] Thêm `.headers(...)` trước `return http.build()` (sau dòng ~100)
- [ ] Apply 6 headers theo [RESOURCES.md § Phase 0.A.1](RESOURCES.md#phase-0a1--http-security-headers)
- [ ] Adjust CSP `connect-src` cho domain thật
- [ ] Verify: `curl -sI http://localhost:8080/actuator/health` → 6 headers
- [ ] Test: `SecurityConfigHeadersIT.java`
- **PR**: —

#### 0.A.2 — Soft Delete Annotation (30m)
- [ ] **DECIDE**: Option A (`@SQLDelete`+`@Where`) vs B (manual) — log [ADR-002](DECISIONS.md#adr-002)
- [ ] Nếu A: add 2 annotation vào `HealthRecord.java`
- [ ] Imports: `org.hibernate.annotations.SQLDelete` + `Where`
- [ ] Test soft delete, verify `findAll()` không trả
- [ ] Verify 3 leak methods filter tự động
- [ ] `HealthRecordSoftDeleteIT.java`
- **PR**: —

#### 0.A.3 — Redis Cache Purge (1h)
- [ ] Mở `DataDeletionService.java`, inject `RedisTemplate`
- [ ] Method `purgeUserCache(UUID userId)`:
  ```java
  Set<String> keys = new HashSet<>();
  keys.addAll(redisTemplate.keys("llm:explanation:" + userId + ":*"));
  keys.addAll(redisTemplate.keys("user:session:" + userId + ":*"));
  keys.addAll(redisTemplate.keys("rate-limit:*:" + userId));
  if (!keys.isEmpty()) redisTemplate.delete(keys);
  ```
- [ ] Gọi trong `performDeletion()` sau anonymize user (line ~396)
- [ ] Test: tạo user → cache → delete → verify empty
- **PR**: —

#### 0.A.4 — Side Effect khỏi readOnly tx (30m)
- [ ] Mở `FollowUpReminderService.java`
- [ ] Tách `dispatchDueReminderEmailsIfEnabled` (line 71) ra method riêng
- [ ] Method mới: `@Transactional` non-readonly
- [ ] Caller mới: scheduler hoặc dedicated endpoint
- [ ] Update tests
- **PR**: —

#### 0.A.5 — OcrController DTO (15m)
- [ ] Tạo `OcrExtractRequest(@NotBlank @URL String imageUrl)` record
- [ ] Update controller (line 51): `@RequestBody @Valid OcrExtractRequest`
- [ ] Remove manual null check
- **PR**: —

---

### 0.B — Licensing, Stubs, Config (4.5h)

#### 0.B.1 — Replace Arial.ttf (1h)
- [ ] Download DejaVu Sans hoặc Noto Sans (Vietnamese subset)
- [ ] Replace `apps/api/src/main/resources/fonts/Arial.ttf`
- [ ] Search: `grep -r "Arial.ttf" apps/api/src/main/java/`
- [ ] Update font load path trong `HealthRecordPdfService.java`
- [ ] Generate sample PDF, verify Vietnamese render đúng
- **PR**: —

#### 0.B.2 — Fix/Remove AwsTextractClient (2h)
- [ ] **DECIDE**: implement vs remove vs flag — [ADR-001](DECISIONS.md#adr-001)
- [ ] Nếu remove: `OcrProviderRegistry.java` — xóa Textract case
- [ ] Update `application.yml` — remove `ocr.provider.textract.*`
- [ ] Remove `AwsTextractClient.java` + `TextractOcrProvider.java`
- [ ] Verify no compile error
- **PR**: —

#### 0.B.3 — DevController Profile Gating (30m)
- [ ] Verify `@Profile({"docker", "dev"})` → đổi thành `@Profile("dev")` only
- [ ] Check `application-docker.yml`, prod KHÔNG include `docker` profile
- [ ] Verify `deploy.yml` set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Test: `curl staging-api/dev/health-debug` → 404
- **PR**: —

#### 0.B.4 — Redis Cache TTL Fix (30m)
- [ ] `HealthRecordService.java:322`: `Duration.ofSeconds(5)` → `Duration.ofMinutes(5)`
- [ ] **Critical**: Bỏ presigned URL khỏi cached response
- [ ] Tách response: metadata (cache) + URL (fresh)
- [ ] Test: 2 lần liên tiếp → URL khác nhau
- **PR**: —

#### 0.B.5 — File Size Constant (30m)
- [ ] **DECIDE**: chốt 20MB (match upload hiện tại)
- [ ] Update `packages/shared/constants/index.ts` → `UPLOAD_MAX_SIZE_BYTES = 20 * 1024 * 1024`
- [ ] Update `packages/shared/config/env.ts` → `OCR_CONFIG.MAX_FILE_SIZE` cùng
- [ ] Update `services/ocr-service/app.py` → `MAX_IMAGE_SIZE_BYTES = 20 * 1024 * 1024`
- [ ] Add comment "SINGLE SOURCE"
- **PR**: —

---

### 0.C — Cleanup (1.5h)

- [ ] **0.C.1** Xóa `apps/web/apps/` + `apps/web/packages/` — 5m
- [ ] **0.C.2** Xóa 3 barrel `apps/mobile/{components,hooks,stores}/index.ts` — 5m
- [ ] **0.C.3** Move `test_ssrf_protection.py` → `services/ocr-service/tests/` — 10m
- [ ] **0.C.4** Xóa 5 Next.js SVGs trong `apps/web/public/` — 5m
- [ ] **0.C.5** Xóa expo template assets (`expo-badge*.png`, `react-logo*.png`) — 10m
- [ ] **0.C.6** Xóa `.env.{production,staging,staging.api}` — 5m
- [ ] **0.C.7** Verify `lucide-react@^1.7.0` version trong `apps/web/package.json` — 15m
- [ ] **0.C.8** `git tag -a pre-refactor-2026-06-01` + push — 30m

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

### Manual
- [ ] PDF render đúng Vietnamese (DejaVu)
- [ ] `find . -name "Arial.ttf"` → empty
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
