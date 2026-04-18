# Story 1.5: Thu thập consent dữ liệu y tế nhạy cảm

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Điều Khoản Sử Dụng - HealthLens | `projects/578519912546445367/screens/fb972f816f334c73b426f17cd20bd58d` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2ZlZDY2YzhjN2JkYTQzYjc4OTA1MzhkMzIyZDZiZWMxEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ug2H0x6y0hlGTZnBm7mZeMpPT8cCuNDW-h07DYmSRxTosER51X9siB6CAQ5rqfxWmrPcFTb3NKP4hbsofYxhfvnlesQKBg3AALZVO8zuFA-LcJqTlpkZ4F2P0J6YUoRhDt6BZ0r33Q3Y47kQ7usKjEHPCP9GRR5VkFhFopCmOEuXAl3Q0_-b0rCVnrg23dHv5U-WBOahKiBE2EPZYyFzhmwAOMnoNC7Be9NrmZcLg6UTj72kBi_Dl-tXw) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want xác nhận đồng ý xử lý dữ liệu y tế trước khi upload,
so that tôi hiểu rõ phạm vi sử dụng dữ liệu và quyền của mình.

## Acceptance Criteria

1. **Given** người dùng đăng nhập lần đầu hoặc chưa consent, **When** mở tính năng upload/kết quả, **Then** hệ thống chặn và hiển thị màn hình consent rõ ràng.
2. **Given** màn hình consent, **When** người dùng đọc và nhấn "Đồng ý", **Then** hệ thống ghi log consent với timestamp, version nội dung, user ID, IP.
3. **Given** người dùng đã consent, **When** truy cập tính năng upload, **Then** không hiển thị lại màn hình consent.
4. **Given** nội dung consent thay đổi (version mới), **When** user đăng nhập, **Then** yêu cầu consent lại.
5. **Given** người dùng nhấn "Từ chối", **When** hệ thống xử lý, **Then** không lưu consent và người dùng được redirect về trang chủ với thông báo giải thích.

## Tasks / Subtasks

- [x] Task 1 — Backend: Consent tracking (AC: #2, #3, #4)
  - [x] Flyway migration `V005__create_consent_logs_table.sql`
  - [x] Schema: id, user_id, consent_version, consented_at, ip_address, user_agent, revoked_at
  - [x] `POST /api/v1/users/me/consent` với body `{ "version": "1.0", "accepted": true }`
  - [x] `GET /api/v1/users/me/consent` trả về consent status hiện tại
  - [x] Thêm `CONSENT_VERSION` vào `constants.ts` — hiện tại: `"1.0"`
- [x] Task 2 — Backend: Consent middleware (AC: #1, #3)
  - [x] Tạo annotation `@RequiresConsent` cho các endpoints cần consent
  - [x] AOP interceptor check consent trước khi cho phép truy cập các API liên quan đến upload/health records
- [x] Task 3 — Web: Consent dialog/page (AC: #1, #2, #5)
  - [x] Tạo `ConsentModal` component trong `apps/web/src/components/features/consent/`
  - [x] Nội dung: mục đích thu thập, phạm vi sử dụng, quyền của người dùng (theo NĐ 13/2023)
  - [x] Hai nút: "Đồng ý và tiếp tục", "Từ chối"
  - [x] Trigger: check consent trong `authStore`, hiển thị modal khi `consentGiven === false`
  - [x] Sau khi đồng ý: cập nhật authStore, gọi `POST /api/v1/users/me/consent`
- [ ] Task 4 — Mobile (Phase 2): Consent screen (AC: #1, #2, #5)
  - [ ] Tạo `apps/mobile/app/(auth)/consent.tsx` — full-screen (không thể bỏ qua)
  - [ ] Scroll to bottom required trước khi enable nút "Đồng ý"
  - [ ] Lưu consent status trong SecureStore
- [x] Task 5 — Tests (AC: #1, #2, #3, #4)
  - [x] Unit test: consent service — ghi log, check version, check existing consent
  - [x] Controller test: `/auth/refresh` trả về `accessToken + user` (đồng bộ với frontend bootstrap)
- [x] Task 6 — Web: Ổn định session & consent sau reload (AC: #1, #3)
  - [x] Hook `useAuthBootstrap` đọc `POST /auth/refresh` + fallback dùng `user` hiện tại nếu backend chỉ trả `accessToken`
  - [x] Guard layout `(dashboard)` redirect sang `/login` qua `useEffect` sau khi bootstrap xong (tránh flash và loop)
  - [x] `apiClient` auto-attach access token và tự refresh khi gặp 401 (trừ các auth endpoints)
- [x] Task 7 — DevOps: Docker scripts & rebuild (support dev flow cho story)
  - [x] `docker/scripts/up.sh`: thêm các option `--rebuild-api`, `--rebuild-web`, `--rebuild-ocr`, `--foreground`; mặc định `up -d` và in URL service
  - [x] `docker/scripts/down.sh`: dọn orphan containers `api-dev`, `web-dev`, `ocr`
- [x] Task 8 — Backend: Email xác nhận tài khoản dùng SMTP thật (support AC login/consent)
  - [x] `application.yml` profile `dev`: cấu hình `spring.mail` → host `smtp.resend.com`, port `587`, `starttls` + `auth` bật sẵn
  - [x] `docker/compose.dev.yml`: bỏ Mailhog, truyền `MAIL_HOST/PORT/USERNAME` từ `.env` và để `MAIL_PASSWORD` lấy trực tiếp từ env (không override rỗng)
  - [x] `.env` dev: chuẩn hóa block mail Resend (SMTP), cập nhật `WEB_VERIFY_URL` trỏ đúng route verify email trên web

## Dev Notes

### Database Schema

```sql
-- V005__create_consent_logs_table.sql
CREATE TABLE consent_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_version VARCHAR(20) NOT NULL,
    consented_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45),
    user_agent TEXT,
    revoked_at TIMESTAMP
);
CREATE INDEX idx_consent_logs_user_id ON consent_logs(user_id);
```

### Consent Version

- Hiện tại: version `"1.0"` (hardcode trong `packages/shared/constants/index.ts`)
- Khi nội dung thay đổi: tăng version → user phải consent lại

### Nội Dung Consent (Tiếng Việt, NĐ 13/2023)

```
Chúng tôi thu thập dữ liệu y tế của bạn (kết quả xét nghiệm, chỉ số sức khỏe)
để cung cấp dịch vụ giải thích và theo dõi sức khỏe.

Dữ liệu của bạn:
• Được mã hóa AES-256 và chỉ bạn mới có thể truy cập
• Không được chia sẻ với bên thứ ba mà không có sự đồng ý của bạn
• Bạn có quyền yêu cầu xóa toàn bộ dữ liệu bất kỳ lúc nào

Bằng cách nhấn "Đồng ý", bạn xác nhận đã đọc và đồng ý với
Chính sách Bảo mật và Điều khoản Sử dụng của HealthLens.
```

### Auth Store — Thêm Consent State

```typescript
// authStore.ts — thêm field
interface AuthState {
  // ... existing fields
  consentGiven: boolean;
  consentVersion: string | null;
  setConsent: (version: string) => void;
}
```

### References

- [Source: architecture.md#Mã-Hóa-Dữ-Liệu]
- [Source: epics.md#Story-1.5]
- [Source: prd.md#FR8]

## Review Findings

**Code Review Date:** 2026-04-17  
**Review Mode:** Full (with spec + parallel layers)  
**Overall Status:** ⚠️ CONDITIONAL APPROVAL — Implementation sound but critical issues must be resolved

### DECISION REQUIRED (Architectural — Blocking)

- [ ] [Review][Decision] Backend consent version hardcoded; frontend cannot sync dynamically — Frontend imports `CONSENT_VERSION` from shared package; backend hardcodes "1.0" in `ConsentController`. If backend increments to "2.0", frontend must be redeployed. Violates AC #4. **Options:** (A) Add dynamic version endpoint, (B) Accept version drift + coordinated deployments, (C) Move to database config table. **Recommendation:** Option A.

- [ ] [Review][Decision] @RequiresConsent not wired to actual endpoints — Annotation and aspect defined but not applied to upload/health endpoints. AC #1 requires blocking access. **Action:** Identify and decorate all sensitive endpoints with `@RequiresConsent`.

- [ ] [Review][Decision] Consent status not bootstrapped — Task 6 spec implies consent in refresh response. Currently only `{ accessToken, user }` returned; frontend must call separate endpoint. **Options:** (A) Add `consentGiven`, `consentVersion` to refresh response, (B) Accept two-call pattern, (C) Embed in JWT. **Recommendation:** Option A.

### PATCH (Security/Compliance — High Priority)

- [ ] [Review][Patch] Race condition in concurrent accept/reject [ConsentService.java:44-54] — `recordConsent()` lacks transaction isolation. Concurrent requests could create duplicate active records. **Fix:** Add `@Transactional(isolation = Isolation.SERIALIZABLE)` or pessimistic locking.

- [ ] [Review][Patch] X-Forwarded-For header not validated; IP spoofing risk [ConsentController.java:38-42] — Header taken as-is; doesn't parse proxy chains; no format validation. Attacker can spoof IP; audit trail corrupted. **Fix:** (A) Parse first IP via split, (B) Validate IP format, (C) Fallback to getRemoteAddr() if invalid.

- [ ] [Review][Patch] Silent exception swallowing in ConsentAspect [ConsentAspect.java:28-40] — Non-UUID principal silently bypasses consent check. Attacker can craft token to bypass. **Fix:** Either throw `ConsentRequiredException`, log warning, or explicitly deny.

- [ ] [Review][Patch] User not found returns 500 instead of 404 [ConsentService.java:44] — `orElseThrow(() => new IllegalArgumentException(...))` maps to 500. Semantically should be 404. **Fix:** Create `UserNotFoundException` or use `EntityNotFoundException` mapped to 404.

- [ ] [Review][Patch] Authorization bypass: principal not validated [ConsentController.java:31, 36] — Endpoint trusts `authentication.getPrincipal()` without verifying against token claims. User could request another user's consent. **Fix:** Add explicit validation or document Spring Security JWT validation assumption.

- [ ] [Review][Patch] Cascade delete on ConsentLog destroys audit trail [V005__create_consent_logs_table.sql:6] — User deletion cascades to consent_logs. Regulatory compliance requires immutable audit trail. **Fix:** Change to `ON DELETE RESTRICT` or archive logs before deletion.

### PATCH (Functional — Medium Priority)

- [ ] [Review][Patch] ConsentResponse missing timestamp field [ConsentResponse.java] — Response should include `consentedAt` timestamp. Frontend has no way to detect staleness. **Fix:** Add `Instant consentedAt` field and populate from `ConsentLog`.

- [ ] [Review][Patch] Consent rejection silent; no user feedback [ConsentModal.tsx:34-39] — Spec AC #5 requires explanatory message. Frontend redirects silently. **Fix:** Add toast/alert explaining rejection before logout.

- [ ] [Review][Patch] apiClient error handling too broad [ConsentModal.tsx:29-30] — All errors (network, 500, 401) result in logout. Should differentiate auth vs transient errors. **Fix:** Check error status; only logout on 401/403; else show retry toast.

- [ ] [Review][Patch] ConsentAspect lacks logging [ConsentAspect.java:26-29] — Silent return for anonymous/invalid principals; hard to debug. **Fix:** Add SLF4J debug/warn logging.

- [ ] [Review][Patch] No integration tests for AOP aspect [Test coverage gap] — Aspect interception not tested. Method with `@RequiresConsent` won't actually be blocked without integration test. **Fix:** Add `ConsentAspectIntegrationTest` using MockMvc.

### PATCH (Code Quality — Low Priority)

- [ ] [Review][Patch] Missing @Documented annotation on @RequiresConsent [RequiresConsent.java] — Custom annotation should have `@Documented` for IDE/Javadoc support. **Fix:** Add `@Documented`.

- [ ] [Review][Patch] Email credentials in docker compose [docker/compose.dev.yml] — `MAIL_PASSWORD` env var visible in `docker ps` output. Security anti-pattern. **Fix:** Use Docker secrets or mount `.env` as read-only file.

### DEFER (Pre-Existing or Low Impact)

- [x] [Review][Defer] ConsentModal state reset on refresh [ConsentModal.tsx:11-13] — Local useState lost on page refresh. Low priority; component short-lived. Could persist to localStorage if needed.

- [x] [Review][Defer] Version mismatch check inherent to architecture [ConsentModal.tsx:18] — Resolved by decision #1 (dynamic version endpoint).

- [x] [Review][Defer] Idempotency: repeated rejections create duplicate revokes [ConsentService.java:50-54] — Unnecessary saves only; not functional bug. Could optimize by checking `revokedAt` first.

---

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
