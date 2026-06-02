# 🗳️ HealthLens Refactor — Decisions Log

> ADR-style log. Mỗi decision gate ghi lại với rationale + approver.

---

## Format

```markdown
## ADR-NNN: <Tên ngắn>

- **Date**: YYYY-MM-DD
- **Phase**: 0 / 1 / ...
- **Status**: 🟢 Accepted | 🟡 Proposed | 🔴 Rejected | ⚠️ Superseded
- **Decider(s)**: @username (role)

### Context
<Vấn đề>

### Options
1. **A**: ... — pros/cons
2. **B**: ... — pros/cons

### Decision
<Chốt + tại sao>

### Consequences
- ✅ ...
- ⚠️ ...

### Related
- Task: phase-NN / task X.Y
- Code: file:line
```

---

## ADR-001: AwsTextractClient — Fix vs Remove

- **Date**: TBD
- **Phase**: 0
- **Status**: ✅ Accepted
- **Decider(s)**: Codex implementation pass, pending human review

### Context
`AwsTextractClient.java:15` có TODO chưa implement nhưng `OcrProviderRegistry` có thể route tới. Risk runtime fail.

### Options
1. **A**: Implement Textract — cần AWS account + credentials
2. **B**: Remove khỏi registry — không cho select
3. **C**: Feature flag — chỉ load nếu `OCR_PROVIDER_TEXTRACT_ENABLED=true`

### Decision
Option **B — Remove khỏi registry**.

AWS credentials and a real Textract implementation are not available in this phase. Keeping a selectable stub is a production runtime risk, so Phase 0 removes Textract from provider configuration and deletes the stub provider/client.

### Related
- Task: phase-00 / 0.B.2
- Code: `apps/api/src/main/java/com/healthlens/api/service/AwsTextractClient.java:15`

---

## ADR-002: Soft Delete Strategy

- **Date**: 2026-06-02
- **Phase**: 0
- **Status**: ✅ Accepted
- **Decider(s)**: Codex implementation pass, pending human review

### Context
3 repository methods leak soft-deleted records.

### Options
1. **A — `@SQLDelete` + global filter**: Auto filter mọi query JPA. 30m.
2. **B — Explicit repository filters**: Sửa từng repo leak method. 1h. Purge/right-to-delete vẫn có thể thấy soft-deleted rows.

### Decision
Option **B — Explicit repository filters**, while keeping `@SQLDelete` for entity-level soft delete.

Global Hibernate filtering can hide rows from purge/right-to-delete cleanup. Phase 0 fixes the three known leak vectors by adding `deleted_at IS NULL` to `findAllByProfileIdAndUserId`, `findAllByUserId`, and `findFileKeysByUserId`, while bulk `deleteAllByUserId` remains able to hard-delete soft-deleted rows during account deletion.

### Related
- Task: phase-00 / 0.A.2
- Code: `HealthRecord.java:77`, `HealthRecordRepository.java:34,36,38`

---

## ADR-003: Mobile App — Build vs Drop

- **Date**: 2026-06-02
- **Phase**: 0 cleanup / supersedes 7A
- **Status**: ✅ Accepted
- **Decider(s)**: Review finding follow-up

### Context
`apps/mobile/app/index.tsx` chỉ render placeholder. 54 deps installed.

### Options
1. **A**: Build full — large effort
2. **B**: Drop deps + minimal shell — 1h
3. **C**: Status quo

### Decision
Drop mobile as an active workspace module for now. Remove `apps/mobile`, remove workspace/CI references, and do not run mobile build/lint/test in Phase 0.

### Related
- Task: phase-07 / 7A.1

---

## ADR-004: Data Retention Policy

- **Date**: TBD
- **Phase**: 8.10
- **Status**: 🟡 Proposed
- **Decider(s)**: PM + Legal/Compliance

### Context
Không có retention policy. Health records 5+ năm vẫn còn.

### Options
1. **A**: Auto-delete sau N năm (5/7)
2. **B**: User-controlled only
3. **C**: Tiered (active vs archive)

### Decision
<TBD — phụ thuộc compliance VN>

### Related
- Task: phase-08 / 8.10

---

<!-- Thêm ADR mới ở dưới khi có decision -->
