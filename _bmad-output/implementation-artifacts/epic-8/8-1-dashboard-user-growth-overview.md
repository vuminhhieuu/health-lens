# Story 8.1: Dashboard tổng quan tăng trưởng người dùng

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

### Phạm vi triển khai (in / out)

| Thuộc story 8.1 | Không thuộc story 8.1 |
|-----------------|------------------------|
| API `GET /api/v1/admin/analytics/users` | API upload-quality / upload-history (8.3) |
| Section **Tăng trưởng người dùng** (`UserGrowthPanel` + `UserGrowthChart`) | `UploadQualityPanel` (8.3) |
| Redirect bookmark `/admin/analytics` → `/admin` | `AdminGeneralStats` — WAU / upload volume (8.2) |
| Unit/controller tests cho user analytics | Toàn bộ layout trang **Thống kê** (`/admin`) — header và các section khác thuộc Epic 8 chung hoặc story 8.2/8.3 |

**Vị trí UI:** Section nằm trên route `/admin` (trang **Thống kê** admin), không phải trang analytics riêng. Stitch tham chiếu màn **Admin Dashboard** chung; story này chỉ deliver **một panel** trong màn đó.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Admin Dashboard - HealthLens | `projects/578519912546445367/screens/1cfbb9fe88734e629a044add27adf1f1` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzBiMWFlZWZiMDI4YzQ0MGViNWNjMWQ3MjRlZjhlZDMzEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uijwmGIG1W614k_uW20_8CbGsFR4cgnisiMrAMerujv8PU8lE-LSP3MLF23Vz_C4SBjKyv4HT1rlWyqPpYgs44PrW-FtCiwwW8vD0R7IYmALpnjK6wsfdkOxDg8GbqGtjgZLoEgbrOJurGv2mPjWbX4QuasSNwhghMqOOxZ73o6Dnuw0MaF-1A3_7kuHR0Xr_HRQYWbgTm66_Xkgmkvfm60ok0GctdWO-dAGXyGLOHjzZg4SNuhk1aM) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As an admin,
I want xem tổng user all-time và theo tháng,
so that tôi theo dõi đà tăng trưởng sản phẩm.

## Acceptance Criteria

1. **Given** dữ liệu người dùng đã được ghi nhận, **When** mở trang thống kê admin (`/admin`) và xem section tăng trưởng người dùng, **Then** hiển thị tổng registered users all-time và monthly trend (line chart).
2. **Given** chart tăng trưởng, **When** hiển thị, **Then** trục X: tháng (mặc định 6 tháng gần nhất, UTC), trục Y: số user **mới** trong tháng (Recharts auto-scale theo dữ liệu).
3. **Given** dashboard, **When** load, **Then** số liệu được tính từ database thực qua API (không hardcode); có cache Redis TTL 1 giờ (không real-time theo thiết kế).
4. **Given** admin chọn range khác, **When** thay đổi bộ lọc Từ/Đến, **Then** chart và `monthlyGrowth` cập nhật tương ứng (tối đa 24 tháng).

## Tasks / Subtasks

- [x] Task 1 — Backend: User analytics endpoint (AC: #1, #2, #3)
  - [x] `GET /api/v1/admin/analytics/users?from={iso-date}&to={iso-date}` (cả hai optional; mặc định 6 tháng UTC)
  - [x] Response envelope: `{ data: { totalUsers, monthlyGrowth: [{ month, newUsers }] } }`
  - [x] `totalUsers`: `COUNT` users `role = ROLE_USER` và `account_status <> DELETED` (all-time, **không** lọc theo `from`/`to`)
  - [x] `monthlyGrowth`: native query `DATE_TRUNC('month', created_at)` + `COUNT(*)`, `role = ROLE_USER`, `created_at` trong `[from, toExclusive)`; điền đủ từng tháng trong range (tháng không có đăng ký → `newUsers: 0`)
  - [x] Validate range: `from < toExclusive`, tối đa **24** tháng lịch (UTC)
  - [x] Cache Redis TTL **1 giờ**: key `analytics:users:total:v1` và `analytics:users:growth:v1:{from}:{toExclusive}`
- [x] Task 2 — Web: User growth section (AC: #1, #2, #4)
  - [x] `UserGrowthPanel` trên `/admin` (import từ `apps/web/src/app/admin/page.tsx`)
  - [x] Stat card: tổng người dùng đăng ký (số lớn, locale `vi-VN`)
  - [x] `UserGrowthChart`: line chart Recharts (`newUsers` theo tháng)
  - [x] Date picker Từ/Đến (UTC), validation client khớp backend (24 tháng, `from <= to`)
  - [x] Loading / error / empty states (`LoadingState`, `ErrorState`, `EmptyState`)
  - [x] `apps/web/src/app/admin/analytics/page.tsx`: **redirect** `/admin` (giữ bookmark, không phải trang analytics đầy đủ)
- [x] Task 3 — Tests (AC: #1, #3, #4)
  - [x] `AnalyticsServiceTest.getUserAnalytics_returnsTotalsAndFillsMissingMonths`
  - [x] `AnalyticsServiceTest.validateUserAnalyticsRange_rejectsRangeOverMaxMonths`
  - [x] `AdminAnalyticsControllerTest` — 200 với admin, 400 khi range > 24 tháng

## Dev Notes

### Quy tắc số liệu

| Field | Nguồn | Ghi chú |
|-------|--------|---------|
| `totalUsers` | `UserRepository.countRegisteredProductUsers(ROLE_USER)` | All-time; gồm ACTIVE và trạng thái chờ xóa; loại `DELETED`; **không** lọc email verified |
| `monthlyGrowth[].newUsers` | `UserRepository.findMonthlyUserGrowth(from, toExclusive)` | Chỉ `ROLE_USER`; theo `created_at` UTC |
| Chart trục Y | `newUsers` | **Không** phải tổng tích lũy; Recharts không cố định max — scale theo max trong range |

### Tham số `from` / `to` (API)

- `from` (optional): chuẩn hóa về **ngày 1** của tháng chứa `from` (UTC).
- `to` (optional): `toExclusive` = **ngày 1 tháng kế tiếp** sau tháng chứa `to` (UTC) — tức range **bao gồm cả tháng** của `to`.
- Mặc định khi bỏ query: `from` = đầu tháng (now − 5 tháng), `toExclusive` = đầu tháng sau tháng hiện tại → **6 tháng** lịch.

### Cache (AC #3 — không real-time)

- TTL: 1 giờ (`USER_ANALYTICS_CACHE_TTL`).
- **Không** invalidate khi `POST /auth/register` — sau đăng ký mới, `totalUsers` / `monthlyGrowth` có thể giữ giá trị cũ tới hết TTL hoặc khi xóa key Redis thủ công.
- Kiểm thử “tăng ngay sau đăng ký”: xóa `analytics:users:total:v1` và key growth tương ứng, hoặc đợi TTL.

### API

```
GET /api/v1/admin/analytics/users
GET /api/v1/admin/analytics/users?from=2026-01-01&to=2026-05-19
Authorization: Bearer <admin JWT>   (ROLE_ADMIN)
```

### API Response

```json
{
  "data": {
    "totalUsers": 1250,
    "monthlyGrowth": [
      { "month": "2026-01", "newUsers": 180 },
      { "month": "2026-02", "newUsers": 0 },
      { "month": "2026-03", "newUsers": 220 }
    ]
  }
}
```

### Web — component chính

- `UserGrowthPanel`: fetch qua `adminApiClient` + React Query; query key `["admin-user-analytics", from, to]`.
- `apps/web/src/lib/admin/userAnalytics.ts`: default range 6 tháng UTC, `MAX_USER_ANALYTICS_MONTHS = 24` (khớp `AnalyticsService.MAX_USER_ANALYTICS_MONTHS`).

### Recharts (đã có trong web deps)

```typescript
// UserGrowthChart.tsx — YAxis không set domain cố định; scale theo data
<LineChart data={chartData}>
  <XAxis dataKey="label" />  {/* MM/yyyy từ month yyyy-MM */}
  <YAxis allowDecimals={false} />
  <Line type="monotone" dataKey="newUsers" stroke="#0D9488" />
</LineChart>
```

### Liên quan Epic 8 khác

- **8.2** — WAU + upload volume → `AdminGeneralStats` (stub).
- **8.3** — Upload success/failure → `UploadQualityPanel` (done).
- Spec tổng: [`epic-8-analytics-spec.md`](../../planning-artifacts/review-source/production-review/epic-8-analytics-spec.md).

### References

- [Source: epics.md#Story-8.1]
- [Source: architecture.md#Giám-Sát-&-Quan-Sát]
- [FR38 — Admin dashboard thống kê](../../planning-artifacts/implementation-readiness-report-2026-03-28.md)

## Test Plan

### Tự động (API)

```bash
cd apps/api
./gradlew test --tests 'com.healthlens.api.service.AnalyticsServiceTest.getUserAnalytics_returnsTotalsAndFillsMissingMonths'
./gradlew test --tests 'com.healthlens.api.service.AnalyticsServiceTest.validateUserAnalyticsRange_rejectsRangeOverMaxMonths'
./gradlew test --tests 'com.healthlens.api.controller.AdminAnalyticsControllerTest'
```

### Thủ công — map Acceptance Criteria

| AC | Bước kiểm tra | Pass khi |
|----|----------------|----------|
| #1 | Đăng nhập admin → `/admin` → section **Tăng trưởng người dùng** | Có số tổng + line chart (hoặc empty nếu không có tháng trong range) |
| #2 | Mặc định load (không đổi date) | Chart ~6 tháng; trục X MM/yyyy; Y = user mới/tháng |
| #3 | So `totalUsers` với SQL COUNT `ROLE_USER` not DELETED; đổi DB / đăng ký user mới **sau khi xóa cache Redis** | Khớp DB; không có số hardcode trong UI |
| #4 | Đổi Từ/Đến → Network thấy request mới; chọn range > 24 tháng hoặc from > to | Chart đổi theo data; UI báo lỗi / API 400 |

### Thủ công — bảo mật & edge

- Gọi API không token hoặc user `ROLE_USER` → **403**.
- `/admin/analytics` → redirect **307/308** về `/admin`, panel vẫn hiển thị.
- Tháng không có đăng ký → cột/điểm `newUsers = 0` vẫn xuất hiện trên chart.

### Không thuộc phạm vi kiểm thử 8.1

- Upload quality charts, WAU, header layout toàn trang (trừ khi regression UI chung).

## Dev Agent Record

### Agent Model Used

Composer

### Debug Log References

### Completion Notes List

- Backend: `GET /api/v1/admin/analytics/users` — `totalUsers` (ROLE_USER all-time, loại DELETED), `monthlyGrowth` theo tháng UTC, điền tháng trống = 0, cache Redis TTL 1h.
- Web: `UserGrowthPanel` + `UserGrowthChart` trên `/admin`; style slate/teal đồng bộ `UploadQualityPanel`; `/admin/analytics` redirect giữ bookmark.
- Tests: `AnalyticsServiceTest` (totals + fill months + validate range); `AdminAnalyticsControllerTest` (200 admin, 400 range).
- **Out of scope đã ghi rõ:** không implement toàn trang Thống kê; `AdminGeneralStats` / `UploadQualityPanel` thuộc 8.2 / 8.3.

### File List

**API**

- apps/api/src/main/java/com/healthlens/api/controller/AdminAnalyticsController.java
- apps/api/src/main/java/com/healthlens/api/service/AnalyticsService.java
- apps/api/src/main/java/com/healthlens/api/repository/UserRepository.java
- apps/api/src/main/java/com/healthlens/api/repository/projection/MonthlyUserGrowthProjection.java
- apps/api/src/main/java/com/healthlens/api/dto/response/UserAnalyticsResponse.java
- apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java
- apps/api/src/test/java/com/healthlens/api/service/AnalyticsServiceTest.java
- apps/api/src/test/java/com/healthlens/api/controller/AdminAnalyticsControllerTest.java

**Shared**

- packages/shared/constants/api.ts

**Web**

- apps/web/src/app/admin/page.tsx (mount `UserGrowthPanel`)
- apps/web/src/app/admin/analytics/page.tsx (redirect → `/admin`)
- apps/web/src/components/admin/UserGrowthPanel.tsx
- apps/web/src/components/admin/UserGrowthChart.tsx
- apps/web/src/lib/admin/userAnalytics.ts

## Change Log

- 2026-05-19: Bổ sung tài liệu story — phạm vi in/out, quy tắc số liệu, cache, test plan, file list chính xác; làm rõ UI trên `/admin` (section) thay vì trang analytics riêng.
- 2026-05-19: Triển khai Story 8.1 — API user analytics + `UserGrowthPanel` + tests; status `review`.
