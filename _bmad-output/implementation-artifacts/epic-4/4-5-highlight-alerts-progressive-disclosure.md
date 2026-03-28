# Story 4.5: Cảnh báo nổi bật và progressive disclosure

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
I want thấy cảnh báo ưu tiên cho chỉ số bất thường,
so that tôi không bỏ sót rủi ro quan trọng.

## Acceptance Criteria

1. **Given** bản ghi có metric bất thường, **When** màn hình kết quả được tải, **Then** key metrics bất thường hiển thị expanded mặc định với cảnh báo nổi bật.
2. **Given** màn hình kết quả, **When** tải, **Then** cấu trúc: Header (tên xét nghiệm + ngày + profile) → Summary (overall status + 1 dòng) → Key Metrics (top 3-5 abnormal, expanded) → All Results (collapsed) → Recommendations (bottom).
3. **Given** "All Results" section, **When** user tap "Xem tất cả", **Then** expand danh sách đầy đủ.
4. **Given** kết quả không có abnormal, **When** tải, **Then** header status = "Bình thường" và summary tích cực.
5. **Given** upload thành công, **When** user lần đầu xem kết quả, **Then** hiển thị celebration: subtle confetti + toast "Đã lưu!".

## Tasks / Subtasks

- [ ] Task 1 — Backend: Enriched result summary (AC: #1, #2, #4)
  - [ ] Cập nhật `GET /api/v1/health-records/{recordId}` trả về:
    - `overallStatus`: worst status trong tất cả metrics
    - `keyMetrics`: top 3-5 abnormal metrics (sort by severity)
    - `allMetrics`: đầy đủ tất cả
    - `summary`: 1-line summary text (template-based, không dùng LLM)
- [ ] Task 2 — Web: Result detail page layout (AC: #2, #3, #4, #5)
  - [ ] Tạo `apps/web/src/app/(dashboard)/records/[recordId]/page.tsx`
  - [ ] Implement cấu trúc UX-DR5:
    - `<RecordHeader>`: test type, date, profile name
    - `<OverallStatusSummary>`: badge + 1-line text
    - `<KeyMetricsSection>`: top abnormal, mặc định expanded
    - `<AllResultsSection>`: accordion, mặc định collapsed
    - `<RecommendationsSection>`: (Story 4.4)
  - [ ] "Đã lưu!" toast sau upload success (Zustand flag `justUploaded`)
  - [ ] Confetti: dùng `canvas-confetti` library (npm), chỉ trigger một lần
- [ ] Task 3 — Mobile (Phase 2): Result detail screen (AC: #2, #3, #4)
  - [ ] `apps/mobile/app/records/[recordId].tsx`
  - [ ] ScrollView với các sections tương tự
  - [ ] Collapse/expand với Animated API
- [ ] Task 4 — Tests (AC: #1, #2)
  - [ ] Backend: overallStatus calculation, keyMetrics sorting

## Dev Notes

### Overall Status Logic

```java
// Worst status wins
String overallStatus = metrics.stream()
    .map(m -> m.status)
    .reduce("normal", (a, b) -> {
        if ("abnormal".equals(a) || "abnormal".equals(b)) return "abnormal";
        if ("attention".equals(a) || "attention".equals(b)) return "attention";
        return "normal";
    });
```

### Summary Templates (tiếng Việt, template-based)

```typescript
const SUMMARY_TEMPLATES = {
  abnormal: 'Có {count} chỉ số cần chú ý. Hãy tham khảo ý kiến bác sĩ sớm.',
  attention: 'Có {count} chỉ số ở ngưỡng cần theo dõi thêm.',
  normal: 'Tất cả chỉ số trong giới hạn bình thường. Tiếp tục duy trì!',
};
```

### Confetti Implementation (Web)

```typescript
import confetti from 'canvas-confetti';
// Trigger once when component mounts with justUploaded flag
useEffect(() => {
  if (justUploaded) {
    confetti({ particleCount: 80, spread: 60, origin: { y: 0.6 } });
    clearJustUploaded();
  }
}, [justUploaded]);
```

### UX-DR5 Layout Structure

```
┌─── Header (sticky) ────────────────┐
│ Test Type | Date | Profile Name     │
├─── Overall Summary ────────────────┤
│ 🔴 Bất thường | Có 2 chỉ số...    │
├─── Key Metrics (expanded) ─────────┤
│ ❌ HbA1c: 8.2% [Bất thường]       │
│ ⚠️ Glucose: 6.8 [Cần chú ý]       │
├─── All Results (collapsed) ─────────┤
│ [Xem tất cả 10 chỉ số ▼]          │
├─── Recommendations ────────────────┤
│ 💚 Giảm tinh bột...               │
│ ⚕️ Disclaimer text                  │
└────────────────────────────────────┘
```

### References

- [Source: ux-design-specification.md#UX-DR5]
- [Source: ux-design-specification.md#UX-DR10]
- [Source: epics.md#Story-4.5]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
