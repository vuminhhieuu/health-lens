# Story 1.3: Đăng nhập/đăng xuất với quản lý phiên bảo mật

Status: done

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

- [x] Task 1 — Backend: JWT infrastructure (AC: #1, #2)
  - [x] Tạo `JwtUtil.java` với `generateAccessToken()`, `generateRefreshToken()`, `validateToken()`, `extractClaims()`
  - [x] Thêm dependency `io.jsonwebtoken:jjwt-api:0.12.6`, `jjwt-impl`, `jjwt-jackson`
  - [x] Cấu hình JWT secret, access TTL (15m), refresh TTL (7d) trong `application.yml`
  - [x] Tạo `JwtAuthenticationFilter.java` extends `OncePerRequestFilter` — đọc Bearer token, validate, set SecurityContext
- [x] Task 2 — Backend: Login endpoint (AC: #1, #5)
  - [x] Tạo `LoginRequest` DTO với email, password
  - [x] Tạo `POST /api/v1/auth/login` trong `AuthController`
  - [x] `AuthService.login()`: authenticate credentials, generate tokens, set refresh token HttpOnly cookie
  - [x] Implement `UserDetailsService` load user từ DB
  - [x] Trả về `LoginResponse` với access token, user info (id, email, role)
- [x] Task 3 — Backend: Token refresh endpoint (AC: #2)
  - [x] Tạo `POST /api/v1/auth/refresh` — đọc HttpOnly cookie, validate refresh token, issue new token pair
  - [x] Implement refresh token rotation (invalidate old, issue new) lưu trong bảng `refresh_tokens`
  - [x] Flyway migration `V003__create_refresh_tokens_table.sql`
- [x] Task 4 — Backend: Logout endpoint (AC: #4)
  - [x] Tạo `POST /api/v1/auth/logout` — blacklist access token trong Redis (TTL = remaining expiry), xóa refresh token cookie
  - [x] Redis key pattern: `blacklist:token:{jti}` với TTL bằng thời gian còn lại của access token
- [x] Task 5 — Backend: Rate limiting / account lock (AC: #6)
  - [x] Track failed login attempts trong Redis: key `login_attempts:{email}`, TTL 15 phút
  - [x] Sau 5 lần sai: trả về 429 Too Many Requests với `Retry-After` header
- [x] Task 6 — Backend: Spring Security config (AC: #1)
  - [x] Tạo/cập nhật `SecurityConfig.java`: permit `/api/v1/auth/**`, authenticate tất cả routes khác
  - [x] Thêm `JwtAuthenticationFilter` vào filter chain trước `UsernamePasswordAuthenticationFilter`
- [x] Task 7 — Web: Login form và auth state (AC: #1, #4)
  - [x] Tạo `apps/web/src/app/(auth)/login/page.tsx`
  - [x] Form: email, password với React Hook Form + Zod
  - [x] Submit → `POST /api/v1/auth/login`, lưu access token vào `useAuthStore` (Zustand)
  - [x] Tạo `apps/web/src/stores/authStore.ts` với state: `user`, `accessToken`, `isAuthenticated`
  - [x] Auto-refresh: axios interceptor → gọi `/api/v1/auth/refresh` khi 401
  - [x] Protected route middleware: `(dashboard)/layout.tsx` check auth, redirect về `/login`
- [ ] Task 8 — Mobile (Phase 2): Login screen và auth state (AC: #1, #4)
  - [ ] Tạo `apps/mobile/app/(auth)/login.tsx`
  - [ ] Share `useAuthStore` logic tương tự web (access token trong SecureStore của Expo)
  - [ ] Expo SecureStore thay thế HttpOnly cookie cho mobile (refresh token)
- [x] Task 9 — Tests (AC: #1, #2, #4, #5, #6)
  - [x] `AuthServiceTest`: login thành công, sai password, tài khoản bị lock
  - [x] `AuthControllerTest`: integration test cho login, refresh, logout
- [x] Task 10 — Backend: Tích hợp Swagger / OpenAPI (User Requested)
  - [x] Thêm `springdoc-openapi` dependency
  - [x] Cấu hình `OpenApiConfig` để hỗ trợ Bearer Auth
  - [x] Expose endpoint `/swagger-ui.html` trong `SecurityConfig`
- [x] Task 11 — Refactor: Gom nhóm API Routes Constants (User Requested)
  - [x] Tạo `ApiRoutes.java` cho Backend và `routes.ts` cho Frontend
  - [x] Thay thế toàn bộ hardcoded path strings trong Controller và SecurityConfig

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

Claude Opus 4.6 (Thinking) via Antigravity

### Debug Log References

- Spring Boot 4 `DaoAuthenticationProvider` constructor changed → requires `UserDetailsService` in constructor
- JPQL `CURRENT_TIMESTAMP` không tương thích với `Instant` field trong Hibernate → dùng explicit `Instant` parameter
- Flyway `gen_random_uuid()` không hỗ trợ H2 → dùng `@PrePersist` UUID generation thay thế
- `@MockitoBean JwtAuthenticationFilter` không chain filter → thêm `doAnswer` delegate trong `@BeforeEach`

### Completion Notes List

- ✅ Task 1: JwtUtil (HS256, access 15m, refresh 7d), JwtAuthenticationFilter (Bearer + Redis blacklist check), JJWT 0.12.6 deps
- ✅ Task 2: LoginRequest/LoginResponse DTOs, AuthController.login(), AuthService.login() với email verification gate
- ✅ Task 3: RefreshToken entity, V003 migration, token rotation trong AuthService.refresh()
- ✅ Task 4: AuthController.logout() → Redis blacklist (TTL = remaining expiry) + clear HttpOnly cookie
- ✅ Task 5: LoginRateLimiter (Redis `login_attempts:{email}`, 5 max, 15m TTL), AccountLockedException → 429
- ✅ Task 6: SecurityConfig stateless JWT, permit `/api/v1/auth/**`, DaoAuthenticationProvider, CustomUserDetailsService
- ✅ Task 7: Login page (Stitch Meridian design), Zustand authStore, apiClient (axios interceptor + refresh queue), dashboard protected layout
- ⏭️ Task 8: Mobile — Phase 2, skipped per story scope
- ✅ Task 9: AuthServiceTest (10 tests), AuthControllerTest (11 tests) — all 78 tests pass
- ✅ **Bonus 1**: Tích hợp Swagger (SpringDoc OpenAPI) tại `/swagger-ui.html`
- ✅ **Bonus 2**: Gom tất cả API routes vào `ApiRoutes.java` (backend) và `API_ROUTES` (frontend)

### Change Log

- 2026-04-14: Story 1.3 implemented — JWT auth, login/refresh/logout endpoints, rate limiting, login UI, auth state management

### File List

**New files:**
- `apps/api/src/main/java/com/healthlens/api/util/JwtUtil.java`
- `apps/api/src/main/java/com/healthlens/api/security/JwtAuthenticationFilter.java`
- `apps/api/src/main/java/com/healthlens/api/security/CustomUserDetailsService.java`
- `apps/api/src/main/java/com/healthlens/api/security/LoginRateLimiter.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/LoginRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/LoginResponse.java`
- `apps/api/src/main/java/com/healthlens/api/entity/RefreshToken.java`
- `apps/api/src/main/java/com/healthlens/api/repository/RefreshTokenRepository.java`
- `apps/api/src/main/java/com/healthlens/api/exception/AccountLockedException.java`
- `apps/api/src/main/resources/db/migration/V003__create_refresh_tokens_table.sql`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java`
- `apps/web/src/stores/authStore.ts`
- `apps/web/src/lib/api/apiClient.ts`
- `apps/web/src/app/(dashboard)/layout.tsx`

**Modified files:**
- `apps/api/build.gradle.kts` — thêm JJWT dependencies
- `apps/api/src/main/resources/application.yml` — thêm jwt config
- `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java` — stateless JWT, filter chain
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java` — thêm login/refresh/logout
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java` — thêm login/refresh/logout endpoints
- `apps/api/src/main/java/com/healthlens/api/repository/UserRepository.java` — thêm findByEmailIgnoreCase
- `apps/api/src/main/java/com/healthlens/api/exception/GlobalExceptionHandler.java` — thêm AccountLocked/BadCredentials handlers
- `apps/api/src/test/java/com/healthlens/api/controller/AuthControllerTest.java` — thêm login/refresh/logout tests
- `apps/web/src/app/(auth)/login/page.tsx` — login form UI (Stitch design)
- `packages/shared/schemas/auth.ts` — thêm loginSchema
