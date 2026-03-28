# Story 4.4: Gợi ý lối sống kèm disclaimer bắt buộc

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
I want nhận khuyến nghị dinh dưỡng/lối sống có trách nhiệm,
so that tôi biết hành động tiếp theo an toàn.

## Acceptance Criteria

1. **Given** kết quả có chỉ số cần chú ý hoặc bất thường, **When** hiển thị phần recommendations, **Then** hệ thống đưa 2-3 gợi ý khả thi theo ngữ cảnh chỉ số.
2. **Given** phần recommendations, **When** hiển thị, **Then** luôn có disclaimer rõ ràng: "Thông tin này chỉ mang tính tham khảo và không thay thế tư vấn của bác sĩ chuyên khoa."
3. **Given** tất cả chỉ số bình thường, **When** hiển thị recommendations, **Then** thông điệp khích lệ tích cực (không cần gợi ý hành động cụ thể).
4. **Given** gợi ý đã được cache (cùng metric combo), **When** load lại, **Then** không gọi LLM lại — dùng cache.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Lifestyle recommendations service (AC: #1, #2, #3)
  - [ ] Cập nhật `LlmService` thêm method `generateRecommendations(metrics[], profileAge, gender)`
  - [ ] Chỉ generate cho metrics có status `attention` hoặc `abnormal`
  - [ ] Cache key: `llm:recommendations:{hash(abnormalMetricNames+statuses+ageGroup+gender)}`
  - [ ] Kết quả: list 2-3 recommendations (plain text tiếng Việt)
- [ ] Task 2 — Backend: Recommendations endpoint (AC: #1, #4)
  - [ ] `GET /api/v1/health-records/{recordId}/recommendations`
  - [ ] Trả về: `{ recommendations: [...], disclaimer: "...", allNormal: boolean }`
- [ ] Task 3 — Prompt Template (AC: #1, #2)
  - [ ] Prompt yêu cầu: 2-3 gợi ý lifestyle cụ thể, actionable, tiếng Việt đơn giản
  - [ ] Không được đề xuất thuốc hoặc thủ thuật y tế
  - [ ] Output phải bắt đầu với lời khuyên liên quan trực tiếp đến chỉ số
- [ ] Task 4 — Web (Phase 1) / Mobile (Phase 2): Recommendations section (AC: #1, #2, #3)
  - [ ] Cuối màn hình result detail (UX-DR5 — sau All Results section)
  - [ ] Recommendations section: 2-3 bullet points với icon 💚
  - [ ] Disclaimer text nhỏ hơn, màu xám nhẹ (không nổi bật quá)
  - [ ] Nếu allNormal: "✅ Kết quả của bạn nhìn chung tốt. Tiếp tục duy trì lối sống lành mạnh!"

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

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
