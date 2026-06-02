# Phase 10 — AI Tooling Consolidation

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 10](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 1h (parallel)
> **Goal**: Giảm 6 dirs skills xuống tối đa 2.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `chore/10-ai-tooling`
- **PR(s)**: — (1 PR)

---

## ✅ Pre-flight Checklist

- [ ] Đã đọc `repo_hygiene_audit.md` §3
- [ ] Team poll: dev đang dùng IDE/AI tool gì?

---

## 🎯 Tasks

### 10.1 Team Audit (15m)
- [ ] Survey: Cursor / GH Copilot / Codex / OpenCode / Antigravity?
- [ ] Decide: keep MAX 2 export
- [ ] Log decision

### 10.2 Cleanup .opencode (10m)
- [ ] `rm -rf .opencode/node_modules` (26 MB local — gitignored)
- [ ] Verify `.opencode/package.json` cần?
- [ ] Update `.gitignore`

### 10.3 Disable Unused IDE Exports (30m)
- [ ] Open `_bmad/_config/ides/`
- [ ] Remove/disable config cho IDE không dùng
- [ ] Re-run `bmad export <kept-ide>`

### 10.4 `.github/skills/` Audit (30m)
- [ ] 66 tracked skills — verify dùng GH Copilot?
- [ ] If unused: `git rm -r .github/skills` + .gitignore
- [ ] If used: leave + document CONTRIBUTING.md

### Buffer (15m)
- [ ] Verify clone size giảm

---

## 🧪 Verification

```bash
# Disk usage
du -sh .agent .agents .codex .cursor .opencode .github/skills 2>/dev/null

# Clone size
cd /tmp && git clone $REPO test-clone && du -sh test-clone
```

---

## 🏁 Phase Completion Checklist

- [ ] 4 tasks `[x]`
- [ ] Max 2 AI tool dirs
- [ ] `.opencode/node_modules` deleted
- [ ] `.github/skills/` decision logged
- [ ] Clone size verified
- [ ] Update DASHBOARD

---

## 📝 Retrospective

| Tool dirs | Before | After |
|-----------|:-:|:-:|
| Total | 6 | — |
| Tracked | 1 | — |
| Local MB | ~30+ | — |
