# Story 9.1: Chụp ảnh kết quả khám trên mobile với camera UX

Status: backlog

## Execution scope

**Phase 2 — Mobile:** Chỉ triển khai **sau** khi Web MVP (Epic 1–8) hoàn thành. Phụ thuộc API và luồng web/upload ổn định từ Phase 1.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

**Lưu ý:** Epic 9 (mobile) chưa có screen trong project Stitch Web MVP; dùng cùng luồng auth/upload ở web như tham chiếu UX hoặc bổ sung project Stitch riêng sau.

Không có screen Stitch tương ứng trong project Web MVP này (story kỹ thuật / mobile phase 2).

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng mobile,
I want chụp giấy khám với guide frame,
so that ảnh đủ rõ cho OCR.

## Acceptance Criteria

1. **Given** người dùng mở camera capture, **When** căn giấy vào khung và chụp ảnh, **Then** hệ thống hiển thị preview với lựa chọn "Dùng ảnh này" hoặc "Chụp lại".
2. **Given** camera screen đang mở, **When** giấy được căn chỉnh vào guide frame, **Then** viền guide frame chuyển xanh (edge detection highlight).
3. **Given** preview ảnh, **When** user chọn "Dùng ảnh này", **Then** ảnh được upload theo flow Story 3.1.
4. **Given** preview ảnh, **When** user chọn "Chụp lại", **Then** quay lại camera screen.
5. **Given** camera không có quyền truy cập, **When** mở camera screen, **Then** hiển thị hướng dẫn cấp quyền tiếng Việt.

## Tasks / Subtasks

- [ ] Task 1 — Mobile (Phase 2): Camera screen (AC: #1, #2, #5)
  - [ ] Tạo `apps/mobile/app/upload/camera.tsx`
  - [ ] Dùng `expo-camera` với `CameraView` component
  - [ ] Overlay guide frame: `StyleSheet` với border outline ở giữa màn hình
  - [ ] Edge detection: dùng `onBarcodeScanned` hoặc custom overlay để simulate green highlight khi căn chỉnh
  - [ ] Button "Chụp" ở dưới, nút tắt camera ở góc trên trái
  - [ ] Permission check: `Camera.requestCameraPermissionsAsync()`, hiển thị hướng dẫn nếu bị từ chối
- [ ] Task 2 — Mobile (Phase 2): Preview screen (AC: #1, #3, #4)
  - [ ] Tạo `apps/mobile/app/upload/preview.tsx`
  - [ ] Hiển thị ảnh vừa chụp full-screen
  - [ ] Nút "Dùng ảnh này" → trigger upload flow (Story 3.1)
  - [ ] Nút "Chụp lại" → back to camera screen
- [ ] Task 3 — Mobile (Phase 2): Navigation flow (AC: #1)
  - [ ] Expo Router navigation: `upload/` → `upload/camera` → `upload/preview` → `upload/processing`
  - [ ] Pass photo URI qua Expo Router params hoặc Zustand store

## Dev Notes

### expo-camera API (SDK 55)

```typescript
import { CameraView, useCameraPermissions } from 'expo-camera';

// Permission check
const [permission, requestPermission] = useCameraPermissions();

// Take photo
const cameraRef = useRef<CameraView>(null);
const photo = await cameraRef.current?.takePictureAsync({
  quality: 0.85,
  base64: false,
});
// photo.uri → dùng làm source cho Image và upload
```

### Guide Frame Overlay

```typescript
const styles = StyleSheet.create({
  guideFrame: {
    position: 'absolute',
    width: '85%',
    height: '65%',
    borderWidth: 2,
    borderColor: '#FFFFFF',  // trắng mặc định
    borderRadius: 8,
    // alignSelf: 'center', marginTop: '10%'
  },
  guideFrameAligned: {
    borderColor: '#10B981',  // xanh khi aligned (health status color)
  }
});
```

### Upload from Camera

Sau khi user confirm preview:
1. Convert `photo.uri` thành blob/file object
2. Gọi flow tương tự Story 3.1: pre-signed URL → PUT → confirm

```typescript
// Create FormData or Blob from URI
const response = await fetch(photo.uri);
const blob = await response.blob();
// Then PUT blob to pre-signed URL
```

### UX-DR3 Processing Animation

- Sau khi upload: navigate đến `upload/processing` screen
- Polling `GET /api/v1/health-records/{recordId}/status` mỗi 1.5s
- Text updates: 0-2s, 2-5s, 5-8s, 8-10s (xem Story 3.1 Dev Notes)

### References

- [Source: ux-design-specification.md#UX-DR3]
- [Source: architecture.md#Tích-Hợp-Dịch-Vụ-Bên-Ngoài]
- [Source: epics.md#Story-3.2]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
