# Story 2.1: Generic AI Chat Provider Configuration

Status: ready-for-dev

## Execution Scope

**Area:** Backend AI config, env contract, provider-neutral naming  
**Priority:** P1

## Story

As a deployer, I want LLM provider configuration to use generic AI names instead of Groq-specific names, so that switching OpenAI-compatible providers is clear and low-risk.

## Acceptance Criteria

1. **Given** an OpenAI-compatible provider is configured, **When** the app starts, **Then** chat client uses generic `AI_CHAT_*` env keys.
2. **Given** required API key/model/base URL is missing, **When** production profile starts, **Then** startup fails with a clear configuration error.
3. **Given** legacy `GROQ_*` env exists, **When** generic env is absent, **Then** app maps legacy values with a warning or fails according to documented migration policy.
4. **Given** a native non-OpenAI-compatible provider is requested, **When** no adapter exists, **Then** startup fails and explains that env-only switching is unsupported.

## Tasks / Subtasks

- [ ] Add `AI_CHAT_PROVIDER`, `AI_CHAT_BASE_URL`, `AI_CHAT_API_KEY`, `AI_CHAT_MODEL`, `AI_CHAT_TIMEOUT_MS`.
- [ ] Rename/deprecate Groq-specific config without breaking local dev unexpectedly.
- [ ] Add production startup validation.
- [ ] Update `.env.example`, `application.yml`, and docs.
- [ ] Add tests for missing config, legacy config, and unsupported provider.

## Dev Notes

- Do not claim every provider is env-only switchable. Only OpenAI-compatible providers can share the same client path.
- Avoid provider-specific bean names leaking into core services.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/config/GroqAiConfig.java`
- `apps/api/src/main/resources/application.yml`
- `apps/api/src/main/resources/application-docker.yml`
- `.env.example`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 2.1
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 12 and 14

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
