# Story 4.4: Gợi ý lối sống kèm disclaimer bắt buộc

Status: done 

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch)          | Resource                                                               | HTML prototype                                                                                                                                                                                                                                                              | Screenshot                                                                                                                                                                                                                                                                                                   |
| ------------------------- | ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Health Record Detail Page | `projects/578519912546445367/screens/3c9f3f9c951b4e45aa713c6da552be29` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzc2ZDgwMGI2MWVjZjRjZjBhNjZlYmFhMGM1MThjMTQyEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhzldktcjBvfyA80T-sCTf9VYf7EEA0b8kZJqSNh2gb2l2nyuCMywxSBNQM_QjkdzH-eQvQMehBqSForVsETegaE_zrHY-5HTDbo7tHtOdGCCrnJPfo84-vpSeeO6ecq8xYdwkq5DqHc-zEg0abPd7o_U2M9-LTR7Coxv_9sKGYfag9-O6z0u1vdbo67DJ8abgHQa-7OSDXTdrfLdy-vcamk0AbOBtN-HT_uxCGuCfTJsUkiJ8HqlJRSQ) |

_Ghi chú:_ Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want nhận khuyến nghị dinh dưỡng/lối sống có trách nhiệm,
so that tôi biết hành động tiếp theo an toàn.

## Acceptance Criteria

1. **Given** kết quả có chỉ số cần chú ý hoặc bất thường, **When** hiển thị phần recommendations, **Then** hệ thống đưa 2-3 gợi ý khả thi theo ngữ cảnh chỉ số.
2. **Given** phần recommendations, **When** hiển thị, **Then** luôn có disclaimer rõ ràng: "Thông tin này chỉ mang tính tham khảo và không thay thế tư vấn của bác sĩ chuyên khoa."
3. **Given** tất cả chỉ số bình thường, **When** hiển thị recommendations, **Then** thông điệp khích lệ tích cực (không cần gợi ý hành động cụ thể).
4. **Given** gợi ý đã được cache (cùng metric combo), **When** load lại, **Then** không gọi LLM lại — dùng cache.

## Tasks / Subtasks

- [x] Task 1 — Backend: Lifestyle recommendations service (AC: #1, #2, #3)
  - [x] Cập nhật `LlmService` thêm method `generateRecommendations(metrics[], profileAge, gender)`
  - [x] Chỉ generate cho metrics có status `attention` hoặc `abnormal`
  - [x] Cache key: `llm:recommendations:{hash(abnormalMetricNames+statuses+ageGroup+gender)}`
  - [x] Kết quả: list 2-3 recommendations (plain text tiếng Việt)
- [x] Task 2 — Backend: Recommendations endpoint (AC: #1, #4)
  - [x] `GET /api/v1/health-records/{recordId}/recommendations`
  - [x] Trả về: `{ recommendations: [...], disclaimer: "...", allNormal: boolean }`
- [x] Task 3 — Prompt Template (AC: #1, #2)
  - [x] Prompt yêu cầu: 2-3 gợi ý lifestyle cụ thể, actionable, tiếng Việt đơn giản
  - [x] Không được đề xuất thuốc hoặc thủ thuật y tế
  - [x] Output phải bắt đầu với lời khuyên liên quan trực tiếp đến chỉ số
- [x] Task 4 — Web (Phase 1) / Mobile (Phase 2): Recommendations section (AC: #1, #2, #3)
  - [x] Cuối màn hình result detail (UX-DR5 — sau All Results section)
  - [x] Recommendations section: 2-3 bullet points với icon 💚
  - [x] Disclaimer text nhỏ hơn, màu xám nhẹ (không nổi bật quá)
  - [x] Nếu allNormal: "✅ Kết quả của bạn nhìn chung tốt. Tiếp tục duy trì lối sống lành mạnh!"
- [x] Task 1 — Backend: Lifestyle recommendations service (AC: #1, #2, #3)
  - [x] Cập nhật `LlmService` thêm method `generateRecommendations(metrics[], profileAge, gender)`
  - [x] Chỉ generate cho metrics có status `attention` hoặc `abnormal`
  - [x] Cache key: `llm:recommendations:{hash(abnormalMetricNames+statuses+ageGroup+gender)}`
  - [x] Kết quả: list 2-3 recommendations (plain text tiếng Việt)
- [x] Task 2 — Backend: Recommendations endpoint (AC: #1, #4)
  - [x] `GET /api/v1/health-records/{recordId}/recommendations`
  - [x] Trả về: `{ recommendations: [...], disclaimer: "...", allNormal: boolean }`
- [x] Task 3 — Prompt Template (AC: #1, #2)
  - [x] Prompt yêu cầu: 2-3 gợi ý lifestyle cụ thể, actionable, tiếng Việt đơn giản
  - [x] Không được đề xuất thuốc hoặc thủ thuật y tế
  - [x] Output phải bắt đầu với lời khuyên liên quan trực tiếp đến chỉ số
- [x] Task 4 — Web (Phase 1) / Mobile (Phase 2): Recommendations section (AC: #1, #2, #3)
  - [x] Cuối màn hình result detail (UX-DR5 — sau All Results section)
  - [x] Recommendations section: 2-3 bullet points với icon 💚
  - [x] Disclaimer text nhỏ hơn, màu xám nhẹ (không nổi bật quá)
  - [x] Nếu allNormal: "✅ Kết quả của bạn nhìn chung tốt. Tiếp tục duy trì lối sống lành mạnh!"

## Dev Notes

### Disclaimer Text (hardcode, không dùng LLM)

```
⚕️ Lưu ý: Thông tin này chỉ mang tính tham khảo và không thay thế tư vấn của bác sĩ chuyên khoa.
Nếu bạn có bất kỳ lo ngại nào về sức khỏe, hãy tham khảo ý kiến bác sĩ.
```

### Recommendations Prompt

```
Based on these health metrics (in Vietnamese context):
{abnormalMetrics: [{name, value, unit, status}]}
Patient profile: {ageGroup}, {gender}

Provide 2-3 specific, actionable lifestyle recommendations in simple Vietnamese.
Rules:
- Do NOT recommend medications or medical procedures
- Keep each recommendation to 1-2 sentences
- Be encouraging but factual
- Focus on diet, exercise, and lifestyle habits
Output as JSON array: ["recommendation1", "recommendation2", ...]
```

### References

- [Source: epics.md#Story-4.4]
- [Source: architecture.md#Chiến-Lược-Cache]

## Dev Agent Record

### Agent Model Used

Codex 5.3

### Debug Log References

- `./gradlew test --tests "com.healthlens.api.service.HealthRecordServiceTest" --tests "com.healthlens.api.service.LlmServiceTest"` (PASS)

- `./gradlew test --tests "com.healthlens.api.service.HealthRecordServiceTest" --tests "com.healthlens.api.service.LlmServiceTest"` (PASS)

### Completion Notes List

- Implemented `LlmService.generateRecommendations(...)` with filtering for `attention`/`abnormal`, prompt constraints, JSON parsing, fallback, and Redis caching.
- Added backend endpoint `GET /api/v1/health-records/{recordId}/recommendations` returning recommendations, hardcoded disclaimer, and `allNormal`.
- Added all-normal encouragement message path to satisfy AC #3 without unnecessary LLM calls.
- Added recommendations section on review detail page with 💚 bullets and subtle disclaimer styling.
- Added tests for recommendations behavior in both `HealthRecordServiceTest` and `LlmServiceTest`.

- Refined recommendation generation to avoid generic advice when metrics are abnormal: added output quality gate to reject vague LLM responses and fall back to metric-specific guidance.
- Expanded fallback recommendations with metric-group-specific lifestyle advice (glucose, lipid, hematology, immune/inflammation, liver) so guidance reflects actual risk context.
- Upgraded recommendation prompt constraints (v6) to enforce explicit metric mentions, severity ordering, and exam-context-aware personalization.
- Verified with targeted service tests and full `apps/api` regression suite (`./gradlew test`) — all passed.

- Implemented `LlmService.generateRecommendations(...)` with filtering for `attention`/`abnormal`, prompt constraints, JSON parsing, fallback, and Redis caching.
- Added backend endpoint `GET /api/v1/health-records/{recordId}/recommendations` returning recommendations, hardcoded disclaimer, and `allNormal`.
- Added all-normal encouragement message path to satisfy AC #3 without unnecessary LLM calls.
- Added recommendations section on review detail page with 💚 bullets and subtle disclaimer styling.
- Added tests for recommendations behavior in both `HealthRecordServiceTest` and `LlmServiceTest`.

### File List

- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/RecommendationsResponse.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `packages/shared/constants/api.ts`

### Change Log

- 2026-04-27: Hoàn thành Story 4.4 và cập nhật trạng thái sang `review`.
- 2026-04-29: Tăng mức cá nhân hóa khuyến nghị cho chỉ số bất thường, thêm quality gate chống lời khuyên chung chung, và xác nhận lại toàn bộ test backend.

- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/RecommendationsResponse.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `packages/shared/constants/api.ts`

### Change Log

- 2026-04-27: Hoàn thành Story 4.4 và cập nhật trạng thái sang `review`.
