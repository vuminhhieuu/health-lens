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
- **Status**: 🟡 Proposed
- **Decider(s)**: TBD (ops/PM input)

### Context
`AwsTextractClient.java:15` có TODO chưa implement nhưng `OcrProviderRegistry` có thể route tới. Risk runtime fail.

### Options
1. **A**: Implement Textract — cần AWS account + credentials
2. **B**: Remove khỏi registry — không cho select
3. **C**: Feature flag — chỉ load nếu `OCR_PROVIDER_TEXTRACT_ENABLED=true`

### Decision
<TBD — đề xuất B nếu AWS chưa sẵn>

### Related
- Task: phase-00 / 0.B.2
- Code: `apps/api/src/main/java/com/healthlens/api/service/AwsTextractClient.java:15`

---

## ADR-002: Soft Delete Strategy

- **Date**: TBD
- **Phase**: 0
- **Status**: 🟡 Proposed
- **Decider(s)**: Backend lead

### Context
3 repository methods leak soft-deleted records.

### Options
1. **A — `@SQLDelete` + `@Where`**: Auto filter mọi query JPA. 30m.
2. **B — Manual filter từng method**: Sửa từng repo. 1h. Admin có thể view deleted.

### Decision
<TBD>

### Related
- Task: phase-00 / 0.A.2
- Code: `HealthRecord.java:77`, `HealthRecordRepository.java:34,36,38`

---

## ADR-003: Mobile App — Build vs Drop

- **Date**: TBD
- **Phase**: 7A
- **Status**: 🟡 Proposed
- **Decider(s)**: PM + Mobile lead

### Context
`apps/mobile/app/index.tsx` chỉ render placeholder. 54 deps installed.

### Options
1. **A**: Build full — large effort
2. **B**: Drop deps + minimal shell — 1h
3. **C**: Status quo

### Decision
<TBD — phụ thuộc roadmap>

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
