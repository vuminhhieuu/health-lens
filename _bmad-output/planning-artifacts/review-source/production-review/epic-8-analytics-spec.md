# Epic 8 Analytics Spec

Mục tiêu: đủ để viết story và triển khai admin analytics production.

## 8.1 User growth overview

- Metric: `totalUsers = count(users)`.
- Trend: `count(users.created_at)` theo tháng.
- Default range: 6 tháng gần nhất.
- Nếu cần chỉ tính user sản phẩm thì lọc `role = ROLE_USER`.
- Data source: bảng `users`.

## 8.2 WAU + upload volume

- WAU: distinct `user_id` có ít nhất một authenticated API call trong tuần.
- Upload volume: số `UPLOAD_CONFIRMED` events theo ngày hoặc theo tuần.
- Có toggle day/week và compare-to-previous-period nếu muốn khớp AC.
- Data source: nên có `user_activity_events`; không nên suy từ `health_records` vì sẽ undercount retry.

## 8.3 Upload success/failure rate

- Success = `status = done`.
- Failure = `status = ocr_failed`.
- Denominator chỉ tính terminal jobs trong kỳ, không tính `processing` hay `review_required`.
- Drill-down theo `failure_reason`.
- Data source: `health_records.status` + `created_at`, tốt hơn nếu thêm `failure_reason` column hoặc event log.

## Required events

- `AUTHENTICATED_API_CALL`: `user_id`, `request_id`, `route`, `method`, `role`, `created_at`.
- `UPLOAD_CONFIRMED`: `user_id`, `profile_id`, `record_id`, `file_type`, `is_retry`, `created_at`.
- `OCR_COMPLETED`: `record_id`, `provider`, `confidence`, `has_low_confidence_metrics`, `created_at`.
- `OCR_FAILED`: `record_id`, `reason`, `provider_if_known`, `created_at`.

## Missing instrumentation

- Chưa có persisted activity event table để tính WAU chuẩn.
- Auth/filter code hiện chỉ log runtime, chưa ghi event để query analytics.
- Failure reasons còn hẹp; nếu AC cần thêm `api_error` hoặc `invalid_file`, spec phải mở rộng.
- Admin dashboard hiện vẫn là placeholder.

## Dashboard implications

- Tạo `/admin/analytics`.
- `8.1`: stat card + monthly growth line chart.
- `8.2`: WAU line chart + upload volume bar chart + day/week toggle.
- `8.3`: success/failure stacked chart + threshold + drill-down modal.
- Pending states nên tách riêng hoặc loại khỏi rate charts.
