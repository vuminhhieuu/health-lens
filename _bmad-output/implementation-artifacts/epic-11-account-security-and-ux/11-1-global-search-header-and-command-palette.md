# Story 11.1: Tìm kiếm toàn cục — Header & Command Palette

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a người dùng đã đăng nhập,
I want tìm nhanh hồ sơ, kết quả khám và điều hướng từ thanh search trên header,
so that tôi không phải mở từng trang để tra cứu.

## Acceptance Criteria

1. **Given** người dùng ở bất kỳ trang dashboard nào, **When** focus vào ô search header hoặc nhấn `Ctrl/Cmd+K`, **Then** mở command palette (dialog/modal) với placeholder tiếng Việt: ví dụ "Tìm hồ sơ, kết quả khám...".
2. **Given** người dùng gõ ≥2 ký tự, **When** debounce 300ms, **Then** hiển thị nhóm kết quả: (a) Hồ sơ owned + shared, (b) Kết quả khám (tối đa 10, sort theo ngày khám mới nhất), (c) Điều hướng nhanh tĩnh.
3. **Given** chọn hồ sơ owned, **When** Enter/click, **Then** điều hướng `/profiles/{profileId}/history`.
4. **Given** chọn hồ sơ shared (view-only), **When** Enter/click, **Then** điều hướng đúng route shared đang dùng (tham chiếu `shared-profiles` flow hiện tại).
5. **Given** chọn health record, **When** Enter/click, **Then** điều hướng `/health-records/review/{recordId}`.
6. **Given** không có kết quả, **When** hiển thị, **Then** empty state tiếng Việt; không lỗi console.
7. **Given** API lỗi, **When** search, **Then** hiển thị lỗi qua `notify.error` + fallback nhóm điều hướng tĩnh vẫn hoạt động.
8. **Given** trang `/profiles`, **When** dùng ô search cục bộ trang, **Then** chỉ lọc danh sách gia đình — **không** gọi chung API với header (hai luồng tách biệt).

## Tasks / Subtasks

- [ ] Task 1 — UX & wiring header (AC: #1)
  - [ ] Gắn `onFocus`/`onClick` input tại `AuthenticatedTopHeader.tsx` mở palette
  - [ ] Global hotkey `Ctrl/Cmd+K` trong `(dashboard)/layout.tsx`
  - [ ] Ẩn/disable search trên viewport mobile nếu chưa có UX mobile (document trong story)
- [ ] Task 2 — Component `GlobalSearchCommandPalette` (AC: #1, #2, #6, #7)
  - [ ] Dialog accessible (`role="dialog"`, focus trap, `Escape` đóng)
  - [ ] Keyboard: ↑↓ chọn, Enter mở, Esc đóng
  - [ ] Nhóm kết quả có heading (`Hồ sơ`, `Kết quả khám`, `Trang`)
- [ ] Task 3 — API search backend (AC: #2–#5, #7)
  - [ ] `GET /api/v1/search?q=&limit=` — đồng bộ `ApiRoutes.java` + `packages/shared/constants/api.ts`
  - [ ] Service: search profiles user owns + shared; health records user có quyền (owner hoặc share)
  - [ ] Không trả nội dung OCR/raw file trong payload
  - [ ] Rate limit nhẹ (reuse pattern public auth rate limit nếu có)
- [ ] Task 4 — Web hook `useGlobalSearch` (AC: #2, #7)
  - [ ] TanStack Query `enabled: query.length >= 2`
  - [ ] `staleTime` 30s; cancel in-flight khi query đổi
- [ ] Task 5 — Tests (AC: #1–#8)
  - [ ] API: `GlobalSearchServiceTest` — ownership, empty q, shared profile
  - [ ] Web: test palette mở/đóng + chọn item mock (Vitest/RTL)

## Dev Notes

### BMad Help — Vị trí thanh search (đã phân tích)

| Vị trí | Vai trò | Quyết định |
|--------|---------|------------|
| `AuthenticatedTopHeader` | Tìm kiếm **toàn cục** + command palette | **Chính — triển khai story này** |
| `/profiles` | Lọc **cục bộ** thẻ hồ sơ gia đình | Giữ nguyên `searchQuery` state hiện tại |
| `/profiles/[id]/history` | Lọc **cục bộ** timeline kết quả | Giữ nguyên |
| `/settings/*` | Không có global search | Không thêm |

**Lý do:** UX-DR7 định nghĩa Web Dashboard = sidebar + **header**. Ô search đã có UI stub tại header (không có logic). Đặt search toàn cục ở header giúp truy cập từ Home, Health Records, Follow-up, Visit Summary mà không trùng với filter theo ngữ cảnh trang.

### Trạng thái hiện tại (brownfield)

```91:98:apps/web/src/components/layout/AuthenticatedTopHeader.tsx
          <div className="relative hidden sm:block">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#6d7a77]" />
            <input
              type="text"
              placeholder="Tìm kiếm..."
              className="w-64 rounded-full bg-[#e9f6f3] py-2 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-[#00685f]/20"
            />
```

- `/profiles` đã có search client-side theo `displayName` — **không thay thế**.
- Admin có `reference-data/search` — **không** dùng cho user global search.

### API đề xuất

```
GET /api/v1/search?q={query}&limit=15
Authorization: Bearer ...

Response 200:
{
  "data": {
    "profiles": [{ "id", "displayName", "isShared", "profileId" }],
    "healthRecords": [{ "id", "profileId", "profileDisplayName", "examDate", "title" }],
    "navigation": []  // optional — có thể hardcode phía client
  }
}
```

- `q` min length 2; trim; max 100 chars.
- Search profiles: `ILIKE` trên `display_name` + `notes` (không search email người khác).
- Search records: join profile ownership/share; `ILIKE` exam type / metric summary field nếu có — tránh scan full OCR text (NFR privacy).

### Điều hướng tĩnh (client)

| Label | href |
|-------|------|
| Trang chủ | `/home` |
| Kết quả khám | `/health-records` |
| Hồ sơ gia đình | `/profiles` |
| Hồ sơ cá nhân | `/settings/profile` |
| Cài đặt | `/settings` |

### Architecture compliance

- REST `/api/v1/...`, RFC 7807 errors.
- Không log `q` chứa PII nhạy cảm ở mức verbose — chỉ correlation id.
- Dùng `apiClient.ts` cho mọi request.
- Đồng bộ route constants: `ApiPaths` + `ApiRoutes.java`.

### File structure (dự kiến)

**Backend:**
- `apps/api/.../controller/GlobalSearchController.java`
- `apps/api/.../service/GlobalSearchService.java`
- `apps/api/.../dto/response/GlobalSearchResponse.java`
- `apps/api/src/main/resources/db/migration/V0xx__search_indexes_optional.sql` (optional GIN/trgm nếu cần)

**Frontend:**
- `apps/web/src/components/features/search/GlobalSearchCommandPalette.tsx`
- `apps/web/src/hooks/useGlobalSearch.ts`
- Sửa: `AuthenticatedTopHeader.tsx`, `(dashboard)/layout.tsx`

### Testing requirements

- `cd apps/api && ./gradlew test --tests '*GlobalSearch*'`
- `cd apps/web && pnpm test` cho component palette
- Manual: Cmd+K từ `/home`, `/health-records`; verify `/profiles` local search vẫn độc lập

### Dependencies

- **Khuyến nghị** làm sau `11-4-shared-dashboard-workflow-components` nếu team muốn dùng `PageSearchField` — không bắt buộc cho MVP palette.

### References

- [Source: _bmad-output/planning-artifacts/account-security-and-ux-enhancements-epics-and-stories.md#story-111]
- [Source: docs/project-context.md#Non-Negotiable-Synchronization-Rules]
- [Source: _bmad-output/planning-artifacts/epics.md#UX-DR7]
- [Source: apps/web/src/app/(dashboard)/profiles/page.tsx — local search pattern]

## Dev Agent Record

### Agent Model Used

(pending)

### Debug Log References

### Completion Notes List

### File List
