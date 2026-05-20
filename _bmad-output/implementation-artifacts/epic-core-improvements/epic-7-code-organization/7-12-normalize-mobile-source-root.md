# Story 7.12: Normalize Mobile Source Root

Status: proposed

## Execution Scope

**Area:** Mobile source-tree cleanup, repository navigation, maintainability  
**Priority:** P3

## Story

As a maintainer, I want the mobile app to use one canonical source root, so that navigation, onboarding, and future mobile work are not confused by duplicate or placeholder app structures.

## Acceptance Criteria

1. **Given** the mobile app source tree is reviewed, **When** this story is complete, **Then** one canonical source root is documented and the redundant structure is removed or clearly deprecated.
2. **Given** mobile developers navigate the repository, **When** they inspect `apps/mobile`, **Then** the intended app entry structure is obvious.
3. **Given** the current mobile build or lint flow exists, **When** verification runs, **Then** cleanup does not break the active source root.

## Tasks / Subtasks

- [ ] Confirm whether `app/` or `src/app/` is the canonical mobile source root.
- [ ] Remove or deprecate redundant placeholder structure in a safe way.
- [ ] Update any related config or developer notes if paths depend on the chosen root.
- [ ] Verify the mobile workspace still resolves the active app source tree.

## Dev Notes

- This is a cleanup story, not a mobile feature story.
- Keep the active mobile runtime behavior unchanged.
- If redundant folders are intentionally staged for future work, document that explicitly instead of silently deleting them.

## Likely Files

- `apps/mobile/`
- `apps/mobile/app/`
- `apps/mobile/src/app/`
- `apps/mobile/package.json`
- `apps/mobile/tsconfig.json`

## References

- `epic-7-issues-and-proposed-stories.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
