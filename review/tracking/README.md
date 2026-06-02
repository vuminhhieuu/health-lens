# 📋 HealthLens — Tracking System

> Hệ thống tracking cho [`INTEGRATED_MASTER_PLAN.md`](../INTEGRATED_MASTER_PLAN.md). Mục đích: **không quên item nào sau mỗi phase**.

---

## 🗂️ Cấu trúc

| File | Mục đích | Khi nào đụng |
|------|----------|--------------|
| `DASHBOARD.md` | At-a-glance status 11 phase + blockers | Đầu mỗi ngày |
| `DECISIONS.md` | ADR log các decision gate | Mỗi khi chốt decision |
| `RESOURCES.md` | Deps, doc links, snippets, commands | Lookup nhanh |
| `phase-NN-name.md` | Detailed checklist + sign-off cho 1 phase | Khi execute phase đó |

---

## 🔄 Workflow chuẩn 1 phase

```
1. Đọc plan      → INTEGRATED_MASTER_PLAN.md → Phase tương ứng
2. Mở tracking   → review/tracking/phase-NN-*.md
3. Pre-flight    → check pre-flight checklist
4. Execute       → check off từng task khi xong (PR link, effort actual)
5. Verify        → chạy verification commands
6. Sign-off      → tick completion checklist
7. Update DASHBOARD → phase 🟢 Done + end date + PR links
8. Retrospective → ghi note: gì học được
```

---

## 📝 Convention

### Status emoji
- 🔴 Not Started / Blocked
- 🟡 In Progress
- 🟢 Done / Verified
- ⚠️ Has Issues
- 🚫 Skipped (với lý do)

### Checkbox states
- `- [ ]` chưa làm
- `- [x]` xong + verified
- `- [!]` blocked — ghi lý do bên cạnh

### Effort tracking
```markdown
- **Estimate**: 2h
- **Actual**: 3.5h
- **Variance**: +1.5h (lý do: ...)
```

---

## 🎯 Quy tắc vàng

1. KHÔNG move sang phase tiếp nếu Decision Gate chưa pass
2. KHÔNG check `[x]` nếu chưa verify (test + smoke)
3. KHÔNG xóa task kể cả skip — đổi thành `🚫 SKIPPED: <lý do>`
4. Mỗi decision → ghi `DECISIONS.md` ngay, kèm rationale
5. Cuối phase update `DASHBOARD.md`

---

## 🆘 Khi stuck

1. Đánh dấu task `- [!] BLOCKED:` + ghi blocker
2. Tạo decision entry trong `DECISIONS.md` nếu cần stakeholder input
3. Update `DASHBOARD.md` → phase ⚠️ Has Issues
4. Tiếp tục các task không bị block
