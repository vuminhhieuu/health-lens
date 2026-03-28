# Story 8.2: Dashboard hoạt động WAU và upload volume

Status: ready-for-dev

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

- [ ] Task 1 — Backend: Activity tracking (AC: #1, #2)
  - [ ] Bảng `user_activity_events(id, user_id, event_type, created_at)` (V018)
  - [ ] Spring filter log `LOGIN`, `UPLOAD` events (quan trọng nhất)
  - [ ] `GET /api/v1/admin/analytics/activity?granularity=day|week&from={date}&to={date}`
  - [ ] WAU query: `SELECT DATE_TRUNC('week', created_at), COUNT(DISTINCT user_id) FROM user_activity_events ...`
  - [ ] Upload volume query: count uploads per day/week
- [ ] Task 2 — Web: Activity charts (AC: #1, #3, #4)
  - [ ] Trong admin analytics page: thêm 2 sections
  - [ ] WAU line chart (recharts)
  - [ ] Upload volume bar chart (recharts BarChart)
  - [ ] Toggle day/week granularity
- [ ] Task 3 — Tests (AC: #1, #2)
  - [ ] `AnalyticsServiceTest`: WAU calculation, upload count

## Dev Notes

### Activity Events Table

```sql
-- V018__create_user_activity_events.sql
CREATE TABLE user_activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,  -- 'AUTH', 'UPLOAD', 'VIEW'
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_activity_events_user_week ON user_activity_events(user_id, DATE_TRUNC('week', created_at));
CREATE INDEX idx_activity_events_type_date ON user_activity_events(event_type, created_at);
```

### WAU Definition

```sql
SELECT DATE_TRUNC('week', created_at) AS week,
       COUNT(DISTINCT user_id) AS wau
FROM user_activity_events
WHERE event_type = 'AUTH'
  AND created_at BETWEEN :from AND :to
GROUP BY 1
ORDER BY 1 DESC;
```

### References

- [Source: epics.md#Story-8.2]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
