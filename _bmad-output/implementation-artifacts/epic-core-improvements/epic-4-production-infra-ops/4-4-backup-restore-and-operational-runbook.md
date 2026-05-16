# Story 4.4: Backup, Restore And Operational Runbook

Status: ready-for-dev

## Execution Scope

**Area:** Backup/restore, runbooks, RPO/RTO, operational drills  
**Priority:** P0

## Story

As an operator, I want verified backup/restore procedures and runbooks, so that health records, files, and vector data can be recovered after incidents.

## Acceptance Criteria

1. **Given** a backup exists, **When** restore drill is executed, **Then** app can read restored users/profiles/records/files.
2. **Given** Qdrant data is not backed up directly, **When** restore is needed, **Then** system can reingest corpus from source version and embedding config.
3. **Given** OCR/LLM/provider outage occurs, **When** operator follows runbook, **Then** service can degrade or fail over predictably.
4. **Given** RPO/RTO are defined, **When** restore drill completes, **Then** measured times are recorded.

## Tasks / Subtasks

- [ ] Define RPO/RTO.
- [ ] Document Postgres backup/restore.
- [ ] Document object storage backup/restore.
- [ ] Document Qdrant reingestion or backup strategy.
- [ ] Run and record restore drill.
- [ ] Add outage runbooks for OCR, LLM, DB, Redis, Qdrant.

## Dev Notes

- For vector data, source-of-truth may be corpus + embedding config rather than Qdrant snapshots.
- Do not mark production-ready until restore has been tested.

## Likely Files

- `docs/ops/backup-restore-runbook.md`
- `docs/ops/incident-runbook.md`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 4.4

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
