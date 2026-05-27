# Story R4.4: Split OcrService Responsibilities

Status: ready-for-dev

## Story

Là OCR feature developer,
tôi muốn tách upload, job, provider, và result persistence responsibilities,
để provider changes và job-state changes được cô lập.

## Acceptance Criteria

1. **Given** OCR package boundary đã consolidate **When** `OcrService` split hoàn tất **Then** upload orchestration, provider invocation, job state, và result persistence tách được.
2. **Given** OCR failure/partial recovery quan trọng **When** tests chạy **Then** error và partial-result recovery vẫn pass.
3. **Given** story là decomposition **When** review diff **Then** không đổi external OCR API contract.

## Tasks / Subtasks

- [ ] Map current OCR workflows and collaborators. (AC: 1)
- [ ] Extract focused services. (AC: 1)
- [ ] Update injection/call sites. (AC: 1)
- [ ] Update OCR tests. (AC: 2)

## Dev Notes

- Preserve provider registry behavior.
- Preserve DLQ/job-state behavior.

### Project Structure Notes

- Relevant file: `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`.

### References

- `review/api-package-structure-analysis.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

