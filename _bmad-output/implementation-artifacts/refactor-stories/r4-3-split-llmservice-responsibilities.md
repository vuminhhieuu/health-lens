# Story R4.3: Split LlmService Responsibilities

Status: ready-for-dev

## Story

Là AI feature developer,
tôi muốn tách prompt rendering, provider calls, parsing, fallback, và caching,
để LLM behavior tiến hóa mà không làm bất ổn health-record workflows.

## Acceptance Criteria

1. **Given** `LlmService` trộn nhiều responsibility **When** decomposition hoàn tất **Then** mỗi responsibility có collaborator focused và failure behavior rõ.
2. **Given** provider failures/parsing/fallback quan trọng **When** tests chạy **Then** các paths này được cover.
3. **Given** prompts đang dùng production behavior **When** split hoàn tất **Then** prompt content và disclaimer behavior không đổi.

## Tasks / Subtasks

- [ ] Map current LLM responsibilities. (AC: 1)
- [ ] Extract prompt assembly/rendering, provider gateway, parser, fallback policy. (AC: 1)
- [ ] Update tests for provider failure/parsing/fallback. (AC: 2)
- [ ] Verify prompt resource loading. (AC: 3)

## Dev Notes

- Do not rewrite prompts in this story.
- Medical disclaimer behavior must remain.

### Project Structure Notes

- Relevant file: `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`.

### References

- `docs/llm-prompt-templates.md`
- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

