# Refactor Baseline - 2026-05-27

## Metadata

- Project: `health-lens`
- Branch: `tmp/task-refactor`
- Captured at: `2026-05-27T13:34:48+0700`
- Story: `R1.1 Capture Refactor Baseline And Quality Gates`
- Scope: baseline documentation only; no application source files were changed by this story.

## Git Working Tree Snapshot

Command:

```bash
git status --short
```

Output captured before the R1.1 baseline report and master-roadmap placeholder were written:

```text
A  _bmad-output/implementation-artifacts/epic-7/7-6-core-feature-reference-dataset-v1.md
A  _bmad-output/implementation-artifacts/epic-7/7-7-activate-rag-corpus-ingestion-approval-pipeline.md
A  _bmad-output/implementation-artifacts/public-account-experience/pae-13-notification-inbox-pagination.md
M  _bmad-output/implementation-artifacts/sprint-status.yaml
M  _bmad-output/planning-artifacts/epics.md
M  apps/api/src/main/java/com/healthlens/api/dto/response/AdminReferenceImportPreviewRowResponse.java
M  apps/api/src/main/java/com/healthlens/api/repository/ReferenceMetricAliasRepository.java
M  apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java
M  apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java
M  apps/api/src/main/java/com/healthlens/api/service/ProfileService.java
M  apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java
M  apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java
M  apps/api/src/main/resources/ai/metric-explanations.vi.json
M  apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java
M  apps/api/src/test/java/com/healthlens/api/service/MetricExplanationRetrievalServiceTest.java
M  apps/api/src/test/java/com/healthlens/api/service/ProfileServiceTest.java
M  apps/api/src/test/java/com/healthlens/api/service/ReferenceDataAdminServiceTest.java
M  apps/api/src/test/java/com/healthlens/api/service/ReferenceDataServiceTest.java
A  docs/reference-data/core-feature-data-requirements.md
A  docs/reference-data/core-feature-reference-dataset-v1.csv
A  docs/reference-data/core-feature-reference-dataset-v1.provenance.md
A  docs/reference-data/initial-reference-ranges.csv
A  docs/reference-data/initial-reference-ranges.provenance.md
A  review/api-package-structure-analysis.md
A  review/constants-scripts-deep-dive.md
A  review/current-project-refactor-audit-2026-05-26.md
A  review/full-project-structure-analysis.md
A  review/sprint-refactoring-scripts-constants.md
?? _bmad-output/implementation-artifacts/refactor-stories/
?? _bmad-output/planning-artifacts/refactor-epics-and-stories.md
```

Expanded untracked refactor-story inventory from `git status --short -uall` during review:

```text
?? _bmad-output/implementation-artifacts/refactor-stories/index.md
?? _bmad-output/implementation-artifacts/refactor-stories/r1-1-capture-refactor-baseline-and-quality-gates.md
?? _bmad-output/implementation-artifacts/refactor-stories/r1-2-create-master-refactor-roadmap-source-of-truth.md
?? _bmad-output/implementation-artifacts/refactor-stories/r1-3-move-automation-scripts-to-root-structure.md
?? _bmad-output/implementation-artifacts/refactor-stories/r1-4-extract-shared-docker-script-utilities.md
?? _bmad-output/implementation-artifacts/refactor-stories/r1-5-analyze-flyway-migration-squash-groups.md
?? _bmad-output/implementation-artifacts/refactor-stories/r1-6-update-script-documentation-and-package-entrypoints.md
?? _bmad-output/implementation-artifacts/refactor-stories/r2-1-decide-openapi-generated-contract-ownership.md
?? _bmad-output/implementation-artifacts/refactor-stories/r2-2-add-openapi-generation-script-and-config.md
?? _bmad-output/implementation-artifacts/refactor-stories/r2-3-quarantine-dead-and-planned-shared-contracts.md
?? _bmad-output/implementation-artifacts/refactor-stories/r2-4-add-error-code-compatibility-layer.md
?? _bmad-output/implementation-artifacts/refactor-stories/r2-5-modularize-active-shared-constants.md
?? _bmad-output/implementation-artifacts/refactor-stories/r2-6-add-contract-drift-ci-checks.md
?? _bmad-output/implementation-artifacts/refactor-stories/r3-1-define-backend-package-boundary-rules.md
?? _bmad-output/implementation-artifacts/refactor-stories/r3-2-consolidate-ocr-package-boundary.md
?? _bmad-output/implementation-artifacts/refactor-stories/r3-3-consolidate-ai-and-rag-package-boundary.md
?? _bmad-output/implementation-artifacts/refactor-stories/r3-4-consolidate-activity-and-notification-packages.md
?? _bmad-output/implementation-artifacts/refactor-stories/r3-5-move-support-domains-to-feature-packages.md
?? _bmad-output/implementation-artifacts/refactor-stories/r3-6-move-core-domains-to-feature-packages.md
?? _bmad-output/implementation-artifacts/refactor-stories/r3-7-clean-resource-directory-ownership.md
?? _bmad-output/implementation-artifacts/refactor-stories/r4-1-split-healthrecordservice-responsibilities.md
?? _bmad-output/implementation-artifacts/refactor-stories/r4-2-split-referencedataadminservice-responsibilities.md
?? _bmad-output/implementation-artifacts/refactor-stories/r4-3-split-llmservice-responsibilities.md
?? _bmad-output/implementation-artifacts/refactor-stories/r4-4-split-ocrservice-responsibilities.md
?? _bmad-output/implementation-artifacts/refactor-stories/r4-5-split-adminauditlogservice-where-needed.md
?? _bmad-output/implementation-artifacts/refactor-stories/r5-1-define-web-feature-extraction-pattern.md
?? _bmad-output/implementation-artifacts/refactor-stories/r5-2-extract-health-record-review-presentational-components.md
?? _bmad-output/implementation-artifacts/refactor-stories/r5-3-extract-health-record-review-hook-and-domain-utilities.md
?? _bmad-output/implementation-artifacts/refactor-stories/r5-4-refactor-admin-reference-data-pages.md
?? _bmad-output/implementation-artifacts/refactor-stories/r5-5-refactor-admin-audit-and-admin-login-pages.md
?? _bmad-output/implementation-artifacts/refactor-stories/r5-6-move-loose-web-lib-files-into-domain-folders.md
?? _bmad-output/implementation-artifacts/refactor-stories/r6-1-mark-historical-review-files-as-superseded-inputs.md
?? _bmad-output/implementation-artifacts/refactor-stories/r6-2-reorganize-active-documentation-by-audience.md
?? _bmad-output/implementation-artifacts/refactor-stories/r6-3-update-developer-and-operations-guides-after-refactor-phases.md
?? _bmad-output/implementation-artifacts/refactor-stories/rd1-1-decide-mobile-folder-convention.md
?? _bmad-output/implementation-artifacts/refactor-stories/rd1-2-consolidate-mobile-source-structure.md
```

## Pre-existing Change Groups

- BMad implementation/planning artifacts: new epic/public-account stories, modified `sprint-status.yaml`, modified product `epics.md`, new refactor story folder, and new refactor planning document.
- API source and tests: modified Java services, repository, response DTO, AI resource JSON, and related service tests.
- Reference data documentation: new CSV/provenance and requirements files under `docs/reference-data/`.
- Review inputs: new refactor/audit review files under `review/`.

These groups existed before R1.1 baseline artifacts were written. Any failures in unrelated source checks should be treated as pre-existing unless a later story changes the affected files.

## Baseline Commands

| Command | Status | Evidence | Pre-existing label |
| --- | --- | --- | --- |
| `pnpm --filter @healthlens/shared build` | pass | `tsc -p tsconfig.json --noEmit` completed with exit code 0. | Not applicable |
| `pnpm --filter web test` | pass | ESLint completed and Vitest reported 22 files / 82 tests passed. | Not applicable |
| `pnpm --filter mobile lint` | pass | `expo lint` completed with exit code 0; warning: Node.js `v18.16.0` is unsupported, required `>=20.19.4`. | Environment warning pre-existing |
| `cd apps/api && ./gradlew test` | skipped | Command could not start because shell reported no Java Runtime available. | Environment blocker pre-existing |

## Baseline Interpretation

- Shared and web quality gates are currently runnable and passing in this shell.
- Mobile lint is runnable and passing, but the local Node runtime is below the version expected by Expo. Future mobile work should either use Node `>=20.19.4` or keep recording this as an environment warning.
- API tests are not runnable in the current shell until Java 21 is available. This story did not change API source/test files, so the blocked API baseline is an environment prerequisite gap, not a refactor regression.

## Minimum Quality Gates For Future Refactor Stories

- Shared contract changes: run `pnpm --filter @healthlens/shared build`; run impacted web/mobile checks when shared exports are consumed.
- Web changes: run `pnpm --filter web test`; add focused tests for extracted components/hooks/utilities when behavior changes or moves.
- Mobile changes: run `pnpm --filter mobile lint` under Node `>=20.19.4`; record environment warnings if local runtime is older.
- API changes: run `cd apps/api && ./gradlew test` under Java 21. If Java is unavailable and the story changed API files, the story must remain incomplete unless an explicit reviewer-approved exception is recorded; it must not claim API pass.
