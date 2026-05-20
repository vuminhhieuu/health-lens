# Story 7.13: Refresh Source Tree And Architecture Docs

Status: proposed

## Execution Scope

**Area:** Documentation accuracy, source-tree references, architecture drift cleanup  
**Priority:** P3

## Story

As a maintainer, I want project structure and architecture documents refreshed after Epic 7 cleanup, so that repository docs match the real codebase and help future contributors navigate correctly.

## Acceptance Criteria

1. **Given** source-tree and architecture docs are reviewed, **When** this story is complete, **Then** outdated package, migration, and source-root references are corrected.
2. **Given** Epic 7 structural changes land, **When** later contributors read the docs, **Then** the documented project layout matches the actual repository.
3. **Given** maintainers need one current overview, **When** they inspect project documentation, **Then** code-organization and architecture guidance are internally consistent.

## Tasks / Subtasks

- [ ] Refresh source-tree documentation to match current backend, web, mobile, and shared package structure.
- [ ] Update architecture or project-context references that drifted from the current repository state.
- [ ] Reflect the final Epic 7 package decisions after the relevant stories complete.
- [ ] Verify that cross-linked docs still point to valid files and concepts.

## Dev Notes

- Do this after the substantive Epic 7 refactor stories, not before them.
- Keep documentation changes factual and repository-grounded.
- Treat this as the documentation close-out story for Epic 7 organization work.

## Likely Files

- `docs/source-tree-analysis.md`
- `docs/project-context.md`
- `docs/index.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/`

## References

- `epic-7-issues-and-proposed-stories.md`
- `code-organization-assessment.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
