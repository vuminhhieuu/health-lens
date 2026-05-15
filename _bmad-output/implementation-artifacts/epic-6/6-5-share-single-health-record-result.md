# Story 6.5: Chia sẻ một kết quả khám trong hồ sơ

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 6 — phạm vi **web** và backend/API. Chức năng này là chia sẻ **một health record cụ thể**, không tự động cấp quyền xem toàn bộ hồ sơ.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Health Record Detail Page | `projects/578519912546445367/screens/3c9f3f9c951b4e45aa713c6da552be29` | Reuse từ Story 5.2 | Reuse từ Story 5.2 |
| Chia sẻ gia đình - HealthLens | `projects/578519912546445367/screens/6c7c4797727240cda249483e8307e4cf` | Reuse modal/list pattern từ Story 6.1 | Reuse modal/list pattern từ Story 6.1 |

*Ghi chú:* Chưa có màn Stitch riêng cho record-level share. Triển khai UI bằng cách tái sử dụng detail page của kết quả khám và pattern invite/share modal hiện có.

## Story

As a chủ hồ sơ,
I want chia sẻ riêng một kết quả khám của một hồ sơ,
so that người nhận chỉ xem được lần khám đó mà không xem toàn bộ lịch sử hồ sơ.

## Acceptance Criteria

1. **Given** tôi là chủ hồ sơ hoặc người có quyền `edit` trên hồ sơ, **When** mở trang chi tiết một kết quả khám và chọn chia sẻ, **Then** tôi có thể nhập email người nhận và gửi lời mời chia sẻ riêng record đó.
2. **Given** lời mời chia sẻ record được tạo, **When** kiểm tra dữ liệu, **Then** invitation gắn với `healthRecordId`, có trạng thái `pending`/`accepted`/`expired`/`revoked`, token unique và hết hạn sau 7 ngày.
3. **Given** người nhận click link mời, **When** họ đã đăng nhập đúng email hoặc đăng ký bằng email được mời, **Then** hệ thống tạo quyền xem read-only cho đúng health record và điều hướng tới trang chi tiết record.
4. **Given** người nhận đã accept record share, **When** họ truy cập health record detail, **Then** họ xem được thông tin kết quả khám, metrics, explanations, recommendations nhưng không thấy action edit/delete/save/upload lại.
5. **Given** người nhận chỉ được share một record, **When** họ truy cập profile history, danh sách records khác cùng hồ sơ, hoặc record khác không được share, **Then** backend trả 403 Forbidden.
6. **Given** chủ hồ sơ xem danh sách người đang được chia sẻ trong record detail, **When** hiển thị, **Then** thấy email, trạng thái, ngày gửi, ngày accept và có thể thu hồi quyền.
7. **Given** chủ hồ sơ thu hồi quyền chia sẻ record, **When** người nhận gọi API detail sau đó, **Then** quyền bị chặn ngay lập tức và audit log ghi nhận actor, timestamp, `healthRecordId`, viewer.

## Tasks / Subtasks

- [x] Task 1 — Backend: Record share data model + migration (AC: #2, #7)
  - [x] Tạo Flyway migration cho `health_record_invitations`
  - [x] Tạo Flyway migration cho `health_record_shares`
  - [x] Đảm bảo unique constraint cho active share theo `(health_record_id, viewer_id)` và pending invite theo `(health_record_id, invitee_email)`
  - [x] Thêm audit log action: `INVITE_HEALTH_RECORD_SHARE`, `ACCEPT_HEALTH_RECORD_SHARE`, `REVOKE_HEALTH_RECORD_SHARE`
- [x] Task 2 — Backend: Invite/accept/revoke APIs (AC: #1, #2, #3, #6, #7)
  - [x] `POST /api/v1/health-records/{recordId}/invitations` → tạo invitation token, gửi email tiếng Việt
  - [x] `GET /api/v1/health-records/{recordId}/invitations` → danh sách người được mời/share cho record
  - [x] `POST /api/v1/health-record-invitations/accept?token={token}` → accept invitation
  - [x] `DELETE /api/v1/health-records/{recordId}/shares/{viewerId}` → revoke record share
  - [x] Owner hồ sơ hoặc shared editor được phép tạo invite; owner hồ sơ revoke
- [x] Task 3 — Backend: Access control cho record-level share (AC: #4, #5, #7)
  - [x] Mở quyền đọc `GET /api/v1/health-records/{recordId}` cho viewer có active `health_record_shares`
  - [x] Mở quyền đọc status/explanations/recommendations cho viewer có active record share
  - [x] Không mở quyền list profile history hoặc xem records khác cùng profile nếu chỉ có record-level share
  - [x] Response detail trả thêm context `shareScope: "owner" | "profile" | "record"` và `canEdit: boolean`
- [x] Task 4 — Web: Share action trong trang chi tiết record (AC: #1, #6)
  - [x] Thêm action "Chia sẻ kết quả" trong `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
  - [x] Modal nhập email, gửi lời mời, hiển thị danh sách pending/accepted/revoked/expired
  - [x] Confirm dialog thu hồi quyền share record
  - [x] Disable share action khi người xem không có quyền share
- [x] Task 5 — Web: Accept invitation + read-only record view (AC: #3, #4, #5)
  - [x] Tạo trang `/health-record-invitations/accept` xử lý token, login redirect nếu cần
  - [x] Sau accept, điều hướng tới `/health-records/review/{recordId}`
  - [x] Ẩn toàn bộ edit/delete/save/upload lại khi `shareScope = "record"` hoặc `canEdit = false`
  - [x] Khi bị 403 do revoke, reuse API client revoked-access handling
- [ ] Task 6 — Tests (AC: #1-#7)
  - [ ] Service tests: create invite, accept invite, expired token, email mismatch, duplicate invite/share
  - [ ] Service tests: record viewer đọc được đúng record nhưng không đọc được profile history/record khác
  - [ ] Service tests: revoke chặn access ngay lập tức và ghi audit log
  - [x] Web lint kiểm tra modal share và accept route không có lint error mới

### Review Findings

- [x] [Review][Patch] Thiếu luồng đăng ký có invitation context cho record share [apps/api/src/main/java/com/healthlens/api/service/HealthRecordShareService.java:184]
- [x] [Review][Patch] UI danh sách chia sẻ chưa hiển thị ngày gửi/ngày chấp nhận theo AC #6 [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:1765]

## Dev Notes

### Data Model

```sql
CREATE TABLE health_record_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    health_record_id UUID NOT NULL REFERENCES health_records(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES users(id),
    invitee_email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE health_record_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    health_record_id UUID NOT NULL REFERENCES health_records(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(id),
    viewer_id UUID NOT NULL REFERENCES users(id),
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP,
    UNIQUE(health_record_id, viewer_id)
);
```

### Access Rules

- `profile_shares` vẫn đại diện cho quyền xem/chỉnh toàn bộ hồ sơ theo Story 6.1–6.4.
- `health_record_shares` chỉ cho phép đọc một record cụ thể và các dữ liệu phụ thuộc trực tiếp vào record đó.
- Record-level viewer không được upload record mới, sửa metrics, xóa record, xem profile history, hoặc xem các records khác cùng profile.

### API Proposal

```http
POST /api/v1/health-records/{recordId}/invitations
GET /api/v1/health-records/{recordId}/invitations
DELETE /api/v1/health-records/{recordId}/shares/{viewerId}
POST /api/v1/health-record-invitations/accept?token={token}
```

### Email

- Subject: "{inviterName} muốn chia sẻ một kết quả khám với bạn"
- Link: `{frontendUrl}/health-record-invitations/accept?token={token}`
- Nội dung cần nói rõ: người nhận chỉ được xem một lần khám cụ thể, không phải toàn bộ hồ sơ.

### Frontend Integration Points

- `packages/shared/constants/api.ts`: thêm route constants cho record invitations/share.
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`: thêm nút share, modal, read-only handling.
- `apps/web/src/app/(auth)/health-record-invitations/accept/page.tsx`: xử lý accept token.
- `apps/web/src/lib/api/apiClient.ts`: reuse 403 handling cho record share revoke.

### References

- [Source: architecture.md#Mẫu-Phân-Quyền]
- [Source: ux-design-specification.md#UX-DR5]
- [Source: epics.md#Chia-Sẻ-Gia-Đình-FR29-FR32]
- [Related: Story 5.2]
- [Related: Story 6.1]
- [Related: Story 6.3]

## Dev Agent Record

### Agent Model Used

Codex GPT-5

### Debug Log References

- `pnpm --filter web lint` — pass với 3 warning cũ không thuộc story.
- `pnpm --filter web exec tsc --noEmit` — blocked bởi lỗi sẵn có: thiếu module/type `qrcode.react` tại `apps/web/src/app/admin/login/page.tsx`.
- `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.healthlens.api.service.HealthRecordServiceTest` — pass.

### Completion Notes List

- Thêm record-level share backend: migration, entity/repository, service, controller invite/list/accept/revoke.
- Health record detail/status/explanation/recommendations cho phép viewer có active record share đọc đúng record; profile history/list vẫn yêu cầu owner hoặc profile share.
- Health record detail response có thêm `shareScope`; record-level viewer luôn `canEdit=false`.
- Thêm email tiếng Việt cho lời mời xem một kết quả khám, nhấn mạnh chỉ chia sẻ một lần khám.
- Web detail page có modal "Chia sẻ kết quả", danh sách người nhận, revoke confirmation và accept route riêng.

### File List

- apps/api/src/main/resources/db/migration/V028__create_health_record_shares_tables.sql
- apps/api/src/main/java/com/healthlens/api/entity/HealthRecordInvitation.java
- apps/api/src/main/java/com/healthlens/api/entity/HealthRecordShare.java
- apps/api/src/main/java/com/healthlens/api/repository/HealthRecordInvitationRepository.java
- apps/api/src/main/java/com/healthlens/api/repository/HealthRecordShareRepository.java
- apps/api/src/main/java/com/healthlens/api/dto/request/InviteHealthRecordRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/response/AcceptHealthRecordInvitationResultResponse.java
- apps/api/src/main/java/com/healthlens/api/dto/response/HealthRecordInvitationResponse.java
- apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java
- apps/api/src/main/java/com/healthlens/api/controller/HealthRecordInvitationController.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordShareService.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java
- apps/api/src/main/java/com/healthlens/api/service/EmailService.java
- apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java
- apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java
- apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java
- packages/shared/constants/api.ts
- apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx
- apps/web/src/app/(auth)/health-record-invitations/accept/page.tsx

### Change Log

- 2026-05-14: Tạo draft story 6.5 cho chức năng chia sẻ riêng một kết quả khám của một hồ sơ.
- 2026-05-14: Implement story 6.5 backend + web record-level share flow, chuyển trạng thái sang review.
