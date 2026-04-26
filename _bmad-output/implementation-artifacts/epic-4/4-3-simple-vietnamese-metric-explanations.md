# Story 4.3: Giải thích tiếng Việt đơn giản cho từng chỉ số

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)


| Tiêu đề (Stitch)          | Resource                                                               | HTML prototype                                                                                                                                                                                                                                                              | Screenshot                                                                                                                                                                                                                                                                                                   |
| ------------------------- | ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
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

- Task 1 — Backend: LLM explanation service (AC: #1, #2, #3)
  - Tạo `LlmService.java` với `generateExplanation(metric, value, status, referenceRange, lang="vi")`
  - Cache key: `llm:explanation:{hash(metricName+value+status+rangeMin+rangeMax)}` trong Redis
  - Cache TTL: 7 ngày (explanation không thay đổi theo thời gian thực)
  - Retry logic: 3 lần, exponential backoff (1s, 2s, 4s)
  - Fallback: hardcoded template explanation nếu LLM thất bại
- Task 2 — Backend: Explanation endpoint (AC: #1, #2)
  - `GET /api/v1/health-records/{recordId}/metrics/{metricName}/explanation`
  - Trả về explanation text kèm source: `llm` hoặc `fallback`
  - Thời gian phản hồi ≤5s (NFR-P3)
- Task 3 — LLM Prompt Template (AC: #1, #4)
  - Prompt tiếng Anh nhưng yêu cầu output tiếng Việt đơn giản
  - Bao gồm: tên chỉ số, giá trị, trạng thái, ngưỡng tham chiếu
  - Instruction: "Giải thích bằng tiếng Việt đơn giản, không dùng thuật ngữ y khoa phức tạp, tối đa 3 câu"
- Task 4 — Web (Phase 1) / Mobile (Phase 2): Explanation UI (AC: #1, #3)
  - Trong HealthMetricCard expanded state: skeleton loader khi đang fetch explanation
  - Hiển thị text explanation sau khi load
  - Nếu fallback: không hiển thị "LLM error" — chỉ hiển thị fallback text bình thường
- Task 5 — Tests (AC: #2, #3)
  - `LlmServiceTest`: cache hit, cache miss, retry, fallback

### Review Findings

- [Review][Decision] Retry/backoff 1s-2s-4s đang mâu thuẫn ràng buộc response <=5s — đã chốt hướng hybrid (retry có time budget cứng, hết budget fallback ngay) và áp dụng trong `LlmService`.
- [Review][Patch] `source` bị sai khi cache chứa fallback [apps/api/src/main/java/com/healthlens/api/service/LlmService.java:83]
- [Review][Patch] `HealthMetricCard` không xử lý trạng thái `isError`, dẫn đến silent failure khi API lỗi [apps/web/src/components/ui/HealthMetricCard.tsx:95]
- [Review][Patch] Fallback text chưa nhất quán AC#4 (vẫn có thuật ngữ khó với user không chuyên) [apps/api/src/main/java/com/healthlens/api/service/LlmService.java:47]
- [Review][Patch] Endpoint explanation thiếu test negative-path (record/metric not found) [apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java:233]
- [Review][Patch] Route metric name dạng path segment có rủi ro với ký tự `/` trong tên chỉ số [apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java:76]

## Dev Notes

### Cache Strategy (Updated 2026-03-29)

```
Key: llm:explanation:{SHA256(metricName + "|" + normalizedValue + "|" + status + "|" + rangeMin + "|" + rangeMax)}
Value: explanation text (plain string)
TTL: 604800 seconds (7 ngày)

Note: With local Ollama, cache hit rate is critical for performance.
      Consider caching at multiple levels: LLM response cache (Redis) + embedding cache (Qdrant).
```

### Prompt Template

```
You are a health assistant. Explain the following health metric result in simple Vietnamese (tiếng Việt đơn giản) that anyone can understand. Maximum 3 sentences. Do not use complex medical terms.

Metric: {metricName} ({displayNameVi})
Value: {value} {unit}
Status: {status} (normal/attention/abnormal)
Reference range: {min} - {max} {unit}
```

### Prompt v2 + Quality Framework (Consolidated in this story)

#### Prompt v2 (target production)

```text
You are a health-lab explanation assistant for non-medical users in Vietnam.
Explain one metric in plain Vietnamese.

Hard constraints:
- Output exactly 3 lines, in this order:
  1) Chỉ số này là gì: ...
  2) Chỉ số này liên quan đến: ...
  3) Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: ...
- Keep each line short and clear.
- No diagnosis, no treatment plan.
- No unexplained jargon.
```

#### Input contract for prompt

```text
Metric key: {metricKey}
Metric display name: {displayName}
Aliases: {aliases}
Value: {value} {unit}
Status: {status} (normal/attention/abnormal/no_data)
Reference range: {rangeMin} - {rangeMax} {unit}
Knowledge snippet: {knowledgeSnippet}
```

### RAG Retrieval Contract (BMAD-aligned, no extra file)

#### Retrieval objective

Lấy đúng "knowledgeSnippet" cho từng metric trước khi gọi LLM, ưu tiên dữ liệu domain nội bộ thay vì để model suy diễn tự do.

#### Retrieval inputs

- `metricName` (raw + normalized + alias)
- `status` (normal/attention/abnormal/no_data)
- `referenceRange` (min/max/unit)
- optional: `profile` context (age/gender) nếu có

#### Retrieval query strategy

1. Match exact theo metric canonical name trong reference data.
2. Nếu không có, match theo alias normalized.
3. Nếu vẫn không có, fallback generic snippet.

#### Retrieval output contract (`knowledgeSnippet`)

`knowledgeSnippet` phải là text ngắn, gồm tối đa 3 block:

1. `Metric identity`: chỉ số này đại diện cho thành phần/chức năng gì.
2. `Clinical relation`: thường liên quan nhóm vấn đề nào (mức high-level, không chẩn đoán).
3. `Out-of-range impact`: ảnh hưởng thường gặp nếu lệch ngưỡng.

#### Ranking / trust order

1. Internal reference data (ưu tiên cao nhất)
2. Curated medical copy reviewed by team
3. Generic fallback template (khi thiếu dữ liệu)

#### Safety constraints for retrieval usage

- Không sinh câu kết luận bệnh.
- Không đưa phác đồ điều trị.
- Không gợi ý hành động vượt phạm vi story 4.3.
- Luôn giữ output cuối cùng đúng format 3 ý.

#### Quality gates after generation

- Reject if output is not exactly 3 lines
- Reject if any line empty
- Reject if contains diagnosis/treatment wording
- Reject if all lines are generic without metric-specific clue
- Retry once with stricter instruction if failed

#### Rubric chấm điểm output (10 điểm)

- 0-3: Đúng cấu trúc 3 ý
- 0-3: Đúng ngữ nghĩa chỉ số
- 0-2: Hữu ích cho user (không chung chung)
- 0-1: Dễ hiểu, ít jargon
- 0-1: An toàn (không chẩn đoán/phác đồ)

Pass threshold: >= 8/10

#### Golden test set (QA prompt/model)

ALT, AST, HDL-C, LDL-C, Triglyceride, Glucose, HbA1c, EO%, BASO%, WBC, RBC, PLT + 1 chỉ số lạ (ví dụ Ferritin) để kiểm tra generic fallback.

#### Example output 10/10

```text
Chỉ số này là gì: ALT (GPT) là men gan, thường tăng khi tế bào gan bị kích thích hoặc tổn thương.
Chỉ số này liên quan đến: viêm gan, gan nhiễm mỡ, rượu bia và một số thuốc có thể ảnh hưởng gan.
Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: nếu tăng kéo dài, nguy cơ tổn thương gan tăng và nên kiểm tra thêm các chỉ số gan liên quan.
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

### LLM Provider Config (Option B+ - Updated 2026-04-01)

**Primary:** Groq API with qwen-2.5-72b-versatile — fast inference, excellent Vietnamese support
**Fallback:** OpenRouter / Claude API — when Groq unavailable

**Setup:** See Story 1.7 (`1-7-groq-api-setup.md`)

```yaml
# application.yml
spring:
  ai:
    groq:
      api-key: ${GROQ_API_KEY}
      chat:
        options:
          model: qwen-2.5-72b-versatile
          temperature: 0.7
          max-tokens: 500
        endpoint: https://api.groq.com/openai/v1

# Fallback Configuration
app:
  ai:
    primary: groq
    fallback:
      enabled: true
      provider: openrouter  # or: claude
    retry:
      max-attempts: 3
      initial-delay-ms: 1000
      multiplier: 2.0
```

### Groq Service Architecture

**Why Groq for HealthLens:**

- **Speed:** LPU inference chips - fastest available
- **Vietnamese:** qwen-2.5-72b excellent for Vietnamese text
- **Free Tier:** 14,400 requests/minute - sufficient for MVP
- **OpenAI Compatible:** Easy Spring AI integration

```java
// LlmService.java - Option B+ Architecture
@Service
@RequiredArgsConstructor
public class LlmService {
    private final ChatClient groqChatClient;
    
    public String generateExplanation(String metricName, String value, 
                                     String unit, String status) {
        String prompt = buildPrompt(metricName, value, unit, status);
        
        try {
            return groqChatClient.prompt()
                .user(prompt)
                .call()
                .content();
        } catch (Exception e) {
            log.warn("Groq API failed: {}, returning fallback", e.getMessage());
            return getFallbackExplanation(metricName);
        }
    }
}
```

**Groq Available Models:**


| Model                     | Context | Vietnamese  | Best For            |
| ------------------------- | ------- | ----------- | ------------------- |
| `qwen-2.5-72b-versatile`  | 128K    | ✅ Excellent | Health explanations |
| `llama-3.3-70b-versatile` | 128K    | ⚠️ Good     | Complex reasoning   |


### References

- [Source: architecture.md#Chiến-Lược-Cache]
- [Source: architecture.md#Tích-Hợp-Dịch-Vụ-Bên-Ngoài]
- [Source: architecture.md#ADR-003-Cloud-First-AI]
- [Source: 1-7-groq-api-setup.md]
- [Source: epics.md#Story-4.3]

## Dev Agent Record

### Agent Model Used

- Codex 5.3

### Debug Log References

- `cd apps/api && ./gradlew test --tests "com.healthlens.api.service.LlmServiceTest" --tests "com.healthlens.api.service.HealthRecordServiceTest"` (pass)
- `cd apps/web && pnpm exec eslint "src/components/ui/HealthMetricCard.tsx" "src/app/(dashboard)/health-records/review/[recordId]/page.tsx"` (failed: local ESLint config not detected in this shell context)
- `cd apps/api && ./gradlew test` (pass)

### Completion Notes List

- Hoàn tất refactor `LlmService`: signature theo story, cache Redis key SHA256 + TTL 7 ngày, retry 3 lần exponential backoff, fallback khi LLM thất bại.
- Bổ sung endpoint `GET /api/v1/health-records/{recordId}/metrics/{metricName}/explanation` trả về `explanation` + `source`.
- Cập nhật prompt template theo yêu cầu: prompt tiếng Anh, output tiếng Việt đơn giản, tối đa 3 câu, hạn chế thuật ngữ y khoa.
- Cập nhật `HealthMetricCard` để fetch explanation khi expanded, có skeleton loader và không hiển thị lỗi LLM cho fallback case.
- Mở rộng test coverage cho `LlmServiceTest` (cache hit/miss, retry, fallback, prompt) và `HealthRecordServiceTest` (metric explanation endpoint flow).
- Address toàn bộ review findings: hybrid retry budget (`max-total-delay-ms`), cache source fidelity (`source||explanation`), UI fallback khi API explanation lỗi, fallback text dễ hiểu hơn, thêm endpoint query-param để tránh edge case metricName chứa `/`, và bổ sung negative tests cho explanation service.

### File List

- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/MetricExplanationResponse.java`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `apps/web/src/components/ui/HealthMetricCard.tsx`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `packages/shared/constants/api.ts`

