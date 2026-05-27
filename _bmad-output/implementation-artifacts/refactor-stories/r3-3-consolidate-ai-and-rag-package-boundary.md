# Story R3.3: Consolidate AI And RAG Package Boundary

Status: ready-for-dev

## Story

Là backend developer,
tôi muốn AI/chat/embedding/RAG code được colocate theo AI concerns,
để prompt, provider, retrieval, và corpus code có ownership rõ.

## Acceptance Criteria

1. **Given** AI code đã một phần feature-based **When** migration hoàn tất **Then** AI config/services/entities/repositories/tests nằm trong AI hierarchy hoặc common documented.
2. **Given** prompt/resource paths đang được dùng **When** files/packages đổi **Then** resource loading vẫn đúng.
3. **Given** AI tests tồn tại **When** migration hoàn tất **Then** relevant tests pass hoặc existing failures documented.

## Tasks / Subtasks

- [ ] Inventory AI/RAG classes, configs, resources, tests. (AC: 1)
- [ ] Move package declarations/imports theo rules R3.1. (AC: 1)
- [ ] Verify prompt paths and seed resources. (AC: 2)
- [ ] Run targeted AI/RAG tests. (AC: 3)

## Dev Notes

- Không split `LlmService` trong story này; đó là R4.3.
- Avoid changing prompt content.

### Project Structure Notes

- Relevant resources: `apps/api/src/main/resources/ai`.

### References

- `review/api-package-structure-analysis.md`
- `docs/llm-prompt-templates.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

