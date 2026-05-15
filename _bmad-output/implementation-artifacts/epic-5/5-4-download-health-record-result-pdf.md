# Story 5.4: Tải xuống PDF của một kết quả khám

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 5 — phạm vi **web** và backend/API. Chức năng này là xuất/tải file PDF tổng hợp của **một health record cụ thể** từ dữ liệu đã lưu trong HealthLens, không mở rộng sang export toàn bộ hồ sơ hay export hàng loạt.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Health Record Detail Page | `projects/578519912546445367/screens/3c9f3f9c951b4e45aa713c6da552be29` | Reuse từ Story 5.2 | Reuse từ Story 5.2 |

*Ghi chú:* Trang detail hiện đã có action icon `FileDown` với label "Tải PDF" trong `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`, nhưng đang `disabled`. Story này biến action đó thành workflow tải PDF thật.

## Story

As a người dùng có quyền xem một kết quả khám,
I want tải xuống PDF của kết quả khám đó,
so that tôi có thể lưu trữ, in ấn, hoặc gửi kết quả đã được HealthLens tổng hợp cho bác sĩ/người thân khi cần.

## Acceptance Criteria

1. **Given** tôi là owner, shared profile viewer/editor, hoặc record-level viewer có quyền xem health record, **When** mở trang chi tiết kết quả khám đã có dữ liệu, **Then** nút "Tải PDF" hiển thị active và cho phép tải file PDF của đúng record đó.
2. **Given** người dùng không có quyền xem record, record đã bị xóa mềm, hoặc share đã bị thu hồi, **When** gọi API tải PDF, **Then** backend trả 403/404 theo pattern hiện có và không tạo link/file PDF.
3. **Given** PDF được tạo, **When** mở file, **Then** nội dung bao gồm tối thiểu: tên hồ sơ, ngày khám, loại phiếu, cơ sở y tế, chẩn đoán/ghi chú nếu có, danh sách chỉ số với giá trị/đơn vị/ngưỡng tham chiếu/trạng thái, và disclaimer "Thông tin chỉ mang tính tham khảo, không thay thế tư vấn y tế chuyên môn."
4. **Given** record có explanation và recommendations đã hiển thị trong detail page, **When** tải PDF, **Then** PDF bao gồm các giải thích/góc nhìn sức khỏe và khuyến nghị sẵn có hoặc có thể tải lazy-load qua service hiện có mà không làm fail toàn bộ download nếu LLM/RAG tạm thời lỗi.
5. **Given** record được upload từ ảnh hoặc PDF gốc, **When** tải PDF kết quả, **Then** file tải về là PDF tổng hợp của HealthLens, không phải chỉ là presigned URL của file gốc; nếu muốn hiển thị hồ sơ gốc trong PDF thì chỉ chèn thumbnail/phần phụ khi kỹ thuật cho phép.
6. **Given** tải PDF thành công, **When** kiểm tra audit log, **Then** có entry cho actor, timestamp, `healthRecordId`, `profileId`, action `DOWNLOAD_HEALTH_RECORD_PDF`, và `shareScope` nếu actor là viewer được chia sẻ.
7. **Given** API tạo PDF mất thời gian, **When** user bấm tải, **Then** UI hiển thị loading state, ngăn double-click, hiển thị toast/error tiếng Việt nếu thất bại, và không làm mất trang detail hiện tại.
8. **Given** file PDF được tải về, **When** trình duyệt lưu file, **Then** filename thân thiện và ổn định theo mẫu `healthlens-{profileName}-{examDate}-{recordId-short}.pdf`, được sanitize để không có ký tự không hợp lệ.

## Tasks / Subtasks

- [x] Task 1 — Backend: PDF download endpoint (AC: #1, #2, #3, #5, #8)
  - [x] Thêm route `GET /api/v1/health-records/{recordId}/pdf` trong `HealthRecordController`
  - [x] Thêm constant `ApiRoutes` nếu cần và `ApiPaths.HEALTH_RECORDS.DOWNLOAD_PDF(recordId)` trong `packages/shared/constants/api.ts`
  - [x] Reuse `HealthRecordService.loadAccessibleRecord` logic để chấp nhận owner, profile share, và record-level share; không tạo logic phân quyền riêng lẻ
  - [x] Trả về response `application/pdf` với `Content-Disposition: attachment; filename="...pdf"`
  - [x] Không dùng trực tiếp `fileUrl` presigned hiện có làm kết quả download PDF tổng hợp
- [x] Task 2 — Backend: PDF generation service (AC: #3, #4, #5)
  - [x] Tạo service riêng, ví dụ `HealthRecordPdfService`, để render PDF từ `HealthRecord`, `Profile`, metrics đã enrich, explanations/recommendations nếu có
  - [x] Dùng thư viện Java PDF đã có trong project nếu tồn tại; nếu chưa có, thêm dependency nhỏ và rõ ràng vào Gradle, ưu tiên OpenPDF hoặc Apache PDFBox
  - [x] Đảm bảo font hỗ trợ tiếng Việt dấu đầy đủ; nếu font mặc định không hỗ trợ Unicode, bundle font phù hợp trong resources và cấu hình embed
  - [x] Format bảng metrics để không tràn dòng với tên chỉ số dài, đơn vị dài, hoặc ngưỡng tham chiếu dài
  - [x] Nếu explanation/recommendation service lỗi, log warning và vẫn tạo PDF với phần "Chưa có giải thích/khuyến nghị tại thời điểm tải xuống"
- [x] Task 3 — Backend: Audit và security guardrails (AC: #2, #6)
  - [x] Ghi audit action `DOWNLOAD_HEALTH_RECORD_PDF` sau khi authorization pass và PDF được tạo thành công
  - [x] Audit metadata gồm `healthRecordId`, `profileId`, `shareScope`, `viewerId`/actor id
  - [x] Không ghi raw metric values vào log application/audit metadata
  - [x] Đảm bảo soft-deleted records không download được
- [x] Task 4 — Web: Enable nút "Tải PDF" trên detail page (AC: #1, #7, #8)
  - [x] Cập nhật `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`: bỏ disabled cho nút `FileDown` khi record detail hợp lệ và user có quyền xem
  - [x] Gọi API bằng `apiClient` với `responseType: "blob"` và tạo object URL để trigger browser download
  - [x] Hiển thị loading spinner trong icon button, disable trong lúc tải, và toast/error tiếng Việt nếu fail
  - [x] Giữ action có `title` và `aria-label` để đảm bảo accessibility
  - [x] Không hiển thị/cho phép edit/delete cho viewer read-only; download PDF vẫn được phép nếu có quyền xem
- [x] Task 5 — Tests (AC: #1-#8)
  - [x] Service/controller tests: owner download thành công và response có content type PDF
  - [x] Service/controller tests: profile shared viewer và record-level viewer download được
  - [x] Service/controller tests: unauthorized, revoked share, và soft-deleted record bị chặn
  - [x] Service tests: PDF content có profile name, exam date, metric value/status, disclaimer
  - [x] Web lint/typecheck cho detail page và API constant

## Dev Notes

### Current state

- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` đã có nút:

```tsx
<button
  type="button"
  title="Tải PDF"
  aria-label="Tải PDF"
  disabled
>
  <FileDown className="h-4 w-4" />
</button>
```

- `HealthRecordDetailResponse` hiện trả `fileUrl`, nhưng đây là presigned URL của file gốc từ `StorageService.generateDownloadUrl(record.getFileKey(), Duration.ofHours(1))`.
- Story này cần PDF tổng hợp HealthLens, vì vậy backend nên tạo PDF từ dữ liệu record thay vì expose lại file gốc.

### Access Rules

- Download PDF là read action. Cho phép:
  - owner của record
  - viewer/editor của profile share active
  - viewer/editor của record share active
- Không cho phép:
  - user không có share active
  - record đã soft-delete (`deleted_at IS NOT NULL`)
  - record không tồn tại
- Nên reuse logic access trong `HealthRecordService` để giữ đồng bộ với `GET /api/v1/health-records/{recordId}`, explanations, recommendations, và Story 6.5 record-level share.

### API Proposal

```http
GET /api/v1/health-records/{recordId}/pdf
Accept: application/pdf

200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="healthlens-nguyen-van-a-2026-05-15-a1b2c3d4.pdf"
```

Frontend route constant:

```typescript
ApiPaths.HEALTH_RECORDS.DOWNLOAD_PDF = (id: string) =>
  `/api/v1/health-records/${id}/pdf`
```

### PDF Content

Recommended sections:

1. Header: "HealthLens - Kết quả khám" + ngày tạo PDF.
2. Thông tin hồ sơ: tên hồ sơ, ngày khám, loại phiếu, bệnh viện/phòng xét nghiệm, chẩn đoán/ghi chú nếu có.
3. Tóm tắt trạng thái: bình thường/cần chú ý/bất thường dựa trên metrics.
4. Bảng chỉ số: tên chỉ số, giá trị, đơn vị, ngưỡng tham chiếu, trạng thái.
5. Giải thích và khuyến nghị: lấy từ data/service hiện có, fallback mềm nếu không có.
6. Disclaimer y tế bắt buộc.

### Frontend Integration Points

- `packages/shared/constants/api.ts`: thêm `HEALTH_RECORDS.DOWNLOAD_PDF`.
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`: enable `FileDown` action, blob download, loading/error state.
- `apps/web/src/lib/api/apiClient.ts`: reuse revoked-access handling hiện có cho `/health-records` nếu backend trả `profile-access-revoked`.

### Backend Integration Points

- `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordPdfService.java` (new)
- `apps/api/src/main/java/com/healthlens/api/service/StorageService.java` chỉ nên dùng nếu cần asset/file gốc phụ trợ, không dùng làm kết quả chính
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/controller/HealthRecordControllerTest.java`

### References

- [Source: epics.md#Story-5.2]
- [Source: epics.md#Story-5.3]
- [Source: prd.md#FR24-FR26]
- [Source: architecture.md#FR24-FR28-Lịch-Sử]
- [Related: Story 5.2]
- [Related: Story 5.3]
- [Related: Story 6.5]

## Dev Agent Record

### Agent Model Used

Codex GPT-5

### Debug Log References

- 2026-05-15: Started implementation. BMad core config missing, continued with repo story defaults.
- `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.healthlens.api.controller.HealthRecordControllerTest --tests com.healthlens.api.service.HealthRecordServiceTest --tests com.healthlens.api.service.HealthRecordPdfServiceTest` — pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test` — pass.
- `pnpm --filter web test` — pass với 3 warning lint cũ ngoài story.
- `pnpm --filter web exec tsc --noEmit` — pass.
- `pnpm --filter @healthlens/shared build` — pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.healthlens.api.service.HealthRecordServiceTest --tests com.healthlens.api.service.HealthRecordPdfServiceTest` — pass sau review fixes.
- `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test` — pass sau review fixes.

### Completion Notes List

- Thêm endpoint `GET /api/v1/health-records/{recordId}/pdf` trả `application/pdf` attachment với filename sanitize theo profile/date/record id.
- Thêm `HealthRecordPdfService` dùng PDFBox để tạo PDF tổng hợp HealthLens từ record/profile/metrics/explanations/recommendations, không trả lại presigned URL file gốc.
- Reuse access-control hiện có qua `loadAccessibleRecord`: owner, profile share, record-level share được tải; unauthorized/revoked/soft-deleted bị chặn trước khi tạo PDF.
- Ghi audit `DOWNLOAD_HEALTH_RECORD_PDF` vào `health_record_audit_logs` với metadata `profileId`, `viewerId`, `shareScope`, `resourceType`.
- Enable nút "Tải PDF" trên web detail page với blob download, loading spinner, double-click guard và lỗi tiếng Việt.
- Thêm type shim `qrcode.react` để web typecheck chạy qua dependency đã khai báo trong package.
- Hardening sau code review: bỏ silent ASCII fallback cho PDF font và chỉ dùng Unicode font hợp lệ; nếu host không có font phù hợp thì fail rõ ràng.
- Hardening sau code review: bỏ gọi explanation theo từng metric trong luồng download PDF; PDF chỉ dùng explanation sẵn có trên metric data.

### File List

- apps/api/build.gradle.kts
- apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java
- apps/api/src/main/java/com/healthlens/api/dto/response/DownloadHealthRecordPdfResponse.java
- apps/api/src/main/java/com/healthlens/api/entity/HealthRecordAuditLog.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordPdfContext.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordPdfService.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java
- apps/api/src/main/resources/db/migration/V030__add_pdf_download_audit_metadata.sql
- apps/api/src/test/java/com/healthlens/api/controller/HealthRecordControllerTest.java
- apps/api/src/test/java/com/healthlens/api/service/HealthRecordPdfServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java
- apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx
- apps/web/src/types/qrcode.react.d.ts
- packages/shared/constants/api.ts

### Change Log

- 2026-05-15: Tạo draft story 5.4 cho chức năng tải xuống PDF của một kết quả khám.
- 2026-05-15: Implemented Story 5.4 backend PDF endpoint/generation/audit, web download action, tests, and validation; marked ready for review.
- 2026-05-15: Addressed code review findings for font portability and per-metric explanation latency in PDF download flow.
