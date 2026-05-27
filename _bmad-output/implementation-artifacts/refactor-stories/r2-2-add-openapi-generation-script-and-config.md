# Story R2.2: Add OpenAPI Generation Script And Config

Status: ready-for-dev

## Story

Là developer,
tôi muốn command sinh OpenAPI contracts có thể chạy lặp lại,
để generated contracts được tái tạo nhất quán.

## Acceptance Criteria

1. **Given** API expose OpenAPI docs qua springdoc **When** generation script được thêm **Then** script mô tả prerequisites, input source, output location, và failure behavior.
2. **Given** generated files được tạo **When** rerun generation **Then** output deterministic hoặc differences explainable.
3. **Given** story chỉ thêm generation foundation **When** review diff **Then** không migrate toàn bộ frontend API client.

## Tasks / Subtasks

- [ ] Thêm script dưới root scripts theo decision R2.1. (AC: 1)
- [ ] Thêm config OpenAPI generator. (AC: 1)
- [ ] Document cách chạy với API local hoặc OpenAPI JSON file. (AC: 1)
- [ ] Chạy generation smoke nếu môi trường cho phép; nếu không, ghi lý do. (AC: 2)

## Dev Notes

- Ưu tiên generated artifacts phục vụ shared usage.
- Không tự ý upgrade framework versions.

### Project Structure Notes

- Depends on R2.1.
- Candidate tool: `@openapitools/openapi-generator-cli` nếu đã chốt.

### References

- `_bmad-output/planning-artifacts/architecture.md`
- `docs/project-context.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

