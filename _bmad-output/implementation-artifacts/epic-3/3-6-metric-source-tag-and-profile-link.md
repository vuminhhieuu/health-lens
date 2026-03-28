# Story 3.6: Gán nguồn dữ liệu từng chỉ số và lưu vào đúng hồ sơ

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
I want mỗi chỉ số có tag nguồn OCR/manual và gắn đúng hồ sơ,
so that tôi truy vết độ tin cậy dữ liệu về sau.

## Acceptance Criteria

1. **Given** người dùng xác nhận lưu bản ghi, **When** hệ thống persist health record, **Then** mỗi metric được lưu kèm `source` tag (`ocr` hoặc `manual`).
2. **Given** health record được lưu, **When** kiểm tra DB, **Then** `profile_id` khớp với profile mà user đã chọn khi upload.
3. **Given** user sở hữu nhiều profile, **When** xem danh sách record theo profile, **Then** chỉ thấy records của profile đó, không thấy records của profile khác.
4. **Given** health record đã lưu, **When** hiển thị chi tiết, **Then** mỗi metric card hiển thị badge "OCR" hoặc "Nhập tay" (UX-DR4 source tagging).
5. **Given** một profile được share với family member, **When** gia đình xem records, **Then** source tag vẫn hiển thị đầy đủ.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Final confirm save với profile binding (AC: #1, #2)
  - [ ] `POST /api/v1/health-records/{recordId}/confirm` đảm bảo `profile_id` đã set từ step upload-url
  - [ ] Validate: `profile_id` phải thuộc user đang đăng nhập (RBAC check)
  - [ ] Update `source_type` của record dựa trên analysis metric sources
  - [ ] Set `status = 'done'`, set `exam_date` nếu thiếu (default: ngày hôm nay)
- [ ] Task 2 — Backend: Records by profile query (AC: #3)
  - [ ] `GET /api/v1/profiles/{profileId}/health-records` với pagination
  - [ ] Filter theo `profile_id`, chỉ trả về records của profile thuộc user hiện tại
  - [ ] Sort: `created_at DESC`
- [ ] Task 3 — Web (Phase 1) / Mobile (Phase 2): Source tag display (AC: #4)
  - [ ] MetricCard component thêm source badge
  - [ ] Badge: "OCR detected" (chip xanh dương) vs "Manual input" (chip xám)
  - [ ] Dùng shared constants `METRIC_SOURCE`
- [ ] Task 4 — Tests (AC: #1, #2, #3)
  - [ ] `HealthRecordServiceTest`: profile binding, ownership validation, query filter

## Dev Notes

### Source Badge UI (UX-DR4)

```typescript
// SourceBadge component
const SourceBadge = ({ source }: { source: 'ocr' | 'manual' }) => (
  <Badge variant={source === 'ocr' ? 'blue' : 'gray'}>
    {source === 'ocr' ? '🔍 OCR detected' : '✏️ Manual input'}
  </Badge>
);
```

### API

```
GET /api/v1/profiles/{profileId}/health-records?page=0&limit=20
Response 200: {
  "data": [{ id, examDate, status, sourceType, metrics, createdAt }],
  "pagination": { page, limit, total, totalPages },
  "meta": {...}
}
```

### Ownership Chain

```
User → owns → Profile → has → HealthRecord
           → can share → ProfileShare (Epic 6)
```

Mọi query health records phải join qua profiles để verify ownership.

### References

- [Source: architecture.md#Mẫu-Phân-Quyền]
- [Source: ux-design-specification.md#UX-DR4]
- [Source: epics.md#Story-3.6]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
