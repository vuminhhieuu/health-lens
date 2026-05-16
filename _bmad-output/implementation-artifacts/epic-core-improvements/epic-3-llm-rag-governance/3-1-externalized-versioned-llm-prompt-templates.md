# Story 3.1: Externalized Versioned LLM Prompt Templates

Status: ready-for-dev

## Execution Scope

**Area:** Backend LLM prompt resources, prompt versioning, tests  
**Priority:** P1

## Story

As an AI feature maintainer, I want prompts stored as versioned templates outside large Java strings, so that prompt behavior can be reviewed, tested, and rolled back.

## Acceptance Criteria

1. **Given** a recommendation is generated, **When** AI metadata is persisted/logged, **Then** prompt version and model version are available for audit.
2. **Given** a prompt template is edited, **When** tests run, **Then** required medical disclaimer and no-invent-reference-ranges instruction are verified.
3. **Given** prompt rendering fails, **When** API handles request, **Then** system returns safe fallback and records a failure metric.

## Tasks / Subtasks

- [ ] Move prompts from Java inline strings to resource templates.
- [ ] Add prompt version metadata.
- [ ] Add prompt rendering tests.
- [ ] Record prompt/model version in generated output metadata.
- [ ] Document prompt update/review process.

## Dev Notes

- Do not change user-facing AI behavior without tests capturing current required guardrails.
- Keep templates readable by reviewers who are not Java developers.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/main/resources/ai/`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 3.1
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 9

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
