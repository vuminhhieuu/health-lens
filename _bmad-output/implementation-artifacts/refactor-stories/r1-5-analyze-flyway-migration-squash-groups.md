# Story R1.5: Analyze Flyway Migration Squash Groups

Status: ready-for-dev

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

- [ ] Tạo script phân tích dưới `scripts/db/`. (AC: 1,2)
- [ ] Classify migrations theo domain: identity/auth, profile/privacy, health-record/OCR, reference/AI/RAG, audit/activity/notification. (AC: 2)
- [ ] Thêm markdown output option. (AC: 1,2)
- [ ] Ghi rollout warnings. (AC: 3,4)

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

TBD

### Debug Log References

### Completion Notes List

### File List

