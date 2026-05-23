# Story 6.3: Frontend Bundle And Image Rule Cleanup

Status: done

## Execution Scope

**Phase:** Remaining production review / frontend maintainability  
**Area:** Bundle impact, image lint warnings, Next.js frontend hygiene  
**Priority:** P3

## Story

As a maintainer,  
I want frontend bundle and image warnings addressed,  
so that build output remains clean and predictable.

## Acceptance Criteria

1. `@radix-ui/themes` usage is reviewed for tree-shaking impact.
2. `@next/next/no-img-element` warnings are resolved or explicitly justified.
3. Bundle-impact changes are measured before/after where practical.

## Tasks / Subtasks

- [x] Task 1 - Review `@radix-ui/themes` usage and import patterns (AC: #1)
- [x] Task 2 - Resolve or justify raw `<img>` usages (AC: #2)
- [x] Task 3 - Measure bundle/build impact where tooling exists (AC: #3)
- [x] Task 4 - Document any intentional lint exceptions (AC: #2, #3)

## Dev Notes

### Implementation Guardrails

- Do not replace image handling mechanically if it breaks authenticated/private images.
- Measure before introducing broad component/library changes.
- Keep this story lower priority than production correctness and security stories.

### Likely Files

- `apps/web/src/**/*`
- `apps/web/next.config.*`
- `apps/web/eslint.config.*`
- `apps/web/package.json`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 6.3
- `_bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

- Replaced the lone `@radix-ui/themes` `Callout` usage in the delete-account flow with a local alert panel, leaving `Theme` and the global Radix stylesheet as the only remaining themes dependency surface in the app shell.
- Tightened `SafeImage` into a discriminated raw-vs-Next/Image wrapper so blob/data/authenticated image cases still use plain `<img>`, while the optimized branch preserves Next/Image sizing requirements.
- Verified the web app production build after clearing stale `.next` output; the build completed successfully and the route manifest remained unchanged.
- Ran `next experimental-analyze --output` before and after the delete-account `Callout` swap; total analyzed bundle size stayed the same at `4,354,194` bytes uncompressed and `831,874` bytes compressed, so the change did not measurably affect the app bundle.
- `pnpm --dir apps/web lint` still reports one pre-existing warning in `apps/web/src/app/(dashboard)/settings/_components/SettingsAccountNav.tsx:46` about `aria-disabled` on `listitem`; that issue is outside this story's scope.

### File List

- `apps/web/src/components/ui/SafeImage.tsx`
- `apps/web/src/app/(dashboard)/settings/delete-account/page.tsx`
