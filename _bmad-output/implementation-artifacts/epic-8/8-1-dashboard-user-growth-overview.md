# Story 8.1: Dashboard tổng quan tăng trưởng người dùng

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
I want xem tổng user all-time và theo tháng,
so that tôi theo dõi đà tăng trưởng sản phẩm.

## Acceptance Criteria

1. **Given** dữ liệu người dùng đã được ghi nhận, **When** mở trang thống kê admin, **Then** hiển thị tổng registered users all-time và monthly trend.
2. **Given** chart tăng trưởng, **When** hiển thị, **Then** trục X: tháng (6 tháng gần nhất), trục Y: số user mới trong tháng.
3. **Given** dashboard, **When** load, **Then** tất cả số liệu được tính từ database thực (không hardcode).
4. **Given** admin chọn range khác, **When** thay đổi filter, **Then** chart cập nhật tương ứng.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Analytics endpoints (AC: #1, #2, #3)
  - [ ] `GET /api/v1/admin/analytics/users?from={date}&to={date}`
  - [ ] Response: `{ totalUsers, monthlyGrowth: [{ month, newUsers }] }`
  - [ ] Query: `SELECT DATE_TRUNC('month', created_at), COUNT(*) FROM users GROUP BY 1 ORDER BY 1 DESC LIMIT 12`
  - [ ] Cache Redis 1 giờ (analytics không cần real-time)
- [ ] Task 2 — Web: User analytics section (AC: #1, #2, #4)
  - [ ] Tạo `apps/web/src/app/admin/analytics/page.tsx`
  - [ ] Stat card: "Tổng người dùng: {N}" (large number)
  - [ ] Line chart (recharts): monthly new users, 6-12 tháng gần nhất
  - [ ] Date range picker để filter
- [ ] Task 3 — Tests (AC: #1, #3)
  - [ ] `AnalyticsServiceTest`: user count query, monthly breakdown

## Dev Notes

### Recharts (đã có trong web deps)

```typescript
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

<ResponsiveContainer width="100%" height={300}>
  <LineChart data={monthlyGrowth}>
    <XAxis dataKey="month" tickFormatter={m => format(new Date(m), 'MM/yyyy')} />
    <YAxis />
    <Tooltip />
    <Line type="monotone" dataKey="newUsers" stroke="#10B981" strokeWidth={2} />
  </LineChart>
</ResponsiveContainer>
```

### API Response

```json
{
  "data": {
    "totalUsers": 1250,
    "monthlyGrowth": [
      { "month": "2026-03", "newUsers": 180 },
      { "month": "2026-02", "newUsers": 220 }
    ]
  }
}
```

### References

- [Source: epics.md#Story-8.1]
- [Source: architecture.md#Giám-Sát-&-Quan-Sát]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
