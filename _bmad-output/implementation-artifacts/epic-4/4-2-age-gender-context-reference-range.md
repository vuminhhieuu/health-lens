# Story 4.2: Áp dụng ngưỡng tham chiếu theo tuổi và giới tính

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
I want ngưỡng tham chiếu phù hợp hồ sơ,
so that đánh giá bình thường/bất thường chính xác hơn.

## Acceptance Criteria

1. **Given** hồ sơ có tuổi và giới tính hợp lệ, **When** hệ thống phân loại chỉ số, **Then** reference range được chọn theo rule context-aware từ reference data.
2. **Given** profile thiếu birthDate hoặc gender, **When** phân loại, **Then** dùng reference range mặc định (không phân biệt tuổi/giới).
3. **Given** metric card hiển thị, **When** user xem reference range, **Then** có ghi chú "Theo [độ tuổi] [giới tính]" nếu áp dụng context-aware.
4. **Given** phân loại thực hiện, **When** kiểm tra audit, **Then** log nguồn rule đã áp dụng (metric_id + reference_range_id).

## Tasks / Subtasks

- [x] Task 1 — Backend: Context-aware reference range lookup (AC: #1, #2)
  - [x] Cập nhật `ReferenceDataService.classifyMetric()` để nhận `Profile` object
  - [x] Query bảng `reference_ranges` với filter: `gender = profile.gender OR gender IS NULL`, age range overlaps với `profile.birthDate`
  - [x] Priority: specific (gender + age) > gender-only > age-only > default (no filter)
  - [x] Log rule áp dụng vào audit bảng
- [x] Task 2 — Backend: Enrich metrics endpoint với context (AC: #3)
  - [x] `GET /api/v1/health-records/{recordId}?profileId={profileId}` trả về metrics với context-aware ranges
  - [x] Thêm field `rangeContext` trong response: `{ gender: "female", ageRange: "40-60" }`
- [x] Task 3 — Web (Phase 1) / Mobile (Phase 2): Display range context note (AC: #3)
  - [x] HealthMetricCard expanded state thêm note: "Ngưỡng áp dụng cho: Nữ, 40-60 tuổi"
  - [x] Nếu dùng default range: "Ngưỡng tham chiếu chung"
- [x] Task 4 — Tests (AC: #1, #2, #4)
  - [x] `ReferenceDataServiceTest`: context lookup với profile full, profile thiếu tuổi, profile thiếu giới tính

## Dev Notes

### Reference Range Priority Logic

```java
// Trong ReferenceDataService
Optional<ReferenceRange> findBestRange(String metricName, Profile profile) {
    // 1. gender + age match
    // 2. gender match only
    // 3. age match only
    // 4. default (gender=null, age range=null)
    // Trả về Optional.empty() nếu không có gì
}
```

### Response Enrichment

```json
{
  "metrics": [{
    "name": "Glucose",
    "value": "5.4",
    "unit": "mmol/L",
    "referenceRange": { "min": 3.9, "max": 6.1 },
    "status": "normal",
    "rangeContext": { "gender": "female", "ageRange": "18-45" }
  }]
}
```

### References

- [Source: epics.md#Story-4.2]
- [Source: architecture.md#Kiến-Trúc-Dữ-Liệu]

## Dev Agent Record

### Agent Model Used

Codex 5.3

### Debug Log References

- Updated `ReferenceDataService` to classify with `Profile`, apply specificity priority, and log applied rule (`metric_id`, `reference_range_id`).
- Added repository query to load all active ranges by metric and select best-fit range in service layer.
- Enriched health record detail flow with optional `profileId` query param and per-metric `rangeContext`.
- Updated web metric card to show context note ("Ngưỡng áp dụng cho: ...") and fallback "Ngưỡng tham chiếu chung".
- Expanded `ReferenceDataServiceTest` for full profile and missing age/gender fallback scenarios.
- Could not execute Gradle tests in this sandbox because Gradle wrapper download is blocked (`UnknownHostException: services.gradle.org`).
### Completion Notes List

- Implemented context-aware reference range selection with explicit priority: gender+age > gender-only > age-only > default.
- Added `rangeContext` to API response model and wired it from backend classification to web display.
- Enabled `GET /api/v1/health-records/{recordId}?profileId={profileId}` to override profile context when needed.
- Added tests for rule priority and fallback behavior when profile lacks birth date or gender.
### File List

- `apps/api/src/main/java/com/healthlens/api/repository/ReferenceRangeRepository.java`
- `apps/api/src/main/java/com/healthlens/api/dto/RangeContextDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/MetricClassificationDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/MetricDto.java`
- `apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`
- `apps/api/src/test/java/com/healthlens/api/service/ReferenceDataServiceTest.java`
- `apps/web/src/components/ui/HealthMetricCard.tsx`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`

## Change Log

- 2026-04-26: Completed Story 4.2 implementation for context-aware reference ranges, response/UI range context, and service tests.

Status: review
