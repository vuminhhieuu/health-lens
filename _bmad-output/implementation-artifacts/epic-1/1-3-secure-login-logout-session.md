# Story 1.3: Đăng nhập/đăng xuất với quản lý phiên bảo mật

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Đăng Nhập - HealthLens | `projects/578519912546445367/screens/09409d972eaa45f592c396b6d14469aa` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzc2MmUyZWUxZWFjZTQ1M2Q5NTZjNTJiMWRiYzdiZDEwEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugHcSPA5P0oZU6j_eteOyHbxbYMimQ_1cqMdz_maFfFgYKnBxsSjBYguAB5gSy_Iss-GJ0GbzpZ2SsYo4-P-Hx-D0arBuRKAjDd2pyivXISs5x8K6RJFq7zy4Nm_CXrDFzzi-oMLuXM_2DxKr-WOksDFSA3YgGPATc8cCS2QqveSoGo7tOQqmbwBeKq2shumcmMCgaDEgshvJ7LozC4qULNbp81XJEOdOvzJXZRaqcKb7L_ElsoGHWz) |
| Dashboard Home Page - HealthLens | `projects/578519912546445367/screens/010095343e6c46b4969b42c4ab93165a` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzBkNTk5MTY2NzIxNzQ0NDk5YjJlMjQxMWM1N2U3ZGRkEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ujIG2rNbWjcWsO9G1za9-KcM9gHUwj2X237xGhShjEjCWwi29NS6udE0Flhdtdop0NdfoD_Q2MWeNXsYPv9ycxE1WW7o8Q-d1R0yleCrW7kDtJKwmG3PSp2i5ElEnFX2PBVJRnaMV1LyvBv2tceKjwgi32axKgQD_qspcqMvh2sOPytIhDH-xmzLB10vKA25LIp5_lTgyqaRkKrSQ3LXCMp6nLt8BGuRGlAxnkrlkV-A5GUZWmIVjBIVg) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want đăng nhập và đăng xuất an toàn,
so that tôi truy cập đúng dữ liệu của mình và kiểm soát phiên đăng nhập.

## Acceptance Criteria

1. **Given** tài khoản đã xác thực email, **When** đăng nhập đúng email/password, **Then** hệ thống cấp access token (15 phút) và refresh token (7 ngày, HttpOnly cookie).
2. **Given** access token hết hạn, **When** gọi API với expired token, **Then** 401 Unauthorized và client có thể dùng refresh token để lấy token mới.
3. **Given** người dùng không hoạt động 30 phút, **When** refresh token cũng hết hạn, **Then** phiên tự động kết thúc và redirect đến trang login.
4. **Given** đã đăng nhập, **When** bấm đăng xuất, **Then** access token bị blacklist trong Redis và refresh token bị xóa khỏi HttpOnly cookie.
5. **Given** đăng nhập sai password, **When** hệ thống kiểm tra, **Then** trả về 401 với RFC 7807, **không** lộ thông tin email có tồn tại hay không.
6. **Given** sau 5 lần đăng nhập sai liên tiếp, **When** thử lần thứ 6, **Then** tài khoản bị lock tạm thời 15 phút (rate limiting).

## Tasks / Subtasks

- [ ] Task 1 — Backend: JWT infrastructure (AC: #1, #2)
  - [ ] Tạo `JwtUtil.java` với `generateAccessToken()`, `generateRefreshToken()`, `validateToken()`, `extractClaims()`
  - [ ] Thêm dependency `io.jsonwebtoken:jjwt-api:0.12.x`, `jjwt-impl`, `jjwt-jackson`
  - [ ] Cấu hình JWT secret, access TTL (15m), refresh TTL (7d) trong `application.yml`
  - [ ] Tạo `JwtAuthenticationFilter.java` extends `OncePerRequestFilter` — đọc Bearer token, validate, set SecurityContext
- [ ] Task 2 — Backend: Login endpoint (AC: #1, #5)
  - [ ] Tạo `LoginRequest` DTO với email, password
  - [ ] Tạo `POST /api/v1/auth/login` trong `AuthController`
  - [ ] `AuthService.login()`: authenticate credentials, generate tokens, set refresh token HttpOnly cookie
  - [ ] Implement `UserDetailsService` load user từ DB
  - [ ] Trả về `LoginResponse` với access token, user info (id, email, role)
- [ ] Task 3 — Backend: Token refresh endpoint (AC: #2)
  - [ ] Tạo `POST /api/v1/auth/refresh` — đọc HttpOnly cookie, validate refresh token, issue new token pair
  - [ ] Implement refresh token rotation (invalidate old, issue new) lưu trong bảng `refresh_tokens`
  - [ ] Flyway migration `V003__create_refresh_tokens_table.sql`
- [ ] Task 4 — Backend: Logout endpoint (AC: #4)
  - [ ] Tạo `POST /api/v1/auth/logout` — blacklist access token trong Redis (TTL = remaining expiry), xóa refresh token cookie
  - [ ] Redis key pattern: `blacklist:token:{jti}` với TTL bằng thời gian còn lại của access token
- [ ] Task 5 — Backend: Rate limiting / account lock (AC: #6)
  - [ ] Track failed login attempts trong Redis: key `login_attempts:{email}`, TTL 15 phút
  - [ ] Sau 5 lần sai: trả về 429 Too Many Requests với `Retry-After` header
- [ ] Task 6 — Backend: Spring Security config (AC: #1)
  - [ ] Tạo/cập nhật `SecurityConfig.java`: permit `/api/v1/auth/**`, authenticate tất cả routes khác
  - [ ] Thêm `JwtAuthenticationFilter` vào filter chain trước `UsernamePasswordAuthenticationFilter`
- [ ] Task 7 — Web: Login form và auth state (AC: #1, #4)
  - [ ] Tạo `apps/web/src/app/(auth)/login/page.tsx`
  - [ ] Form: email, password với React Hook Form + Zod
  - [ ] Submit → `POST /api/v1/auth/login`, lưu access token vào `useAuthStore` (Zustand)
  - [ ] Tạo `apps/web/src/stores/authStore.ts` với state: `user`, `accessToken`, `isAuthenticated`
  - [ ] Auto-refresh: axios interceptor → gọi `/api/v1/auth/refresh` khi 401
  - [ ] Protected route middleware: `(dashboard)/layout.tsx` check auth, redirect về `/login`
- [ ] Task 8 — Mobile (Phase 2): Login screen và auth state (AC: #1, #4)
  - [ ] Tạo `apps/mobile/app/(auth)/login.tsx`
  - [ ] Share `useAuthStore` logic tương tự web (access token trong SecureStore của Expo)
  - [ ] Expo SecureStore thay thế HttpOnly cookie cho mobile (refresh token)
- [ ] Task 9 — Tests (AC: #1, #2, #4, #5, #6)
  - [ ] `AuthServiceTest`: login thành công, sai password, tài khoản bị lock
  - [ ] `AuthControllerTest`: integration test cho login, refresh, logout

## Dev Notes

### JWT Token Structure

```
Access Token (Bearer):
- Header: alg=HS256
- Claims: sub=userId, email, role, jti (unique ID), iat, exp (15m)

Refresh Token (HttpOnly Cookie):
- Name: `refresh_token`
- HttpOnly: true, Secure: true (prod), SameSite: Strict
- Path: /api/v1/auth/refresh
- Max-Age: 7d
```

### Database Schema

```sql
-- V003__create_refresh_tokens_table.sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,  -- hash của token, không lưu raw
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

### Redis Keys

```
blacklist:token:{jti}       → "1" (TTL = remaining access token expiry)
login_attempts:{email}      → count (TTL = 15 phút)
```

### API Contracts

```
POST /api/v1/auth/login
Body: { "email": "...", "password": "..." }
Response 200: {
  "data": {
    "accessToken": "eyJ...",
    "user": { "id": "uuid", "email": "...", "role": "ROLE_USER" }
  },
  "meta": { "timestamp": "...", "requestId": "..." }
}
Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; Max-Age=604800

POST /api/v1/auth/refresh
Cookie: refresh_token=...
Response 200: { "data": { "accessToken": "eyJ..." }, "meta": {...} }
(+ new Set-Cookie)

POST /api/v1/auth/logout
Authorization: Bearer ...
Response 204 No Content
```

### Zustand Auth Store

```typescript
// apps/web/src/stores/authStore.ts
interface AuthState {
  user: { id: string; email: string; role: string } | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  setAuth: (user, token) => void;
  clearAuth: () => void;
}
```

### Security Notes

- Không lộ "email không tồn tại" vs "sai password" trong response
- refresh token chỉ hash SHA-256 lưu DB, không raw
- `jti` claim trong access token dùng để blacklist tại logout
- Session 30 phút: access token 15m + refresh window tối đa 30m không hoạt động → implement bằng `last_active` tracking

### Project Structure Notes

- `SecurityConfig.java` trong `com.healthlens.api.config`
- `JwtUtil.java` trong `com.healthlens.api.util`
- `JwtAuthenticationFilter.java` trong `com.healthlens.api.security`

### References

- [Source: architecture.md#Chiến-Lược-Xác-Thực]
- [Source: architecture.md#Mẫu-Phân-Quyền]
- [Source: architecture.md#Quản-Lý-State]
- [Source: epics.md#Story-1.3]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
