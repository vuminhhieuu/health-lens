# Story 8.2: Dashboard hoạt động WAU và upload volume

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Admin Dashboard - HealthLens | `projects/578519912546445367/screens/1cfbb9fe88734e629a044add27adf1f1` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzBiMWFlZWZiMDI4YzQ0MGViNWNjMWQ3MjRlZjhlZDMzEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uijwmGIG1W614k_uW20_8CbGsFR4cgnisiMrAMerujv8PU8lE-LSP3MLF23Vz_C4SBjKyv4HT1rlWyqPpYgs44PrW-FtCiwwW8vD0R7IYmALpnjK6wsfdkOxDg8GbqGtjgZLoEgbrOJurGv2mPjWbX4QuasSNwhghMqOOxZ73o6Dnuw0MaF-1A3_7kuHR0Xr_HRQYWbgTm66_Xkgmkvfm60ok0GctdWO-dAGXyGLOHjzZg4SNuhk1aM) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As an admin,
I want xem WAU và số lượt upload theo ngày/tuần,
so that tôi đo mức sử dụng thực tế của hệ thống.

## Acceptance Criteria

1. **Given** sự kiện sử dụng đã được tracking, **When** chọn khoảng thời gian, **Then** dashboard hiển thị WAU và upload counts theo granularity.
2. **Given** WAU chart, **When** hiển thị, **Then** định nghĩa WAU: user có ít nhất 1 API call authenticated trong tuần.
3. **Given** upload volume, **When** hiển thị, **Then** bar chart theo ngày hoặc tuần tùy chọn.
4. **Given** filter theo tuần vs ngày, **When** switch, **Then** chart cập nhật granularity tương ứng.

## Tasks / Subtasks

- [x] Task 1 — Backend: Activity tracking (AC: #1, #2)
  - [x] Bảng `user_activity_events` (V043)
  - [x] `UserActivityRecordingFilter` + confirmUpload ghi `AUTHENTICATED_API_CALL` / `UPLOAD_CONFIRMED`
  - [x] `GET /api/v1/admin/analytics/activity?granularity=day|week&from&to&comparePrevious`
  - [x] WAU query theo tuần (distinct user)
  - [x] Upload volume query theo ngày/tuần
- [x] Task 2 — Web: Activity charts (AC: #1, #3, #4)
  - [x] `ActivityVolumePanel` trên `/admin` (giữa stats và chất lượng upload)
  - [x] WAU line chart + upload bar chart (recharts)
  - [x] Toggle day/week + so sánh kỳ trước
- [x] Task 3 — Tests (AC: #1, #2)
  - [x] `AnalyticsServiceTest`: WAU/upload buckets + percentChange
  - [x] `activityAnalytics.test.ts`: UTC default range + rule validate max 90 ngày

## Dev Notes

### Activity Events Table

```sql
-- V043__create_user_activity_events.sql
CREATE TABLE user_activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,  -- 'AUTHENTICATED_API_CALL' | 'UPLOAD_CONFIRMED'
    is_retry BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_activity_events_type_created ON user_activity_events (event_type, created_at);
CREATE INDEX idx_activity_events_user_created ON user_activity_events (user_id, created_at);
CREATE UNIQUE INDEX idx_activity_auth_daily
    ON user_activity_events (user_id, ((created_at AT TIME ZONE 'UTC')::date))
    WHERE event_type = 'AUTHENTICATED_API_CALL';
```

### WAU Definition

```sql
SELECT DATE_TRUNC('week', created_at AT TIME ZONE 'UTC') AS week,
       COUNT(DISTINCT user_id) AS wau
FROM user_activity_events
WHERE event_type = 'AUTHENTICATED_API_CALL'
  AND created_at >= :from
  AND created_at < :toExclusive
GROUP BY 1
ORDER BY 1;
```

### References

- [Source: epics.md#Story-8.2]

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes List

- Migration `V043__create_user_activity_events.sql` với unique index 1 AUTH event / user / UTC day.
- `UserActivityRecordingFilter` ghi `AUTHENTICATED_API_CALL` sau response thành công cho `ROLE_USER` (bỏ qua admin/auth/consent).
- `HealthRecordService.confirmUpload` ghi `UPLOAD_CONFIRMED` (is_retry khi upload lại).
- API `GET /api/v1/admin/analytics/activity` trả summary, WAU buckets (weekly), upload buckets (day|week), so sánh kỳ trước.
- Web: `ActivityVolumePanel` hiển thị trên `/admin` (giữa `UserGrowthPanel` và `UploadQualityPanel`), bao gồm card WAU/upload theo tuần và biểu đồ WAU/upload theo range.
- Frontend test: `activityAnalytics.test.ts` kiểm tra default UTC window và validate range (khớp `MAX_ACTIVITY_DAYS`).

### File List

- `apps/api/src/main/resources/db/migration/V043__create_user_activity_events.sql`
- `apps/api/src/main/java/com/healthlens/api/entity/UserActivityEvent.java`
- `apps/api/src/main/java/com/healthlens/api/activity/UserActivityEventType.java`
- `apps/api/src/main/java/com/healthlens/api/repository/UserActivityEventRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/projection/ActivityWauBucketProjection.java`
- `apps/api/src/main/java/com/healthlens/api/repository/projection/ActivityUploadBucketProjection.java`
- `apps/api/src/main/java/com/healthlens/api/service/UserActivityService.java`
- `apps/api/src/main/java/com/healthlens/api/service/AnalyticsService.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/ActivityAnalyticsResponse.java`
- `apps/api/src/main/java/com/healthlens/api/controller/AdminAnalyticsController.java`
- `apps/api/src/main/java/com/healthlens/api/security/UserActivityRecordingFilter.java`
- `apps/api/src/test/java/com/healthlens/api/security/UserActivityRecordingFilterTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/UserActivityServiceTest.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/test/java/com/healthlens/api/service/AnalyticsServiceTest.java`
- `packages/shared/constants/api.ts`
- `apps/web/src/components/admin/ActivityVolumePanel.tsx`
- `apps/web/src/components/admin/WauLineChart.tsx`
- `apps/web/src/components/admin/UploadVolumeBarChart.tsx`
- `apps/web/src/app/admin/page.tsx`
- `apps/web/src/lib/admin/activityAnalytics.test.ts`
