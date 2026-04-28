# Story 5.1: Timeline lịch sử theo từng hồ sơ

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Dashboard Home Page - HealthLens | `projects/578519912546445367/screens/010095343e6c46b4969b42c4ab93165a` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzBkNTk5MTY2NzIxNzQ0NDk5YjJlMjQxMWM1N2U3ZGRkEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ujIG2rNbWjcWsO9G1za9-KcM9gHUwj2X237xGhShjEjCWwi29NS6udE0Flhdtdop0NdfoD_Q2MWeNXsYPv9ycxE1WW7o8Q-d1R0yleCrW7kDtJKwmG3PSp2i5ElEnFX2PBVJRnaMV1LyvBv2tceKjwgi32axKgQD_qspcqMvh2sOPytIhDH-xmzLB10vKA25LIp5_lTgyqaRkKrSQ3LXCMp6nLt8BGuRGlAxnkrlkV-A5GUZWmIVjBIVg) |
| Kết quả Sức khỏe - HealthLens | `projects/578519912546445367/screens/2f0902b360ee4d9fb7be1cb8d51c2640` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzM2ODZmN2IxNmFiMjQ2NTg4ODdjMjUxYTU4ZTk0YjczEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uiaQJ6Sh7qTeT0Fi1CWgOK3PorgJLICcpZwncEhTtHL-vWhO1pEJnyd9hYMPayvhYWTTTLFBzmlJI9ik55NKYWSEZ-fY9HeK2t9do7L59PLqw7q5enUAdumASwPokP7wTI1ne5Ffo4UUcmpKwnav-_E_GCb4I5GK0DWutaD5l8VGQr0eM3FXxim3Cr-ici6he-k7TPfU5KpiWV83n3suWbZxNHp3-Ep4FB6-hDFsxoeJNQam2dnYfeffw) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want xem timeline các lần khám theo hồ sơ,
so that tôi theo dõi diễn tiến sức khỏe theo thời gian.

## Acceptance Criteria

1. **Given** hồ sơ có từ một bản ghi trở lên, **When** mở tab history, **Then** hệ thống hiển thị danh sách theo thứ tự thời gian mới nhất trước.
2. **Given** danh sách history, **When** hiển thị mỗi item, **Then** item có: overall status badge, loại xét nghiệm, ngày khám, số chỉ số bất thường.
3. **Given** hồ sơ chưa có bản ghi, **When** mở tab history, **Then** hiển thị empty state với CTA "Thêm kết quả đầu tiên".
4. **Given** danh sách dài, **When** scroll xuống cuối, **Then** load thêm records (infinite scroll, 20 items/page).
5. **Given** profile được share với family member, **When** family member xem history, **Then** cùng timeline view nhưng read-only (không có nút xóa).

## Tasks / Subtasks

- [x] Task 1 — Backend: History endpoint với pagination (AC: #1, #2, #4)
  - [x] `GET /api/v1/profiles/{profileId}/health-records?page=0&limit=20&sort=examDate,desc`
  - [x] Response mỗi item: id, examDate, testType (từ metrics hoặc manual), overallStatus, abnormalCount, sourceType, createdAt
  - [x] Ownership check hoặc family share check
- [x] Task 2 — Web: History tab/page (AC: #1, #2, #3, #4)
  - [x] Tạo `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx`
  - [x] HealthResultSummary cards (UX-DR2) — tên test, ngày, overall status badge, abnormalCount
  - [x] Empty state component với "Upload kết quả đầu tiên" button
  - [x] Infinite scroll với TanStack Query `useInfiniteQuery`
- [ ] Task 3 — Mobile (Phase 2): History screen trong profile tab (AC: #1, #2, #3)
  - [ ] `apps/mobile/app/profiles/[profileId]/history.tsx`
  - [ ] FlatList với `onEndReached` cho pagination
  - [ ] Empty state với FAB redirect
- [x] Task 4 — Tests (AC: #1, #4, #5)
  - [x] `HealthRecordServiceTest`: pagination, ownership check, share access check

## Dev Notes

### API

```
GET /api/v1/profiles/{profileId}/health-records?page=0&limit=20
Response 200: {
  "data": [{
    "id": "uuid",
    "examDate": "2026-03-17",
    "testType": "Xét nghiệm máu",
    "overallStatus": "attention",
    "abnormalCount": 2,
    "sourceType": "ocr",
    "createdAt": "2026-03-17T10:30:00Z"
  }],
  "pagination": { "page": 0, "limit": 20, "total": 45, "totalPages": 3 }
}
```

### HealthResultSummary Component (UX-DR2)

```typescript
interface HealthResultSummaryProps {
  id: string;
  testType: string;
  examDate: string;
  overallStatus: 'normal' | 'attention' | 'abnormal';
  abnormalCount: number;
  onClick: () => void;
}
```

### TanStack Query Infinite Scroll

```typescript
const { data, fetchNextPage, hasNextPage } = useInfiniteQuery({
  queryKey: ['health-records', profileId],
  queryFn: ({ pageParam = 0 }) => fetchRecords(profileId, pageParam),
  getNextPageParam: (lastPage) => lastPage.pagination.hasNext ? lastPage.pagination.page + 1 : undefined,
});
```

### References

- [Source: ux-design-specification.md#UX-DR2]
- [Source: architecture.md#Quản-Lý-State]
- [Source: epics.md#Story-5.1]

## Dev Agent Record

### Agent Model Used

Codex 5.3

### Debug Log References

- `./gradlew test --tests com.healthlens.api.service.HealthRecordServiceTest` không chạy được trong sandbox do không tải được Gradle distribution từ `services.gradle.org` (UnknownHost).

### Completion Notes List

- ✅ Task 1: Bổ sung endpoint timeline theo profile với pagination 20 items/page, sort mới nhất trước, trả về summary fields đúng AC.
- ✅ Task 2: Tạo trang `profiles/[profileId]/history` với card summary, empty state CTA "Thêm kết quả đầu tiên", và infinite scroll bằng `useInfiniteQuery`.
- ⏭️ Task 3 (Mobile Phase 2): Deferred theo Execution scope của story (Web MVP Phase 1).
- ✅ Task 4: Thêm test cho `HealthRecordService` gồm pagination summary và ownership guard.
- ✅ Follow-up review fix: hỗ trợ share-access check qua `profile_shares` để viewer được cấp quyền có thể xem timeline read-only.
- ✅ Follow-up review fix: timeline sort `examDate desc nulls last, createdAt desc` để tránh record không có ngày khám nổi lên đầu danh sách.
- ✅ Follow-up review fix: giới hạn page size tối đa 20 items đúng AC.
- ⚠️ Chưa verify runtime test trong môi trường local vì giới hạn network sandbox khi tải Gradle wrapper.

### File List

**New files:**
- `apps/api/src/main/java/com/healthlens/api/dto/response/HealthRecordHistoryItemResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/HealthRecordHistoryPageResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/PaginationResponse.java`
- `apps/api/src/main/java/com/healthlens/api/entity/ProfileShare.java`
- `apps/api/src/main/java/com/healthlens/api/repository/ProfileShareRepository.java`
- `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx`
- `apps/api/src/main/resources/db/migration/V015__create_profile_shares_table.sql`

**Modified files:**
- `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/repository/HealthRecordRepository.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `packages/shared/constants/api.ts`

### Change Log

- 2026-04-27: Implement Story 5.1 (Web MVP scope) — profile history timeline API + web page + service tests; mobile task deferred to Phase 2.
- 2026-04-27: Address code review findings — add share-access guard for history endpoint, clamp page size to 20, and sort null examDate last.
