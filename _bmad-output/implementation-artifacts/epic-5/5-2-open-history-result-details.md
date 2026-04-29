# Story 5.2: Xem chi tiết một bản ghi từ timeline

Status: done

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

- [x] Task 1 — Reuse từ Story 4.5 (AC: #1, #2, #3)
  - [x] Detail page `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` được tái sử dụng cho lịch sử
  - [x] Navigate từ history item → `/health-records/review/{recordId}`
  - [x] Ensure back navigation về history page (giữ browser history stack qua client navigation)
- [x] Task 2 — Read-only mode cho family member (AC: #4)
  - [x] Detect context: is owner or family viewer? (từ API response `isOwner: boolean`)
  - [x] Hide action buttons (delete, edit) khi `isOwner = false`
- [x] Task 3 — Mobile navigation (AC: #1)
  - [x] Deferred theo execution scope Phase 1 (Web MVP only)
  - [x] Mobile back stack sẽ được triển khai ở Phase 2

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

Codex 5.3

### Debug Log References

- `pnpm exec eslint "src/app/(dashboard)/profiles/[profileId]/history/page.tsx" "src/app/(dashboard)/health-records/review/[recordId]/page.tsx"` (pass)

### Completion Notes List

- Added history-item action to open result detail from timeline to existing detail route.
- Reused existing record detail page and enforced owner/family read-only behavior using `isOwner` from API.
- Family viewer can still see full metric cards/explanations but cannot edit/delete/save.
- Mobile subtasks are explicitly deferred by story execution scope (Phase 1 web-only).

### File List
- apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx
- apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx

### Change Log

- 2026-04-28: Implemented web detail navigation from history timeline and family read-only mode for shared record detail view. Marked story ready for review.
