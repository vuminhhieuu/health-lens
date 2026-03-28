# Story 8.3: Tỷ lệ upload thành công vs thất bại

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
I want theo dõi success/failure rate của upload pipeline,
so that tôi phát hiện sớm vấn đề OCR hoặc hạ tầng.

## Acceptance Criteria

1. **Given** các upload job có trạng thái xử lý, **When** mở phần quality metrics, **Then** hiển thị tỷ lệ thành công/thất bại theo ngày và tuần.
2. **Given** failure chart, **When** hiển thị, **Then** "thành công" = status `done`, "thất bại" = status `ocr_failed`.
3. **Given** admin muốn drill-down, **When** click vào một bar trong chart, **Then** hiển thị breakdown theo nguyên nhân lỗi (timeout, low_confidence, api_error).
4. **Given** sudden spike trong failure rate, **When** nhìn chart, **Then** dễ nhận ra bất thường (visual threshold line hoặc color coding).

## Tasks / Subtasks

- [ ] Task 1 — Backend: Upload quality endpoint (AC: #1, #2, #3)
  - [ ] `GET /api/v1/admin/analytics/upload-quality?from={date}&to={date}&granularity=day|week`
  - [ ] Query: COUNT records by status per day/week
  - [ ] Thêm cột `failure_reason` vào health_records (V019): `timeout`, `low_confidence`, `api_error`, `invalid_file`
  - [ ] Response: `{ data: [{ date, success, failed, failureBreakdown: {...} }] }`
- [ ] Task 2 — Web: Upload quality charts (AC: #1, #3, #4)
  - [ ] Trong analytics page: stacked bar chart (recharts BarChart)
  - [ ] Màu: xanh lá (success), đỏ (fail)
  - [ ] Threshold line at 95% success rate (dashed horizontal line)
  - [ ] Drill-down: click bar → modal với failure breakdown pie chart
- [ ] Task 3 — Tests (AC: #1, #2)
  - [ ] `AnalyticsServiceTest`: upload quality query, failure rate calculation

## Dev Notes

### Database Update

```sql
-- V019__add_failure_reason_to_health_records.sql
ALTER TABLE health_records ADD COLUMN failure_reason VARCHAR(50);
-- Values: 'timeout', 'low_confidence', 'api_error', 'invalid_file', NULL (success)
```

### API Response

```json
{
  "data": {
    "summary": { "totalUploads": 500, "successRate": 0.94, "failureRate": 0.06 },
    "daily": [{
      "date": "2026-03-17",
      "success": 47,
      "failed": 3,
      "failureBreakdown": { "timeout": 2, "low_confidence": 1 }
    }]
  }
}
```

### Recharts Stacked Bar

```typescript
<BarChart data={dailyData}>
  <Bar dataKey="success" stackId="a" fill="#10B981" />
  <Bar dataKey="failed" stackId="a" fill="#EF4444" />
  <ReferenceLine y={95} stroke="#F59E0B" strokeDasharray="3 3" label="95% target" />
</BarChart>
```

### References

- [Source: epics.md#Story-8.3]
- [Source: architecture.md#Giám-Sát-&-Quan-Sát]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
