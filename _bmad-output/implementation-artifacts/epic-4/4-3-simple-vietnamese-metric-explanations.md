# Story 4.3: Giải thích tiếng Việt đơn giản cho từng chỉ số

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

As a người dùng không chuyên y khoa,
I want đọc giải thích dễ hiểu cho từng chỉ số,
so that tôi hiểu ý nghĩa sức khỏe của kết quả.

## Acceptance Criteria

1. **Given** metric card có dữ liệu đầy đủ, **When** người dùng mở phần giải thích, **Then** hệ thống trả về diễn giải tiếng Việt Mức A (ý nghĩa, tầm quan trọng), rõ ràng và dễ đọc.
2. **Given** giải thích đã load, **When** load lại cùng metric+range combo, **Then** không gọi thêm LLM API (cache hit) — phải < 5 giây kể cả cache lookup.
3. **Given** LLM API thất bại sau 3 retries, **When** user xem giải thích, **Then** hiển thị giải thích mặc định đơn giản (fallback text) thay vì lỗi.
4. **Given** giải thích, **When** hiển thị, **Then** tuyệt đối không chứa từ ngữ y khoa phức tạp mà không giải thích.

## Tasks / Subtasks

- [ ] Task 1 — Backend: LLM explanation service (AC: #1, #2, #3)
  - [ ] Tạo `LlmService.java` với `generateExplanation(metric, value, status, referenceRange, lang="vi")`
  - [ ] Cache key: `llm:explanation:{hash(metricName+value+status+rangeMin+rangeMax)}` trong Redis
  - [ ] Cache TTL: 7 ngày (explanation không thay đổi theo thời gian thực)
  - [ ] Retry logic: 3 lần, exponential backoff (1s, 2s, 4s)
  - [ ] Fallback: hardcoded template explanation nếu LLM thất bại
- [ ] Task 2 — Backend: Explanation endpoint (AC: #1, #2)
  - [ ] `GET /api/v1/health-records/{recordId}/metrics/{metricName}/explanation`
  - [ ] Trả về explanation text kèm source: `llm` hoặc `fallback`
  - [ ] Thời gian phản hồi ≤5s (NFR-P3)
- [ ] Task 3 — LLM Prompt Template (AC: #1, #4)
  - [ ] Prompt tiếng Anh nhưng yêu cầu output tiếng Việt đơn giản
  - [ ] Bao gồm: tên chỉ số, giá trị, trạng thái, ngưỡng tham chiếu
  - [ ] Instruction: "Giải thích bằng tiếng Việt đơn giản, không dùng thuật ngữ y khoa phức tạp, tối đa 3 câu"
- [ ] Task 4 — Web (Phase 1) / Mobile (Phase 2): Explanation UI (AC: #1, #3)
  - [ ] Trong HealthMetricCard expanded state: skeleton loader khi đang fetch explanation
  - [ ] Hiển thị text explanation sau khi load
  - [ ] Nếu fallback: không hiển thị "LLM error" — chỉ hiển thị fallback text bình thường
- [ ] Task 5 — Tests (AC: #2, #3)
  - [ ] `LlmServiceTest`: cache hit, cache miss, retry, fallback

## Dev Notes

### Cache Strategy

```
Key: llm:explanation:{SHA256(metricName + "|" + normalizedValue + "|" + status + "|" + rangeMin + "|" + rangeMax)}
Value: explanation text (plain string)
TTL: 604800 seconds (7 ngày)
```

### Prompt Template

```
You are a health assistant. Explain the following health metric result in simple Vietnamese (tiếng Việt đơn giản) that anyone can understand. Maximum 3 sentences. Do not use complex medical terms.

Metric: {metricName} ({displayNameVi})
Value: {value} {unit}
Status: {status} (normal/attention/abnormal)
Reference range: {min} - {max} {unit}
```

### Fallback Explanations (packages/shared/constants)

```typescript
export const METRIC_FALLBACK_EXPLANATIONS: Record<string, string> = {
  'Glucose': 'Đây là chỉ số đường huyết của bạn. Hãy tham khảo bác sĩ để hiểu rõ hơn về kết quả.',
  'HbA1c': 'Chỉ số này phản ánh lượng đường huyết trung bình trong 3 tháng qua.',
  // ... các chỉ số phổ biến khác
  'default': 'Kết quả này cần được bác sĩ chuyên khoa giải thích thêm để đưa ra đánh giá chính xác.',
};
```

### LLM Provider Config

```yaml
# application.yml
llm:
  provider: openai  # hoặc anthropic
  api-key: ${LLM_API_KEY}
  model: gpt-4o-mini  # balance cost/quality
  max-tokens: 200
  timeout-seconds: 10
```

### References

- [Source: architecture.md#Chiến-Lược-Cache]
- [Source: architecture.md#Tích-Hợp-Dịch-Vụ-Bên-Ngoài]
- [Source: epics.md#Story-4.3]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
