# Story R2.1: Decide OpenAPI Generated Contract Ownership

Status: done

## Story

Là developer,
tôi muốn có quyết định ownership cho generated OpenAPI contracts,
để code generated có vị trí, policy review, và consumer rõ trước khi sinh code.

## Acceptance Criteria

1. **Given** web và mobile tương lai đều cần API contracts **When** decision được viết **Then** nó chọn output location, generation command, commit policy, và consumer packages.
2. **Given** có nhiều lựa chọn output **When** ADR/decision được review **Then** nó giải thích vì sao không chọn alternatives.
3. **Given** generated output ảnh hưởng package boundary **When** decision hoàn tất **Then** nó nêu quan hệ giữa `packages/shared`, `apps/web`, và mobile tương lai.

## Tasks / Subtasks

- [x] Rà hiện trạng `springdoc-openapi`, shared package, web API client. (AC: 1)
- [x] Viết ADR/decision trong `review/` hoặc docs roadmap. (AC: 1,2,3)
- [x] Chốt naming/location cho generated artifacts, ví dụ `packages/shared/generated/openapi`. (AC: 1)
- [x] Nêu policy commit generated files và CI stale check. (AC: 1)

### Review Findings

- [x] [Review][Patch] Shared build can miss generated contract breakage [review/openapi-generated-contract-ownership-decision-2026-05-28.md:101]
- [x] [Review][Patch] Generated export boundary is not pinned [review/openapi-generated-contract-ownership-decision-2026-05-28.md:101]
- [x] [Review][Patch] Runtime generator output lacks dependency/runtime constraints [review/openapi-generated-contract-ownership-decision-2026-05-28.md:39]
- [x] [Review][Patch] CI OpenAPI source is ambiguous [review/openapi-generated-contract-ownership-decision-2026-05-28.md:62]
- [x] [Review][Patch] Active decision is not wired into the roadmap source-of-truth convention [review/refactor-master-roadmap-2026-05-27.md:27]

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

GPT-5 Codex

### Debug Log References

- 2026-05-28: Loaded BMad config, project context, story file, sprint status, architecture notes, refactor roadmap, constants/scripts review, SpringDoc config, web API client, and `@healthlens/shared` package metadata.
- 2026-05-28: Confirmed current state: SpringDoc dependency/config exists in API, OpenAPI docs path is `/v3/api-docs`, web uses hand-written Axios `apiClient.ts`, and `packages/shared` is the TS shared package for cross-app contracts.
- 2026-05-28: Added `review/openapi-generated-contract-ownership-decision-2026-05-28.md` documenting output location, generation/check commands, commit policy, CI stale-check expectation, consumer package boundaries, and rejected alternatives.
- 2026-05-28: Validation passed: `rg -n "packages/shared/generated/openapi|pnpm openapi:generate|pnpm openapi:check|Commit Policy|Consumer Packages|Alternatives|apps/web|Mobile" review/openapi-generated-contract-ownership-decision-2026-05-28.md`; `pnpm --filter @healthlens/shared build`; `git diff --check`.
- 2026-05-28: Code review resolved 5 patch findings by pinning generated type-check/export boundary, type-only default generation, CI OpenAPI source expectations, and roadmap linkage.

### Completion Notes List

- Created a Vietnamese ownership decision for generated OpenAPI contracts.
- Chose `packages/shared/generated/openapi` as the generated output location and `packages/shared` as the contract-owning package.
- Defined future commands `pnpm openapi:generate` and `pnpm openapi:check` for R2.2/R2.6 implementation.
- Documented commit/review policy for generated files and CI stale-check behavior.
- Clarified that `apps/web` remains a consumer using its existing auth-aware API client, while future mobile consumes generated contracts through `@healthlens/shared` with its own runtime adapter.
- No application code changed and no OpenAPI code was generated; `@healthlens/shared` build passed, and app/API/web/mobile test suites were not run because this story is documentation-only.
- Addressed all code review patch findings and marked Story R2.1 done.

### File List

- `review/openapi-generated-contract-ownership-decision-2026-05-28.md`
- `review/refactor-master-roadmap-2026-05-27.md`
- `_bmad-output/implementation-artifacts/refactor-stories/r2-1-decide-openapi-generated-contract-ownership.md`

### Change Log

- 2026-05-28: Added OpenAPI generated contract ownership decision and moved Story R2.1 to review.
- 2026-05-28: Resolved code review findings and moved Story R2.1 to done.
