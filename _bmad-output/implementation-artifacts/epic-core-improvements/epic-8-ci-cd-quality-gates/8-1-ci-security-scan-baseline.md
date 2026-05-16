# Story 8.1: CI Security Scan Baseline

Status: ready-for-dev

## Execution Scope

**Area:** CI/CD security scans, dependency/SAST/container/secret scanning  
**Priority:** P1

## Story

As a release owner, I want dependency, SAST, container, and secret scans in CI, so that high-risk issues are caught before deployment.

## Acceptance Criteria

1. **Given** CI runs on pull request, **When** scans complete, **Then** results are visible in checks.
2. **Given** critical vulnerability is detected, **When** severity gate evaluates, **Then** CI fails.
3. **Given** Docker image is built, **When** image scan runs, **Then** critical/high vulnerabilities are reported.
4. **Given** a secret-like value is committed, **When** secret scan runs, **Then** CI blocks or reports according to gate policy.

## Tasks / Subtasks

- [ ] Add dependency scanning for Java/Node/Python.
- [ ] Add SAST scan for Java/TypeScript.
- [ ] Add Docker image scan for API/Web/OCR images.
- [ ] Add secret scanning.
- [ ] Configure severity gates and report artifacts.

## Dev Notes

- This supersedes old story `epic-10/10-9-ci-cd-security-integration.md`.
- Choose tools pragmatically; avoid requiring paid services unless tokens are available.

## Likely Files

- `.github/workflows/ci.yml`
- `.github/workflows/deploy.yml`
- Dockerfiles

## References

- Old source: `_bmad-output/implementation-artifacts/epic-10/10-9-ci-cd-security-integration.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 8.1

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
