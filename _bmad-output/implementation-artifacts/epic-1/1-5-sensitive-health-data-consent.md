# Story 1.5: Thu thập consent dữ liệu y tế nhạy cảm

Status: ready-for-dev

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

- [ ] Task 1 — Backend: Consent tracking (AC: #2, #3, #4)
  - [ ] Flyway migration `V005__create_consent_logs_table.sql`
  - [ ] Schema: id, user_id, consent_version, consented_at, ip_address, user_agent, revoked_at
  - [ ] `POST /api/v1/users/me/consent` với body `{ "version": "1.0", "accepted": true }`
  - [ ] `GET /api/v1/users/me/consent` trả về consent status hiện tại
  - [ ] Thêm `CONSENT_VERSION` vào `constants.ts` — hiện tại: `"1.0"`
- [ ] Task 2 — Backend: Consent middleware (AC: #1, #3)
  - [ ] Tạo annotation `@RequiresConsent` cho các endpoints cần consent
  - [ ] AOP interceptor check consent trước khi cho phép truy cập các API liên quan đến upload/health records
- [ ] Task 3 — Web: Consent dialog/page (AC: #1, #2, #5)
  - [ ] Tạo `ConsentModal` component trong `apps/web/src/components/features/consent/`
  - [ ] Nội dung: mục đích thu thập, phạm vi sử dụng, quyền của người dùng (theo NĐ 13/2023)
  - [ ] Hai nút: "Đồng ý và tiếp tục", "Từ chối"
  - [ ] Trigger: check consent trong `authStore`, hiển thị modal khi `consentGiven === false`
  - [ ] Sau khi đồng ý: cập nhật authStore, gọi `POST /api/v1/users/me/consent`
- [ ] Task 4 — Mobile (Phase 2): Consent screen (AC: #1, #2, #5)
  - [ ] Tạo `apps/mobile/app/(auth)/consent.tsx` — full-screen (không thể bỏ qua)
  - [ ] Scroll to bottom required trước khi enable nút "Đồng ý"
  - [ ] Lưu consent status trong SecureStore
- [ ] Task 5 — Tests (AC: #1, #2, #3, #4)
  - [ ] Unit test: consent service — ghi log, check version, check existing consent

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

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
