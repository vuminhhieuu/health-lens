# Story R1.6: Update Script Documentation And Package Entrypoints

Status: ready-for-dev

## Story

Là developer,
tôi muốn docs và package scripts trỏ tới root scripts mới,
để onboarding và vận hành không dùng command path cũ.

## Acceptance Criteria

1. **Given** scripts đã move về root **When** docs được cập nhật **Then** README, development guide, operations runbook, deployment guide, Infisical docs dùng path mới.
2. **Given** old paths tồn tại trong historical review **When** search active docs **Then** old paths không còn trong active operational instructions.
3. **Given** root `package.json` có convenience scripts **When** đọc scripts **Then** common Docker và DB analysis commands discoverable.

## Tasks / Subtasks

- [ ] Update `README.md`, `docs/development-guide.md`, `docs/operations-runbook.md`, `docs/deployment-guide.md`. (AC: 1)
- [ ] Update `infisical/README.md` và `infisical/ONBOARDING.md`. (AC: 1)
- [ ] Add root `package.json` scripts cho Docker lifecycle và DB analysis. (AC: 3)
- [ ] Run `rg` để kiểm tra active docs không còn path cũ. (AC: 2)

## Dev Notes

- Không sửa historical review files trong story này.
- Nếu README có examples nhiều dòng, update đồng bộ để tránh mixed path.

### Project Structure Notes

- Depends on R1.3/R1.4 paths.

### References

- `docs/project-context.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

