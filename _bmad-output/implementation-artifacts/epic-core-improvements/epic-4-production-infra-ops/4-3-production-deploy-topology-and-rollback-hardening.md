# Story 4.3: Production Deploy Topology And Rollback Hardening

Status: ready-for-dev

## Execution Scope

**Area:** Deployment topology, health gates, rollback, smoke tests  
**Priority:** P0/P1

## Story

As a release owner, I want production deployment to include all required services and rollback gates, so that API/Web/OCR/Redis/Qdrant dependencies are deployed predictably.

## Acceptance Criteria

1. **Given** production deploy runs, **When** deploy completes, **Then** API, web, OCR, Redis, DB, storage, and vector store health checks pass.
2. **Given** post-deploy smoke test fails, **When** rollback is triggered, **Then** previous stable version is restored according to runbook.
3. **Given** compose/swarm/kubernetes mode differs, **When** deploy docs are followed, **Then** service replicas/health behavior is accurate for that mode.
4. **Given** DB migration is required, **When** deploy runs, **Then** migration and rollback strategy are documented and gated.

## Tasks / Subtasks

- [ ] Include OCR service in production topology.
- [ ] Add deploy health gates.
- [ ] Clarify compose vs swarm/k8s behavior.
- [ ] Add rollback procedure.
- [ ] Add smoke test checklist.

## Dev Notes

- Do not rely on `deploy.replicas` unless running Docker Swarm.
- OCR service is part of production surface if upload/OCR is production feature.

## Likely Files

- `.github/workflows/deploy.yml`
- `docker/compose.prod.yml`
- `docs/STAGING_DEPLOYMENT.md`
- deployment runbooks

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 4.3

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
