# Story 8.3: Release Smoke Tests For Provider And Infrastructure Config

Status: ready-for-dev

## Execution Scope

**Area:** Release smoke tests, provider health, infrastructure readiness  
**Priority:** P1

## Story

As a release owner, I want deploy-time smoke tests for configured providers and infrastructure, so that bad env/config is caught before users hit broken flows.

## Acceptance Criteria

1. **Given** deployment completes, **When** smoke suite runs, **Then** API, OCR, Redis, Qdrant, and AI provider checks pass or deployment fails.
2. **Given** provider key is invalid, **When** smoke test calls provider health endpoint, **Then** release is blocked before user traffic is shifted.
3. **Given** Qdrant dimension/config is wrong, **When** smoke test runs, **Then** failure is explicit.
4. **Given** Redis stream publish/consume check fails, **When** smoke test runs, **Then** release is blocked or marked degraded according to policy.

## Tasks / Subtasks

- [ ] Add API health smoke test.
- [ ] Add OCR service health smoke test.
- [ ] Add safe AI provider health check.
- [ ] Add Qdrant connection/dimension check.
- [ ] Add Redis stream non-destructive check.
- [ ] Integrate with deploy workflow.

## Dev Notes

- Do not send real health data in smoke tests.
- Keep provider health checks low-cost and safe.

## Likely Files

- `.github/workflows/deploy.yml`
- `scripts/` or `docker/scripts/`
- health endpoints/config docs

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 8.3

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
