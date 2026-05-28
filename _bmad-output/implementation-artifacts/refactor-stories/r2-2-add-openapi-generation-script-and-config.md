# Story R2.2: Add OpenAPI Generation Script And Config

Status: done

## Story

Là developer,
tôi muốn command sinh OpenAPI contracts có thể chạy lặp lại,
để generated contracts được tái tạo nhất quán.

## Acceptance Criteria

1. **Given** API expose OpenAPI docs qua springdoc **When** generation script được thêm **Then** script mô tả prerequisites, input source, output location, và failure behavior.
2. **Given** generated files được tạo **When** rerun generation **Then** output deterministic hoặc differences explainable.
3. **Given** story chỉ thêm generation foundation **When** review diff **Then** không migrate toàn bộ frontend API client.

## Tasks / Subtasks

- [x] Thêm script dưới root scripts theo decision R2.1. (AC: 1)
- [x] Thêm config OpenAPI generator. (AC: 1)
- [x] Document cách chạy với API local hoặc OpenAPI JSON file. (AC: 1)
- [x] Chạy generation smoke nếu môi trường cho phép; nếu không, ghi lý do. (AC: 2)

### Review Findings

- [x] [Review][Patch] Generation script can recursively delete arbitrary output paths [scripts/openapi/generate.mjs:146]
- [x] [Review][Patch] OpenAPI script tests are not part of the root test command [package.json:9]

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

GPT-5 Codex

### Debug Log References

- 2026-05-28: Loaded BMad config, project context, story file, R2.1 ownership decision, architecture notes, sprint status, root/shared package metadata, and existing scripts layout.
- 2026-05-28: Added `scripts/openapi/generate.mjs`, OpenAPI Generator config, root package scripts, pinned `openapitools.json`, and unit tests for script argument/command behavior.
- 2026-05-28: Added `@healthlens/shared/generated/openapi` package export while preserving existing `@healthlens/shared/{constants,schemas,types,config}` deep-import boundaries.
- 2026-05-28: Fixed SpringDoc security scheme key from `Bearer Authentication` to `BearerAuthentication`; OpenAPI Generator rejects spaces in security scheme names.
- 2026-05-28: Smoke generated contracts from `/tmp/healthlens-openapi-current.json`, captured from local `/v3/api-docs` with the same security-scheme rename applied because Docker API rebuild stalled on base-image metadata pull.
- 2026-05-28: Determinism check passed: rerunning generation from the same OpenAPI JSON produced identical SHA-256 hashes for all files under `packages/shared/generated/openapi`.
- 2026-05-28: Validation passed: `pnpm run test:openapi`; `pnpm --filter @healthlens/shared build`; `pnpm test`; `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test` in `apps/api`; `git diff --check`.
- 2026-05-28: Code review resolved 2 patch findings by constraining OpenAPI generation output to `packages/shared/generated/openapi` and adding `test:openapi` to root `pnpm test`.

### Completion Notes List

- Added repeatable OpenAPI generation foundation at `pnpm openapi:generate`.
- Generation accepts default local API source `http://localhost:8080/v3/api-docs` or a JSON file via `--input` / `OPENAPI_INPUT`.
- Generated contracts are committed under `packages/shared/generated/openapi` and are model/type-only; no frontend API client migration or runtime HTTP client was introduced.
- Script documents and enforces failure behavior for unreachable URL, unreadable input file, missing config, and generator failure.
- Script rejects any output path outside `packages/shared/generated/openapi` before deleting/recreating generated files.
- Root `pnpm test` now runs OpenAPI script tests in addition to DB and workspace tests.
- `packages/shared` now type-checks generated files and exposes the subpath `@healthlens/shared/generated/openapi`.
- Wrapper CLI was pinned to `@openapitools/openapi-generator-cli@2.15.3` because the latest wrapper installed initially failed on this Node 18 environment with a CommonJS/ESM `proxy-agent` runtime error; generator jar is pinned separately to `7.14.0`.

### File List

- `apps/api/src/main/java/com/healthlens/api/config/OpenApiConfig.java`
- `openapitools.json`
- `package.json`
- `packages/shared/generated/openapi/.openapi-generator/FILES`
- `packages/shared/generated/openapi/.openapi-generator/VERSION`
- `packages/shared/generated/openapi/index.ts`
- `packages/shared/generated/openapi/models/index.ts`
- `packages/shared/package.json`
- `packages/shared/tsconfig.json`
- `pnpm-lock.yaml`
- `scripts/openapi/README.md`
- `scripts/openapi/generate.mjs`
- `scripts/openapi/generate.test.mjs`
- `scripts/openapi/openapi-generator.config.yaml`
- `_bmad-output/implementation-artifacts/refactor-stories/r2-2-add-openapi-generation-script-and-config.md`

### Change Log

- 2026-05-28: Added OpenAPI generation script/config, generated shared model contracts, package export/type-check boundary, documentation, and validation tests; moved Story R2.2 to review.
- 2026-05-28: Resolved code review findings and moved Story R2.2 to done.
