# Story R3.2: Consolidate OCR Package Boundary

Status: ready-for-dev

## Story

Là backend developer,
tôi muốn OCR-related code nằm trong OCR feature package,
để thay đổi OCR không cần dò nhiều root layer packages.

## Acceptance Criteria

1. **Given** OCR code split across service/controller/config/entity/repository/dto/events **When** migration hoàn tất **Then** OCR classes nằm trong OCR bounded package hoặc documented common package.
2. **Given** story là package/import move **When** behavior được kiểm tra **Then** OCR behavior không đổi.
3. **Given** OCR tests tồn tại **When** update tests **Then** test package mirrors source và relevant tests pass.

## Tasks / Subtasks

- [ ] Inventory OCR classes và tests. (AC: 1)
- [ ] Move package declarations/imports theo rules R3.1. (AC: 1)
- [ ] Update component scan/imports/tests. (AC: 2,3)
- [ ] Run targeted API tests for OCR. (AC: 3)

## Dev Notes

- Không split `OcrService` trong story này; đó là R4.4.
- Preserve event stream names and provider registry behavior.

### Project Structure Notes

- Relevant current packages: `ocr`, `service/ocr`, `events/ocr`, layer roots.

### References

- `review/api-package-structure-analysis.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

