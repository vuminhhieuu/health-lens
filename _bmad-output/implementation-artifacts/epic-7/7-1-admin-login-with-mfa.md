# Story 7.1: Admin đăng nhập vào panel với MFA bắt buộc

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Xác thực hai yếu tố - HealthLens | `projects/578519912546445367/screens/4624e45abb214f2ea30b2d010918f9f7` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzA3NjA3ZmIxNWM2NjQ4MTJiY2ZkYTQ1Mjk3MmQ4M2NlEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ui8SOp_2ZlHmJDbDvyD3ZIiM1jm4bqlvwkqdNKGP1-tO5wsFTuICrZQKmS_9klSjN9ROg6kOjbNOvBJK-podm9Aduajay_ED0tKjMe4dzQRS17BB1E-GFa9VWwDD1FNKmmbX2_o1l-OzP8IPMqVhla1rz3Eb27EzujuGLr8KCIlKu3p9-ko3MiN2rmMBJLdJOxV4R1PZAVfHZ0iU8ymMvzdjnReqfhDjed7v9IHfRS8FYcFYMqvRJ6s) |
| Thiết lập MFA - Admin HealthLens | `projects/578519912546445367/screens/07e884d40df14c2b8558038dabda2e56` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2Q1NmU1ODhkNWI1ZjQ1NTQ4NDgyNWI5ODI1ZDE1OTA1EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uixK77wAO91Y_axLkL__qICKAVQOV61KD3yG2CZbZRzjnI-jRZwOR2FcaCMlKnf0vfX2nYXeP9Yz4WALCi3gyXDTGdf13QwvlWRSTGwk_aIx_7FTP0vhFxFDijw8sfCYcFueSIBbu4DmWdv-KxfhGKrhFlyDe5iEjFrHPu7CoCQEaeGOBi7idVO5bftMnqvaj9I7X_muN4cR3JsJ6Dkh5d9MrRsAh4jFmxjEwQfVvRGdq0YMRI3LlFCTA) |
| Chọn phương thức MFA - Admin HealthLens | `projects/578519912546445367/screens/397d87fb9f5f44d29e6ea84f12d81200` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2Y1YzhmMjU1MTBhZTQwODRiNWYzYWQxZWE4ZWU0M2M3EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ui45Ahm7PDaRB-od1UnyJbR1CdmgeG1NmAnlHHoxxJsgnxJwAd1y0hOkzY0BOeVrp4P0EM3QyuUp1c-GmKlV_7uwViK9GiRIVArIeSU5_q9IMIA-fAX3nqzNEGQx3Y5xVOejm7fjCq96T6MCoDW7J1tl5aKsCJgvipHhdU9_OKdYWeZcebTJnULCQJYKxu3MaLbmb6JbYEBxNgkj9mJgIoIpSGwuSMEol09Dkxi1PZX7VoPISFnaIfDFg) |
| Admin MFA Setup - HealthLens | `projects/578519912546445367/screens/beac3900063d4ced87a9d40ac644025e` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzM3NGI5OWUxMTcwZjRmNjI4OTA4MTA1ZDA3YjNmZmMzEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uiaKZakrh0QwWDYShE1mz5XklOPrVkvBLi8EMxeZ2dL1WzHHcjnQd6SnOqNuIJZWzQDmbK6iFTQSpnIWrIu4qKPrygkshzzHads2MYnP_vQdo5fo3j9e09XgZYL2imLVxflPpQi0arefdbskUbu3uGCJDyeb_LG3D1oL2vYr8qKD-O2sulbyF4QU2vCDSCT5Y93FPz7k-eWg-KpwMGjbfb6rz9o3Yot4pUGkGMosmFgLyJUVik_60uzAQ) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As an admin,
I want đăng nhập bằng mật khẩu + TOTP,
so that khu vực quản trị được bảo vệ đúng mức.

## Acceptance Criteria

1. **Given** tài khoản admin hợp lệ, **When** nhập đúng mật khẩu và mã TOTP từ Authenticator app, **Then** hệ thống cho phép truy cập admin panel.
2. **Given** mã TOTP sai hoặc hết hạn, **When** submit, **Then** 401 và yêu cầu nhập lại (không bypass được).
3. **Given** admin chưa setup TOTP, **When** đăng nhập lần đầu, **Then** bắt buộc setup TOTP trước khi vào panel (QR code setup).
4. **Given** session admin, **When** hết hạn (15 phút inactivity), **Then** redirect về trang login admin.
5. **Given** tất cả admin routes, **When** truy cập mà không có valid admin session, **Then** 403 redirect về admin login.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Admin role setup (AC: #1, #5)
  - [ ] Đảm bảo `ROLE_ADMIN` được define trong security config
  - [ ] Tất cả `/api/v1/admin/**` routes chỉ cho phép `ROLE_ADMIN`
  - [ ] Admin session TTL 15 phút (override user session)
- [ ] Task 2 — Backend: TOTP setup + verify (AC: #1, #2, #3)
  - [ ] Thêm dependency `com.warrenstrange:googleauth` cho TOTP
  - [ ] Bảng `admin_totp_secrets(user_id, secret, is_verified, created_at)` (Flyway V015)
  - [ ] `POST /api/v1/admin/auth/totp/setup` → generate TOTP secret + QR code URL
  - [ ] `POST /api/v1/admin/auth/login` → validate password + TOTP → admin JWT
  - [ ] `POST /api/v1/admin/auth/totp/verify` → verify code sau setup
- [ ] Task 3 — Web: Admin login page (AC: #1, #2, #3)
  - [ ] Tạo `apps/web/src/app/admin/login/page.tsx` (route group `/admin/`)
  - [ ] Step 1: email + password form
  - [ ] Step 2: TOTP input (6 digit, numeric)
  - [ ] Setup flow: QR code display + verify code step
- [ ] Task 4 — Web: Admin layout (AC: #4, #5)
  - [ ] `apps/web/src/app/admin/layout.tsx` — check admin session
  - [ ] Sidebar navigation: Reference Data, Audit Log, Analytics
  - [ ] Session timeout: auto-redirect sau 15 phút
- [ ] Task 5 — Tests (AC: #1, #2, #3)
  - [ ] `AdminAuthServiceTest`: login success, wrong TOTP, expired TOTP, unverified TOTP

## Dev Notes

### TOTP Library

```java
// build.gradle.kts
implementation("com.warrenstrange:googleauth:1.4.0")

// GoogleAuthenticator compatible (Google Authenticator, Authy, etc.)
GoogleAuthenticator gAuth = new GoogleAuthenticator();
GoogleAuthenticatorKey key = gAuth.createCredentials();
String secret = key.getKey();
// QR URL: otpauth://totp/{label}?secret={secret}&issuer=HealthLens
```

### Database (Flyway V015)

```sql
CREATE TABLE admin_totp_secrets (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    encrypted_secret TEXT NOT NULL,  -- AES-encrypted
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Admin JWT Claims

```json
{
  "sub": "adminUserId",
  "role": "ROLE_ADMIN",
  "totpVerified": true,
  "exp": "15m"
}
```

Admin JWT có thêm claim `totpVerified: true` — middleware check này trước khi allow access.

### Admin Sidebar (UX-DR7)

```
Admin Panel
├── 📊 Thống kê (Story 8.x)
├── 🔬 Reference Data (Story 7.2-7.5)
│   ├── Danh mục chỉ số
│   ├── Ngưỡng tham chiếu
│   ├── Import Data
│   └── Phê duyệt thay đổi
└── 📋 Audit Log (Story 7.5)
```

### References

- [Source: architecture.md#Triển-Khai-MFA-Admin]
- [Source: ux-design-specification.md#UX-DR7]
- [Source: epics.md#Story-7.1]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
