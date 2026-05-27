# Story R2.1: Decide OpenAPI Generated Contract Ownership

Status: ready-for-dev

## Story

Là developer,
tôi muốn có quyết định ownership cho generated OpenAPI contracts,
để code generated có vị trí, policy review, và consumer rõ trước khi sinh code.

## Acceptance Criteria

1. **Given** web và mobile tương lai đều cần API contracts **When** decision được viết **Then** nó chọn output location, generation command, commit policy, và consumer packages.
2. **Given** có nhiều lựa chọn output **When** ADR/decision được review **Then** nó giải thích vì sao không chọn alternatives.
3. **Given** generated output ảnh hưởng package boundary **When** decision hoàn tất **Then** nó nêu quan hệ giữa `packages/shared`, `apps/web`, và mobile tương lai.

## Tasks / Subtasks

- [ ] Rà hiện trạng `springdoc-openapi`, shared package, web API client. (AC: 1)
- [ ] Viết ADR/decision trong `review/` hoặc docs roadmap. (AC: 1,2,3)
- [ ] Chốt naming/location cho generated artifacts, ví dụ `packages/shared/generated/openapi`. (AC: 1)
- [ ] Nêu policy commit generated files và CI stale check. (AC: 1)

## Dev Notes

- Không generate code trong story này.
- Không migrate frontend API clients trong story này.

### Project Structure Notes

- Relevant files: `apps/api/build.gradle.kts`, `apps/web/src/lib/api/apiClient.ts`, `packages/shared/package.json`.

### References

- `_bmad-output/planning-artifacts/architecture.md`
- `review/constants-scripts-deep-dive.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

