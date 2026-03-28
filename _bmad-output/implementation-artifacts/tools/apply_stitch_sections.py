#!/usr/bin/env python3
"""Insert or refresh ## Stitch sections in story markdown files."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "_data" / "stitch-screens.json"
ARTIFACTS = ROOT

# story path relative to implementation-artifacts -> list of Stitch screen IDs (hex, no prefix)
STORY_SCREENS: dict[str, list[str]] = {
    "epic-1/1-1-starter-template-setup.md": [],
    "epic-1/1-2-email-password-registration.md": ["a3b22ddf15924e8c860ea3239e312044"],
    "epic-1/1-3-secure-login-logout-session.md": [
        "09409d972eaa45f592c396b6d14469aa",
        "010095343e6c46b4969b42c4ab93165a",
    ],
    "epic-1/1-4-password-reset-via-email.md": [
        "f1b10549c80e4f98ab45fa121a50f009",
        "c8952e57aec54ba0aa5da4bdd913cddb",
    ],
    "epic-1/1-5-sensitive-health-data-consent.md": ["fb972f816f334c73b426f17cd20bd58d"],
    "epic-1/1-6-account-data-deletion-request.md": [
        "fc0abf8742984269b5a25a0e0ab20782",
        "f6198b62522b49d88bd34fad6c9cee84",
    ],
    "epic-2/2-1-update-account-personal-info.md": [
        "fc0abf8742984269b5a25a0e0ab20782",
        "f6198b62522b49d88bd34fad6c9cee84",
    ],
    "epic-2/2-2-create-health-profile.md": ["68716ba836ae433e82006a112d724e12"],
    "epic-2/2-3-profile-name-and-notes.md": ["68716ba836ae433e82006a112d724e12"],
    "epic-3/3-1-upload-pdf-image-library.md": ["0e048c6789e64ceab2f78815dac26939"],
    "epic-3/3-3-ocr-extract-and-review-list.md": [
        "661b65268f614bacab4dee0690eb1790",
        "a9f6ae144fa7402bac66d0c56b36f4c0",
    ],
    "epic-3/3-4-manual-edit-input-ocr-fallback.md": ["1ba2a148df7d427d89fdaaf223deaafd"],
    "epic-3/3-5-ocr-failure-recovery-flow.md": ["ace87425151148448083728c199470c7"],
    "epic-3/3-6-metric-source-tag-and-profile-link.md": ["3c9f3f9c951b4e45aa713c6da552be29"],
    "epic-4/4-1-metric-card-status-reference-range.md": ["3c9f3f9c951b4e45aa713c6da552be29"],
    "epic-4/4-2-age-gender-context-reference-range.md": ["3c9f3f9c951b4e45aa713c6da552be29"],
    "epic-4/4-3-simple-vietnamese-metric-explanations.md": ["3c9f3f9c951b4e45aa713c6da552be29"],
    "epic-4/4-4-lifestyle-recommendations-with-disclaimer.md": ["3c9f3f9c951b4e45aa713c6da552be29"],
    "epic-4/4-5-highlight-alerts-progressive-disclosure.md": ["3c9f3f9c951b4e45aa713c6da552be29"],
    "epic-5/5-1-profile-history-timeline.md": [
        "010095343e6c46b4969b42c4ab93165a",
        "2f0902b360ee4d9fb7be1cb8d51c2640",
    ],
    "epic-5/5-2-open-history-result-details.md": ["3c9f3f9c951b4e45aa713c6da552be29"],
    "epic-5/5-3-delete-health-result-record.md": ["3c9f3f9c951b4e45aa713c6da552be29"],
    "epic-6/6-1-invite-family-member-by-email.md": [
        "6c7c4797727240cda249483e8307e4cf",
        "4fa649c74ee4431fb2ad96bc3550ebd8",
    ],
    "epic-6/6-2-shared-profile-web-view-access.md": ["32cf2c3670374324b90b464766cf8872"],
    "epic-6/6-3-revoke-family-access-anytime.md": ["6c7c4797727240cda249483e8307e4cf"],
    "epic-6/6-4-web-dashboard-new-result-updates.md": ["010095343e6c46b4969b42c4ab93165a"],
    "epic-7/7-1-admin-login-with-mfa.md": [
        "4624e45abb214f2ea30b2d010918f9f7",
        "07e884d40df14c2b8558038dabda2e56",
        "397d87fb9f5f44d29e6ea84f12d81200",
        "beac3900063d4ced87a9d40ac644025e",
    ],
    "epic-7/7-2-admin-crud-metric-reference-data.md": [
        "bb3084187f0e41d5b4ac0617a52da22c",
        "7c23ef03075048afb2fddab20e12afd6",
    ],
    "epic-7/7-3-import-reference-data-csv-json.md": ["bb3084187f0e41d5b4ac0617a52da22c"],
    "epic-7/7-4-approve-reference-data-change-set.md": ["1704331f19474eb9b8345ee6bb68c27f"],
    "epic-7/7-5-reference-data-audit-log-view.md": ["d0690e9e60b74787b60020aaadd6c0e4"],
    "epic-8/8-1-dashboard-user-growth-overview.md": ["1cfbb9fe88734e629a044add27adf1f1"],
    "epic-8/8-2-dashboard-wau-upload-volume.md": ["1cfbb9fe88734e629a044add27adf1f1"],
    "epic-8/8-3-dashboard-upload-success-failure-rate.md": ["1cfbb9fe88734e629a044add27adf1f1"],
    "epic-9/9-1-mobile-camera-capture-ux.md": [],
    "epic-9/9-2-mobile-offline-readonly-history.md": [],
    "epic-9/9-3-auto-sync-when-back-online.md": [],
    "infrastructure/7-1-initialize-monorepo-structure.md": [],
    "infrastructure/7-2-setup-github-pages-jekyll.md": [],
    "infrastructure/7-3-create-internal-documentation-structure.md": [],
    "infrastructure/7-4-create-docker-development-stack.md": [],
    "infrastructure/7-5-setup-github-actions-cicd.md": [],
}

PROJECT_ID = "578519912546445367"
STITCH_SECTION_START = "## Stitch"
NOTE_EXPIRE = (
    "*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. "
    "Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) "
    "rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này."
)


def screen_id_from_name(name: str) -> str:
    return name.split("/")[-1]


def load_screen_index() -> dict[str, dict]:
    raw = json.loads(DATA.read_text(encoding="utf-8"))
    by_id: dict[str, dict] = {}
    for s in raw["screens"]:
        sid = screen_id_from_name(s["name"])
        html = (s.get("htmlCode") or {}).get("downloadUrl") or ""
        shot = (s.get("screenshot") or {}).get("downloadUrl") or ""
        by_id[sid] = {"title": s.get("title") or sid, "html": html, "screenshot": shot}
    return by_id


def build_stitch_section(ids: list[str], by_id: dict[str, dict], mobile_note: str | None) -> str:
    lines = [
        STITCH_SECTION_START + " — giao diện tham chiếu (Google Stitch)",
        "",
        f"- **Dự án:** HealthLens-Web-MVP — `projectId`: `{PROJECT_ID}`",
        "- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)",
        "",
    ]
    if mobile_note:
        lines.extend([mobile_note, ""])
    if not ids:
        lines.append("Không có screen Stitch tương ứng trong project Web MVP này (story kỹ thuật / mobile phase 2).")
        lines.extend(["", NOTE_EXPIRE, ""])
        return "\n".join(lines)

    lines.append("| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |")
    lines.append("|---|---|---|---|")
    for sid in ids:
        info = by_id.get(sid)
        if not info:
            lines.append(f"| *(thiếu trong snapshot)* | `{sid}` | — | — |")
            continue
        title = info["title"].replace("|", "\\|")
        res = f"`projects/{PROJECT_ID}/screens/{sid}`"
        hp = f"[Mở]({info['html']})" if info["html"] else "—"
        sp = f"[Xem]({info['screenshot']})" if info["screenshot"] else "—"
        lines.append(f"| {title} | {res} | {hp} | {sp} |")
    lines.extend(["", NOTE_EXPIRE, ""])
    return "\n".join(lines) + "\n"


def strip_stitch_section(content: str) -> str:
    # Dừng trước mục markdown ## tiếp theo (Story, Tasks, …) — không dùng \Z (sẽ nuốt cả file nếu thiếu ## sau Stitch).
    return re.sub(
        r"^## Stitch — giao diện tham chiếu \(Google Stitch\).*?(?=^## (?!Stitch — giao diện))",
        "",
        content,
        count=1,
        flags=re.MULTILINE | re.DOTALL,
    )


def replace_or_insert_stitch(content: str, section: str) -> str:
    content = strip_stitch_section(content)
    # Đặt Stitch sau dòng Status (chuẩn create-story)
    m = re.search(r"^(# [^\n]+\n\n)(Status:[^\n]+\n\n)", content, re.MULTILINE)
    if m:
        idx = m.end()
        return content[:idx] + section.rstrip() + "\n\n" + content[idx:]
    m2 = re.search(r"^(# [^\n]+\n\n)", content, re.MULTILINE)
    if m2:
        idx = m2.end()
        return content[:idx] + section.rstrip() + "\n\n" + content[idx:]
    return section.rstrip() + "\n\n" + content


def write_stitch_index(by_id: dict[str, dict]) -> None:
    path = ARTIFACTS / "STITCH-SCREEN-LINKS.md"
    lines = [
        "# Stitch — liên kết màn hình (HealthLens Web MVP)",
        "",
        f"**Project ID:** `{PROJECT_ID}`",
        "",
        "**Giao diện Stitch (duyệt project):** [stitch.withgoogle.com](https://stitch.withgoogle.com/) — mở project HealthLens-Web-MVP trong workspace của bạn.",
        "",
        "Bảng dưới đồng bộ với snapshot `_data/stitch-screens.json` (cập nhật qua MCP `list_screens`).",
        "",
        "| Stitch ID | Tiêu đề | HTML prototype | Screenshot |",
        "|---|---|---|---|",
    ]
    for sid in sorted(by_id.keys()):
        info = by_id[sid]
        t = info["title"].replace("|", "\\|")
        hp = f"[Mở]({info['html']})" if info["html"] else "—"
        sp = f"[Xem]({info['screenshot']})" if info["screenshot"] else "—"
        lines.append(f"| `{sid}` | {t} | {hp} | {sp} |")
    lines.extend(["", NOTE_EXPIRE, ""])
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    by_id = load_screen_index()
    write_stitch_index(by_id)

    epic9_note = (
        "**Lưu ý:** Epic 9 (mobile) chưa có screen trong project Stitch Web MVP; "
        "dùng cùng luồng auth/upload ở web như tham chiếu UX hoặc bổ sung project Stitch riêng sau."
    )

    for rel, ids in sorted(STORY_SCREENS.items()):
        path = ARTIFACTS / rel
        if not path.is_file():
            print("skip missing", rel)
            continue
        mobile = epic9_note if rel.startswith("epic-9/") else None
        section = build_stitch_section(ids, by_id, mobile)
        text = path.read_text(encoding="utf-8")
        path.write_text(replace_or_insert_stitch(text, section), encoding="utf-8")
        print("updated", rel)

    print("done")


if __name__ == "__main__":
    main()
