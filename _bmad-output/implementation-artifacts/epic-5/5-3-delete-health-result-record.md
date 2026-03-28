# Story 5.3: Xóa một kết quả khám

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
I want xóa bản ghi khám không còn cần thiết,
so that lịch sử của tôi luôn chính xác và gọn gàng.

## Acceptance Criteria

1. **Given** người dùng là owner của hồ sơ, **When** xác nhận xóa một bản ghi, **Then** hệ thống soft-delete bản ghi và cập nhật timeline ngay.
2. **Given** family member xem hồ sơ, **When** xem history, **Then** nút xóa không hiển thị.
3. **Given** xóa thành công, **When** kiểm tra audit log, **Then** có entry: userId, action='DELETE_HEALTH_RECORD', recordId, timestamp.
4. **Given** hard-delete sau khi soft-delete 30 ngày, **When** kiểm tra DB sau 30 ngày, **Then** record đã bị xóa vật lý và file S3 đã bị xóa.
5. **Given** confirm dialog, **When** hiển thị, **Then** nêu rõ: "Hành động này không thể hoàn tác. File ảnh/PDF gốc cũng sẽ bị xóa."

## Tasks / Subtasks

- [ ] Task 1 — Backend: Soft delete endpoint (AC: #1, #3)
  - [ ] Thêm cột `deleted_at` vào `health_records` (Flyway migration `V013__add_soft_delete_to_health_records.sql`)
  - [ ] `DELETE /api/v1/health-records/{recordId}` — set `deleted_at = now()`
  - [ ] Ownership check: hanya owner yang boleh delete (403 jika bukan owner)
  - [ ] Ghi audit log qua `@Auditable` AOP annotation
  - [ ] Loại bỏ soft-deleted records khỏi tất cả query (thêm `WHERE deleted_at IS NULL`)
- [ ] Task 2 — Backend: Scheduled hard delete (AC: #4)
  - [ ] `@Scheduled` job chạy daily: tìm records `deleted_at < 30 ngày ago`
  - [ ] Xóa file S3/MinIO trước, sau đó xóa record DB
- [ ] Task 3 — Web: Delete confirmation flow (AC: #1, #5)
  - [ ] Delete button (icon thùng rác) trong history item hoặc record detail — chỉ hiện với owner
  - [ ] Confirmation dialog với text cảnh báo rõ ràng
  - [ ] Sau delete: remove khỏi TanStack Query cache (`queryClient.setQueryData`)
- [ ] Task 4 — Mobile (Phase 2): Delete flow (AC: #1, #5)
  - [ ] Long-press hoặc swipe-to-delete trên history item
  - [ ] Alert.alert() native như confirm dialog
- [ ] Task 5 — Tests (AC: #1, #3)
  - [ ] `HealthRecordServiceTest`: delete thành công, 403 không phải owner, audit log

## Dev Notes

### Audit Log (via Spring AOP)

```java
@Auditable(action = "DELETE_HEALTH_RECORD")
public void deleteHealthRecord(UUID recordId, UUID userId) { ... }
// AOP interceptor tự ghi: userId, action, resource_type="HEALTH_RECORD", resource_id=recordId, timestamp, ip
```

### Soft Delete Pattern

```java
// Tất cả repository queries phải thêm:
@Query("SELECT h FROM HealthRecord h WHERE h.profileId = :profileId AND h.deletedAt IS NULL")
List<HealthRecord> findActiveByProfileId(@Param("profileId") UUID profileId);
```

Hoặc dùng `@SQLRestriction("deleted_at IS NULL")` trên entity.

### TanStack Query Optimistic Delete

```typescript
const deleteMutation = useMutation({
  mutationFn: (recordId) => deleteRecord(recordId),
  onMutate: async (recordId) => {
    await queryClient.cancelQueries({ queryKey: ['health-records', profileId] });
    const prev = queryClient.getQueryData(['health-records', profileId]);
    queryClient.setQueryData(['health-records', profileId], (old) =>
      old.filter(r => r.id !== recordId)
    );
    return { prev };
  },
  onError: (err, vars, ctx) => queryClient.setQueryData(['health-records', profileId], ctx.prev),
});
```

### References

- [Source: architecture.md#Chiến-Lược-Audit-Logging]
- [Source: epics.md#Story-5.3]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
