# Refactor Master Roadmap - 2026-05-27

## Status

Placeholder created by Story R1.1 to hold active minimum quality gates before refactor implementation begins.

Story R1.2 owns expanding this file into the full master roadmap with phase order, accepted decisions, exit criteria, rollback notes, and historical review references.

## Active Baseline

- Baseline report: `_bmad-output/implementation-artifacts/refactor/refactor-baseline-2026-05-27.md`
- Source backlog: `_bmad-output/planning-artifacts/refactor-epics-and-stories.md`
- Baseline branch: `tmp/task-refactor`
- Baseline date: `2026-05-27`

## Minimum Quality Gates

- Shared/package contract changes must run `pnpm --filter @healthlens/shared build`. If shared exports are consumed by web or mobile, also run the relevant web/mobile checks.
- Web changes must run `pnpm --filter web test` and include focused coverage for extracted components, hooks, or domain utilities when behavior is moved.
- Mobile changes must run `pnpm --filter mobile lint` with a supported Node runtime (`>=20.19.4`). If the local runtime is older, record the warning explicitly and do not treat it as a clean environment.
- API changes must run `cd apps/api && ./gradlew test` with Java 21 available.
- Any skipped or failed gate must be labeled as pre-existing only when the story did not change the related files. If the story changed related files, it must either fix the failure, get an explicit reviewer-approved exception, or remain incomplete.

## R1.2 Expansion Checklist

- Identify accepted decisions for scripts, Flyway, OpenAPI, backend packaging, frontend extraction, mobile deferral, and docs handling.
- Link historical `review/*.md` files as inputs, not active instructions.
- Add phase order, phase exit criteria, and rollback notes.
- Preserve the minimum quality gates above unless a later reviewed decision supersedes them.
