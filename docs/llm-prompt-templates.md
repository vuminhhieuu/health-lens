# LLM Prompt Templates

**Last updated:** 2026-05-18

HealthLens lưu prompt LLM dạng resource có version để reviewer có thể đọc, test và rollback mà không cần chỉnh chuỗi Java dài:

- `apps/api/src/main/resources/ai/prompts/metric-explanation.v3.txt`
- `apps/api/src/main/resources/ai/prompts/recommendations.v8-medical-disclaimer-vi.txt`

## Quy Trình Cập Nhật

1. Tạo template file mới khi thay đổi hành vi đáng kể, thay vì sửa trực tiếp version đã deploy.
2. Cập nhật version/config tương ứng:
   - Metric explanation: `app.ai.explanation.prompt-version`
   - Recommendation: `app.ai.recommendations.prompt-version`
   - Nếu đổi file template qua `app.ai.explanation.prompt-template` hoặc `app.ai.recommendations.prompt-template`, tăng version tương ứng để audit và cache phản ánh đúng prompt đang dùng.
3. Giữ các guardrail y tế bắt buộc trong template:
   - Disclaimer y tế chuẩn của HealthLens cho recommendation prompt.
   - Không viết như chẩn đoán hoặc phác đồ điều trị.
   - Không tự bịa, suy diễn hoặc thay đổi reference range.
4. Chạy `cd apps/api && ./gradlew test --tests com.healthlens.api.service.LlmServiceTest`.
5. Review nội dung prompt render trong test trước khi rollout. Metadata `promptVersion` và `modelVersion` được trả về cùng AI explanation để phục vụ audit.

## Rollback

Để rollback prompt behavior, trỏ version/config về resource cũ rồi redeploy. Cache key đã bao gồm prompt version, nên output của version rollback được tách tự nhiên khỏi output của prompt mới hơn.
