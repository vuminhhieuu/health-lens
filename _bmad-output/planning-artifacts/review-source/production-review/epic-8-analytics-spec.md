# Epic 8 Analytics Spec

Mục tiêu: đủ để viết story và triển khai admin analytics production.

## 8.1 User growth overview

**Status:** Implemented (Story `8-1-dashboard-user-growth-overview.md`, review).

- Metric: `totalUsers = count(users WHERE role = ROLE_USER AND account_status <> DELETED)` — all-time, không lọc theo date picker.
- Trend: `count(users.created_at)` theo tháng, `role = ROLE_USER`, trong `[from, toExclusive)` UTC; tháng trống → 0.
- Default range: 6 tháng lịch gần nhất (UTC).
- API: `GET /api/v1/admin/analytics/users?from=&to=`; cache Redis 1h (không invalidate on register).
- UI: section `UserGrowthPanel` trên `/admin` (không phải trang riêng); `/admin/analytics` redirect.
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
- WAU (8.2) vẫn thiếu activity event table — xem story 8.2.

## Dashboard implications

- Route `/admin` = trang **Thống kê** (ghép 8.1 + 8.2 + 8.3).
- `/admin/analytics` → redirect `/admin` (bookmark).
- `8.1` **done (review):** stat card + monthly growth line chart + date range (`UserGrowthPanel`).
- `8.2`: WAU line chart + upload volume bar chart + day/week toggle.
- `8.3`: success/failure stacked chart + threshold + drill-down modal.
- Pending states nên tách riêng hoặc loại khỏi rate charts.
