# Story 4.2: Observability Metrics And Alerting Baseline

Status: ready-for-dev

## Execution Scope

**Area:** Metrics, dashboards, alerts, structured logs  
**Priority:** P1

## Story

As an operator, I want dashboards and alerts for OCR/LLM/RAG/API health, so that failures are visible before users report them.

## Acceptance Criteria

1. **Given** OCR failure rate exceeds threshold, **When** alert evaluation runs, **Then** alert includes provider and failure reason.
2. **Given** RAG hit rate drops unexpectedly, **When** dashboard is viewed, **Then** operator sees source, hit/miss, top score, and latency trends.
3. **Given** LLM schema failures increase, **When** metrics are inspected, **Then** invalid schema rate, retry count, and model are visible.
4. **Given** API error rate or latency breaches threshold, **When** alerting runs, **Then** incident signal is emitted.

## Tasks / Subtasks

- [ ] Add OCR provider success/fail/latency metrics.
- [ ] Add LLM latency/token/schema outcome metrics.
- [ ] Add RAG hit/miss/score/latency metrics.
- [ ] Add dashboards and alert rules.
- [ ] Add runbook links for alert responses.

## Dev Notes

- This is not the same as product analytics event tracking.
- It may depend on event foundation from Story 4.1 and 4.4/old 8.4 where useful.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/*`
- Observability docs/config under `docs/` or deployment config

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 4.2
- Old related story: `_bmad-output/implementation-artifacts/epic-8/8-4-persisted-product-event-tracking-foundation.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
