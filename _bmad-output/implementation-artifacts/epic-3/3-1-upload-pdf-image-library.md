# Story 3.1: Upload file PDF và ảnh từ thư viện

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Tải kết quả xét nghiệm - HealthLens | `projects/578519912546445367/screens/0e048c6789e64ceab2f78815dac26939` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzhmNWU0YTJjZWIxNDQ1YTQ4YWU0OTIwYjg5MTdmOWEzEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uih-gmx0WEcaNtBI0nOUGEU-VaAuvT6dk5EdImXwzQSNPe5LWZYdEGPIdZ7yri_Qs77nYCmFFkRdZe6oH5JLm0Ts9oXPtTLEQRDa2PaPN9pbD-dNkofND0B4hdmP9UYvUlouwc53AStrOwHTRlGpmnYT0vV3CNnHaSUnJPCaRYI1Ct2uT3xXUKKNmR0t5g6W3yhMrxUgfShQTzw5YxcSfF0Q7Ae8V9csmhrgaq3Udu_YRReYt0ZBztw) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want upload PDF hoặc ảnh từ thiết bị,
so that tôi đưa kết quả khám vào hệ thống nhanh chóng.

## Acceptance Criteria

1. **Given** người dùng đã consent và chọn hồ sơ, **When** upload file PDF/JPG/PNG hợp lệ (≤20MB), **Then** hệ thống nhận file qua pre-signed URL và tạo OCR processing job.
2. **Given** file quá 20MB hoặc sai định dạng, **When** chọn file, **Then** hiển thị lỗi trước khi upload (client-side validation).
3. **Given** upload thành công, **When** theo dõi tiến trình, **Then** hiển thị processing screen với progress animation (UX-DR3).
4. **Given** file upload, **When** kiểm tra S3/MinIO, **Then** file được lưu ở thư mục `health-records/{userId}/{profileId}/{recordId}/original.{ext}`.
5. **Given** upload xong, **When** kiểm tra DB, **Then** có health_record với status `processing` và job_id trong Redis queue.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Pre-signed URL endpoint (AC: #1, #4)
  - [ ] `POST /api/v1/health-records/upload-url` với body `{ profileId, fileType: "pdf"|"image" }`
  - [ ] Tạo pre-signed URL với MinIO SDK (dev) / AWS SDK (prod), TTL 15 phút
  - [ ] Path pattern: `health-records/{userId}/{profileId}/{uuid}/original.{ext}`
  - [ ] Trả về: `{ uploadUrl, recordId, fileKey }`
- [ ] Task 2 — Backend: Confirm upload + trigger OCR (AC: #5)
  - [ ] `POST /api/v1/health-records/{recordId}/confirm-upload`
  - [ ] Tạo `health_records` row với status `processing`
  - [ ] Push job vào Redis Streams: `{ recordId, fileKey, profileId }`
  - [ ] Flyway migration `V010__create_health_records_table.sql`
- [ ] Task 3 — Backend: OCR worker (basic) (AC: #5)
  - [ ] Spring `@EventListener` hoặc Redis Streams consumer
  - [ ] Gọi OCR API (Google Vision / AWS Textract) với file từ S3
  - [ ] Timeout 15 giây (NFR-I1), fallback → update record status `ocr_failed`
- [ ] Task 4 — Web: File upload flow (AC: #1, #2, #3)
  - [ ] Tạo `apps/web/src/components/features/upload/UploadButton.tsx`
  - [ ] File picker: accept `application/pdf,image/jpeg,image/png`
  - [ ] Client-side: validate size ≤20MB, type check trước khi upload
  - [ ] Flow: get pre-signed URL → PUT to S3 directly (axios upload với progress) → confirm
  - [ ] Processing screen với pulsing progress bar (UX-DR3)
- [ ] Task 5 — Mobile (Phase 2): File picker từ thư viện (AC: #1, #2, #3)
  - [ ] Dùng `expo-document-picker` cho PDF, `expo-image-picker` cho ảnh từ gallery
  - [ ] Tương tự web: validate → pre-signed URL → upload → confirm
  - [ ] Progress indicator native
- [ ] Task 6 — Tests (AC: #1, #2, #4, #5)
  - [ ] `HealthRecordServiceTest`: generate upload URL, confirm upload, job enqueue

## Dev Notes

### Database Schema

```sql
-- V010__create_health_records_table.sql
CREATE TABLE health_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    exam_date DATE,
    source_type VARCHAR(20) NOT NULL DEFAULT 'ocr',   -- 'ocr', 'manual', 'mixed'
    status VARCHAR(30) NOT NULL DEFAULT 'processing', -- 'processing', 'review_required', 'done', 'ocr_failed'
    file_key VARCHAR(500),
    raw_ocr_result JSONB,
    metrics JSONB NOT NULL DEFAULT '[]',
    ocr_confidence DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_health_records_profile_id ON health_records(profile_id);
CREATE INDEX idx_health_records_metrics ON health_records USING GIN(metrics);
```

### Upload Flow

```
1. Client → POST /api/v1/health-records/upload-url
   ← { uploadUrl, recordId, fileKey }

2. Client → PUT {uploadUrl} (binary file — trực tiếp lên S3/MinIO)
   ← 200 OK từ S3

3. Client → POST /api/v1/health-records/{recordId}/confirm-upload
   ← { recordId, status: "processing" }

4. Worker picks up job từ Redis Streams
5. Worker → OCR API → parse → update health_record
```

### S3 Path Convention

```
health-records/{userId}/{profileId}/{recordId}/original.pdf
health-records/{userId}/{profileId}/{recordId}/original.jpg
```

### Processing Screen (UX-DR3)

```
0-2s:  "Đang nhận dạng..."
2-5s:  "Đang đọc chỉ số..."
5-8s:  "Đang phân tích kết quả..."
8-10s: "Hoàn tất!"
```
Implement bằng polling `GET /api/v1/health-records/{recordId}/status` mỗi 1.5s.

### File Validation Constants

```typescript
// packages/shared/constants/index.ts
export const UPLOAD_MAX_SIZE_BYTES = 20 * 1024 * 1024; // 20MB
export const ALLOWED_FILE_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];
```

### References

- [Source: architecture.md#Chiến-Lược-Upload-File]
- [Source: architecture.md#Kiến-Trúc-Dữ-Liệu]
- [Source: ux-design-specification.md#UX-DR3]
- [Source: epics.md#Story-3.1]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
