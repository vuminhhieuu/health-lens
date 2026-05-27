# Story R6.1: Mark Historical Review Files As Superseded Inputs

Status: ready-for-dev

## Story

Là developer,
tôi muốn các historical review files được đánh dấu rõ,
để khuyến nghị cũ không mâu thuẫn với master roadmap hiện hành.

## Acceptance Criteria

1. **Given** review files chứa recommendations stale/conflicting **When** notes được update **Then** mỗi file trỏ tới current master roadmap.
2. **Given** historical content vẫn có giá trị traceability **When** file được cập nhật **Then** nội dung gốc vẫn được giữ.
3. **Given** developer đọc review cũ **When** mở file **Then** thấy rõ file là historical input, không phải active plan.

## Tasks / Subtasks

- [ ] Add superseded/historical notice vào đầu từng review file cũ. (AC: 1,2,3)
- [ ] Link master roadmap. (AC: 1)
- [ ] Verify no content loss. (AC: 2)

## Dev Notes

- Không rewrite toàn bộ review cũ.
- Giữ ngày và context lịch sử.

### Project Structure Notes

- Files under `review/`.

### References

- Master roadmap from R1.2.

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

