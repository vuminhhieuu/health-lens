# Story 2.1: Generic AI Chat Provider Configuration

Status: done

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

- [x] Add `AI_CHAT_PROVIDER`, `AI_CHAT_BASE_URL`, `AI_CHAT_API_KEY`, `AI_CHAT_MODEL`, `AI_CHAT_TIMEOUT_MS`.
- [x] Rename/deprecate Groq-specific config without breaking local dev unexpectedly.
- [x] Add production startup validation.
- [x] Update `.env.example`, `application.yml`, and docs.
- [x] Add tests for missing config, legacy config, and unsupported provider.

### Review Findings

- [x] [Review][Patch] High: Staging/non-production deploys can boot with `dev-placeholder` chat API key [apps/api/src/main/resources/application.yml:21]
- [x] [Review][Patch] Medium: `AI_CHAT_TIMEOUT_MS` is documented and mapped but not applied or validated [apps/api/src/main/java/com/healthlens/api/config/AiChatProviderEnvironmentPostProcessor.java:54]
- [x] [Review][Patch] Medium: Post-processor defaults override direct Spring AI properties in non-production [apps/api/src/main/java/com/healthlens/api/config/AiChatProviderEnvironmentPostProcessor.java:57]
- [x] [Review][Patch] Low: No test proves Spring Boot discovers the environment post-processor registration [apps/api/src/main/resources/META-INF/spring.factories:1]

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

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests com.healthlens.api.config.AiChatProviderEnvironmentPostProcessorTest`
- `cd apps/api && ./gradlew test --tests com.healthlens.api.config.AiChatProviderEnvironmentPostProcessorTest --tests com.healthlens.api.service.LlmServiceTest`
- `cd apps/api && ./gradlew test`

### Completion Notes List

- Đã thêm bridge cấu hình `AI_CHAT_*` ưu tiên generic env, map sang Spring AI OpenAI-compatible properties, và fallback legacy `GROQ_*` kèm warning khi generic vắng mặt.
- Đã thay `GroqAiConfig` bằng `AiChatConfig`, đổi bean/field/log trong `LlmService` sang provider-neutral naming, giữ local/test không vỡ bằng `dev-placeholder` ngoài production.
- Đã thêm fail-fast cho production khi thiếu `AI_CHAT_API_KEY`, `AI_CHAT_BASE_URL`, hoặc `AI_CHAT_MODEL`; provider native như `anthropic` fail rõ vì chưa có adapter và không hỗ trợ env-only switching.
- Đã cập nhật `.env.example`, `application.yml`, `application-docker.yml`, và docs chính cho migration policy.
- Đã chạy full API regression suite thành công.
- Đã rà soát lại repo và thay các chỗ tài liệu/script còn hướng dẫn dùng Groq như biến chính sang `AI_CHAT_*`; các `GROQ_*` còn lại chỉ phục vụ fallback legacy/test migration.
- Đã xử lý review findings: staging/prod fail-fast, timeout được validate và áp dụng vào RestClient, direct Spring AI properties không bị default override, và thêm test cho `spring.factories` registration.

### File List

- `.env.example`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-2-ai-provider-abstraction/2-1-generic-ai-chat-provider-configuration.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `apps/api/src/main/java/com/healthlens/api/config/AiChatConfig.java`
- `apps/api/src/main/java/com/healthlens/api/config/AiChatProviderEnvironmentPostProcessor.java`
- `apps/api/src/main/java/com/healthlens/api/config/GroqAiConfig.java` (deleted)
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/main/resources/META-INF/spring.factories`
- `apps/api/src/main/resources/application-docker.yml`
- `apps/api/src/main/resources/application.yml`
- `apps/api/build.gradle.kts`
- `docker/scripts/up.sh`
- `README.md`
- `docs/architecture.md`
- `docs/deployment-guide.md`
- `docs/project-overview.md`
- `apps/api/src/test/java/com/healthlens/api/config/AiChatProviderEnvironmentPostProcessorTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`
- `docs/STAGING_DEPLOYMENT.md`
- `docs/development-guide.md`
- `docs/environment-reference.md`

### Change Log

- 2026-05-17: Implemented generic AI chat provider configuration, legacy Groq env migration fallback, production validation, docs, and tests.
- 2026-05-17: Follow-up sweep replaced remaining deployment/documentation prompts that treated Groq env names as primary.
- 2026-05-17: Addressed code review findings and moved story to done.
