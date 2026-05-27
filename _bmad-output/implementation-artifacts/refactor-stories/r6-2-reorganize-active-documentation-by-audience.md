# Story R6.2: Reorganize Active Documentation By Audience

Status: ready-for-dev

## Story

Là project contributor,
tôi muốn docs được tổ chức theo architecture, guides, operations, reference, roadmap,
để tìm thông tin hiện hành nhanh hơn.

## Acceptance Criteria

1. **Given** docs hiện partly flat và partly grouped **When** reorganize hoàn tất **Then** active docs có folder structure documented và index cập nhật.
2. **Given** docs có relative links **When** files move **Then** links được cập nhật và check.
3. **Given** review docs là historical **When** docs reorganize **Then** không làm mất traceability của review inputs.

## Tasks / Subtasks

- [ ] Propose target docs structure. (AC: 1)
- [ ] Move active docs in small batches. (AC: 1)
- [ ] Update `docs/index.md` and links. (AC: 1,2)
- [ ] Run link/path search checks. (AC: 2)

## Dev Notes

- Do this after major refactor phases settle.
- Avoid moving docs that active tools expect unless references update.

### Project Structure Notes

- Current docs root: `docs/`.

### References

- `review/full-project-structure-analysis.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

