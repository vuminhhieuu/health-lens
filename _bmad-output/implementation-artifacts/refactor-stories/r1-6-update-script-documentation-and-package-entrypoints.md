# Story R1.6: Update Script Documentation And Package Entrypoints

Status: done

## Story

Là developer,
tôi muốn docs và package scripts trỏ tới root scripts mới,
để onboarding và vận hành không dùng command path cũ.

## Acceptance Criteria

1. **Given** scripts đã move về root **When** docs được cập nhật **Then** README, development guide, operations runbook, deployment guide, Infisical docs dùng path mới.
2. **Given** old paths tồn tại trong historical review **When** search active docs **Then** old paths không còn trong active operational instructions.
3. **Given** root `package.json` có convenience scripts **When** đọc scripts **Then** common Docker và DB analysis commands discoverable.

## Tasks / Subtasks

- [x] Update `README.md`, `docs/development-guide.md`, `docs/operations-runbook.md`, `docs/deployment-guide.md`. (AC: 1)
- [x] Update `infisical/README.md` và `infisical/ONBOARDING.md`. (AC: 1)
- [x] Add root `package.json` scripts cho Docker lifecycle và DB analysis. (AC: 3)
- [x] Run `rg` để kiểm tra active docs không còn path cũ. (AC: 2)

### Review Findings

- [x] [Review][Patch] Infisical docs still reference old `./infisical/scripts/infisical.sh` path [infisical/README.md:13]
- [x] [Review][Patch] `docker:down:volumes` exposes destructive non-TTY volume removal without explicit confirmation [package.json:16]
- [x] [Review][Patch] Package alias docs do not explain pnpm/npm argument forwarding for advanced Docker options [README.md:97]

## Dev Notes

- Không sửa historical review files trong story này.
- Nếu README có examples nhiều dòng, update đồng bộ để tránh mixed path.

### Project Structure Notes

- Depends on R1.3/R1.4 paths.

### References

- `docs/project-context.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `node -e "JSON.parse(require('fs').readFileSync('package.json','utf8')); console.log('package.json ok')"`
- `pnpm run`
- `pnpm test:db`
- `pnpm db:analyze:flyway-squash --help`
- `rg -n "\.\/docker\/scripts|docker/scripts" README.md docs infisical package.json; test $? -eq 1`
- `pnpm test`
- `bash -n scripts/docker/down.sh scripts/docker/up.sh scripts/docker/logs.sh scripts/docker/cleanup.sh scripts/infisical/infisical.sh`
- `rg -n "\.\/docker\/scripts|docker/scripts|\.\/infisical\/scripts|infisical/scripts" README.md docs infisical package.json`
- `./scripts/infisical/infisical.sh help`
- Docker mock validation for `./scripts/docker/down.sh -v --ci` non-TTY refusal and `./scripts/docker/down.sh -v --yes --ci` success

### Completion Notes List

- Updated active onboarding and operational docs to use root `scripts/docker` paths and root package aliases.
- Added root package scripts for Docker lifecycle commands and Flyway squash analysis discovery.
- Verified old `docker/scripts` paths are absent from active README/docs/Infisical/package instructions.
- Full root test suite passed.
- Resolved code review findings: Infisical docs now use `scripts/infisical/infisical.sh`; destructive Docker down operations refuse non-interactive execution without `--yes` or `--force`; docs now clarify pnpm/npm argument forwarding.

### File List

- `README.md`
- `docs/development-guide.md`
- `docs/operations-runbook.md`
- `docs/deployment-guide.md`
- `docs/source-tree-analysis.md`
- `infisical/README.md`
- `infisical/ONBOARDING.md`
- `package.json`
- `scripts/docker/down.sh`
- `_bmad-output/implementation-artifacts/refactor-stories/r1-6-update-script-documentation-and-package-entrypoints.md`

### Change Log

- 2026-05-28: Updated documentation and package entrypoints for root script locations.
- 2026-05-28: Resolved code review findings and marked story done.
