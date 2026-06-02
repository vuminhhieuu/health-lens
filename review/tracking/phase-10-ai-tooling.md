# Phase 10 — AI Tooling Consolidation

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 10](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 1h (parallel)
> **Goal**: Giảm 6 dirs skills xuống tối đa 2.

---

## 📊 Status

- **Status**: ✅ Done
- **Branch**: `chore/10-ai-tooling`
- **PR(s)**: pending
- **Base**: local `dev` at `b4bb98a`
- **Outcome**: Audit/cleanup completed. No app code changed. No AI tooling dirs exist in the current worktree/tracked tree, so destructive consolidation was not needed.

---

## ✅ Pre-flight Checklist

- [x] Đã đọc `repo_hygiene_audit.md` §3
  - Result: Phase 10 target list confirmed from this checklist: `.agent`, `.agents`, `.codex`, `.cursor`, `.opencode`, `.github/skills`.
- [x] Team poll: dev đang dùng IDE/AI tool gì?
  - Skipped with reason: no team poll data is available in repo artifacts; local worktree has zero AI tooling dirs from the target set, so no keep/remove choice is needed for this branch.

---

## 🎯 Tasks

### 10.1 Team Audit (15m)
- [x] Survey: Cursor / GH Copilot / Codex / OpenCode / Antigravity?
  - Skipped with reason: no `.agent`, `.agents`, `.codex`, `.cursor`, `.opencode`, or `.github/skills` directory exists in filesystem or tracked tree.
- [x] Decide: keep MAX 2 export
  - Decision: keep 0 AI tooling export dirs from the audit set. This satisfies the max-2 rule.
- [x] Log decision
  - Logged in this file under Retrospective.

### 10.2 Cleanup .opencode (10m)
- [x] `rm -rf .opencode/node_modules` (26 MB local — gitignored)
  - Skipped with reason: `.opencode/node_modules` does not exist.
- [x] Verify `.opencode/package.json` cần?
  - Skipped with reason: `.opencode/` does not exist.
- [x] Update `.gitignore`
  - Skipped with reason: no `.opencode` artifacts exist, so there is no new ignore rule to add.

### 10.3 Disable Unused IDE Exports (30m)
- [x] Open `_bmad/_config/ides/`
  - Skipped with reason: `_bmad/_config/ides/` is not present in this worktree.
- [x] Remove/disable config cho IDE không dùng
  - Skipped with reason: no IDE export config directory exists to modify.
- [x] Re-run `bmad export <kept-ide>`
  - Skipped with reason: no kept IDE export target exists in repo; running export would create new tooling artifacts, contrary to cleanup scope.

### 10.4 `.github/skills/` Audit (30m)
- [x] 66 tracked skills — verify dùng GH Copilot?
  - Skipped with reason: `.github/skills/` is not present and no files under that path are tracked.
- [x] If unused: `git rm -r .github/skills` + .gitignore
  - Skipped with reason: no tracked `.github/skills` path exists to remove.
- [x] If used: leave + document CONTRIBUTING.md
  - Skipped with reason: no `.github/skills` path exists to preserve or document.

### Buffer (15m)
- [x] Verify clone size giảm
  - Skipped with reason: no cleanup deletion occurred because target directories were absent. There is no size delta to measure.

---

## 🧪 Verification

```bash
# Disk usage
du -sh .agent .agents .codex .cursor .opencode .github/skills 2>/dev/null

# Clone size
cd /tmp && git clone $REPO test-clone && du -sh test-clone
```

Executed equivalent audit checks:

```bash
find . -maxdepth 3 \( -path './.git' -o -path './.git/*' \) -prune -o \
  \( -name '.agent' -o -name '.agents' -o -name '.codex' -o -name '.cursor' -o -name '.opencode' -o -path './.github/skills' \) -print

git ls-files | rg '(^|/)(\.agent|\.agents|\.codex|\.cursor|\.opencode|\.github/skills)(/|$)' || true

test -d .opencode/node_modules && find .opencode/node_modules -maxdepth 2 -print | sed -n '1,40p' || true
```

Result: all commands produced no target directories/files.

---

## 🏁 Phase Completion Checklist

- [x] 4 tasks `[x]`
- [x] Max 2 AI tool dirs
- [x] `.opencode/node_modules` deleted
  - Completed as not applicable: path absent.
- [x] `.github/skills/` decision logged
  - Decision: skipped because path absent/untracked.
- [x] Clone size verified
  - Completed as not applicable: no target dirs existed to delete, so no clone-size reduction is expected.
- [x] Update DASHBOARD

---

## 📝 Retrospective

| Tool dirs | Before | After |
|-----------|:-:|:-:|
| Total | 0 found in worktree | 0 |
| Tracked | 0 found in git index | 0 |
| Local MB | 0 MB target cleanup | 0 MB target cleanup |

**Phase 10 status**: Done. The original consolidation goal is satisfied by absence of the target AI tooling directories; no app code or unrelated tracking files were changed.
