# Story R1.5: Analyze Flyway Migration Squash Groups

Status: done

## Story

Là developer,
tôi muốn inventory và phân nhóm Flyway migrations trước khi squash,
để giảm rủi ro DB lifecycle trước khi thay active migrations.

## Acceptance Criteria

1. **Given** API có Flyway migrations hiện tại **When** analysis script chạy **Then** nó liệt kê mọi migration theo version order.
2. **Given** migrations thuộc nhiều domain **When** grouping hoàn tất **Then** output đề xuất nhiều baseline logic, không phải một file duy nhất.
3. **Given** story này là analysis-only **When** review diff **Then** không file migration active nào bị sửa/xóa.
4. **Given** rollout DB có rủi ro **When** report được tạo **Then** nó nêu điều kiện test fresh DB và existing migrated DB.

## Tasks / Subtasks

- [x] Tạo script phân tích dưới `scripts/db/`. (AC: 1,2)
- [x] Classify migrations theo domain: identity/auth, profile/privacy, health-record/OCR, reference/AI/RAG, audit/activity/notification. (AC: 2)
- [x] Thêm markdown output option. (AC: 1,2)
- [x] Ghi rollout warnings. (AC: 3,4)

### Review Findings

- [x] [Review][Patch] Classifier overweights generic substring signals and can misclassify profile/privacy migrations [scripts/db/analyze-flyway-squash-groups.mjs:123]
- [x] [Review][Patch] New analyzer tests are not wired into the repository test command [package.json:9]

## Dev Notes

- Không dùng script này để sinh baseline thật trong cùng story.
- Không bật `baseline-on-migrate` trong config.

### Project Structure Notes

- Migration source: `apps/api/src/main/resources/db/migration`.

### References

- `review/sprint-refactoring-scripts-constants.md`
- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- 2026-05-28: Bắt đầu triển khai story R1.5; sprint-status không có key `r1-5-analyze-flyway-migration-squash-groups`, nên chỉ cập nhật trạng thái trong story file.
- 2026-05-28: Red phase chạy `node --test scripts/db/analyze-flyway-squash-groups.test.mjs` thất bại vì module analyzer chưa tồn tại.
- 2026-05-28: Targeted tests pass: `node --test scripts/db/analyze-flyway-squash-groups.test.mjs` với 4/4 tests pass.
- 2026-05-28: Repo workspace tests pass: `pnpm test` với web lint + 22 Vitest files / 82 tests pass.
- 2026-05-28: API regression `cd apps/api && ./gradlew test` chưa chạy được vì máy không có Java Runtime.
- 2026-05-28: Code review findings fixed; `node --test scripts/db/analyze-flyway-squash-groups.test.mjs` pass 5/5, `pnpm test` pass gồm `test:db` + web lint/Vitest 22 files / 82 tests.

### Completion Notes List

- Thêm script `scripts/db/analyze-flyway-squash-groups.mjs` để inventory Flyway migrations theo version order, classify domain, và render `text`, `markdown`, hoặc `json`.
- Markdown report đề xuất nhiều logical baseline groups: identity/auth, profile/privacy, health-record/OCR, reference/AI/RAG, audit/activity/notification; không sinh baseline thật.
- Rollout warnings nêu rõ analysis-only, không sửa/xóa active migrations, cần test fresh DB và existing migrated DB, và không thay đổi `baseline-on-migrate` trong story này.
- Xác minh script đọc 50 migrations hiện tại trong `apps/api/src/main/resources/db/migration`; không có diff ở migration files hoặc Flyway config.
- Thêm test coverage bằng `node:test` cho version ordering, domain grouping, markdown warning content, và CLI markdown output.
- Sau review, classifier ưu tiên filename signals rõ ràng hơn SQL reference noise, thêm regression test cho `profile_fields_to_users` và activity events.
- Nối `scripts/db/*.test.mjs` vào root `pnpm test` qua `test:db`.

### File List

- package.json
- scripts/db/analyze-flyway-squash-groups.mjs
- scripts/db/analyze-flyway-squash-groups.test.mjs
- _bmad-output/implementation-artifacts/refactor-stories/r1-5-analyze-flyway-migration-squash-groups.md

### Change Log

- 2026-05-28: Added Flyway squash group analyzer and tests; marked story ready for review.
- 2026-05-28: Addressed code review findings and marked story done.
