# Story 5.2: Xem chi tiết một bản ghi từ timeline

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Health Record Detail Page | `projects/578519912546445367/screens/3c9f3f9c951b4e45aa713c6da552be29` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzc2ZDgwMGI2MWVjZjRjZjBhNjZlYmFhMGM1MThjMTQyEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhzldktcjBvfyA80T-sCTf9VYf7EEA0b8kZJqSNh2gb2l2nyuCMywxSBNQM_QjkdzH-eQvQMehBqSForVsETegaE_zrHY-5HTDbo7tHtOdGCCrnJPfo84-vpSeeO6ecq8xYdwkq5DqHc-zEg0abPd7o_U2M9-LTR7Coxv_9sKGYfag9-O6z0u1vdbo67DJ8abgHQa-7OSDXTdrfLdy-vcamk0AbOBtN-HT_uxCGuCfTJsUkiJ8HqlJRSQ) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want mở chi tiết từ một item timeline,
so that tôi xem đầy đủ chỉ số và giải thích của lần khám đó.

## Acceptance Criteria

1. **Given** timeline đã hiển thị, **When** tap vào một item, **Then** navigate tới màn hình chi tiết kết quả tương ứng.
2. **Given** màn hình chi tiết, **When** hiển thị, **Then** dữ liệu nhất quán với bản ghi đã lưu (không có discrepancy).
3. **Given** history detail, **When** view, **Then** đầy đủ metrics, explanations (lazy-load), recommendations theo cấu trúc UX-DR5.
4. **Given** family member xem history detail, **When** hiển thị, **Then** giống owner view nhưng không có nút xóa hoặc edit.

## Tasks / Subtasks

- [ ] Task 1 — Reuse từ Story 4.5 (AC: #1, #2, #3)
  - [ ] Detail page `apps/web/src/app/(dashboard)/records/[recordId]/page.tsx` đã có
  - [ ] Navigate từ history item → `/records/{recordId}`
  - [ ] Ensure back navigation về history page
- [ ] Task 2 — Read-only mode cho family member (AC: #4)
  - [ ] Detect context: is owner or family viewer? (từ API response `isOwner: boolean`)
  - [ ] Hide action buttons (delete, edit) khi `isOwner = false`
- [ ] Task 3 — Mobile navigation (AC: #1)
  - [ ] Từ history FlatList tap → navigate `app/records/[recordId]`
  - [ ] Back stack đúng về history screen

## Dev Notes

### Navigation Pattern

```typescript
// Web
router.push(`/records/${recordId}`);

// Mobile (Expo Router)
router.push(`/records/${recordId}`);
```

### isOwner Flag

```json
// GET /api/v1/health-records/{recordId}
{
  "data": {
    "id": "...",
    "isOwner": true,  // false nếu là family viewer
    "metrics": [...]
  }
}
```

Backend xác định `isOwner` dựa trên: record.profile.userId == jwtUserId

### References

- [Source: ux-design-specification.md#UX-DR5]
- [Source: epics.md#Story-5.2]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
