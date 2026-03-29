#!/usr/bin/env python3
"""Generate Linear-style CSV from BMad story markdown files (see tools/README or run -h)."""
from __future__ import annotations

import argparse
import csv
import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
ARTIFACTS = REPO_ROOT / "_bmad-output" / "implementation-artifacts"
TEMPLATE_CSV = REPO_ROOT / "Export Sat Mar 28 2026.csv"
DEFAULT_OUT = REPO_ROOT / "Export Linear user stories import.csv"


def load_template_row() -> dict[str, str]:
    with TEMPLATE_CSV.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        return next(reader)


def story_paths() -> list[Path]:
    files = list(ARTIFACTS.glob("epic-*/*.md"))

    def sort_key(p: Path) -> tuple:
        em = re.search(r"epic-(\d+)", p.as_posix())
        epic = int(em.group(1)) if em else 999
        nm = re.match(r"(\d+)-(\d+)-", p.name)
        if nm:
            return (epic, int(nm.group(1)), int(nm.group(2)), p.name)
        return (epic, 999, 999, p.name)

    return sorted(files, key=sort_key)


def epic_num(path: Path) -> int:
    m = re.search(r"epic-(\d+)", path.as_posix())
    return int(m.group(1)) if m else 0


def main() -> None:
    ap = argparse.ArgumentParser(description="Generate Linear import CSV from story .md files.")
    ap.add_argument(
        "--id-prefix",
        default="LIN",
        help="Issue ID prefix (default LIN -> LIN-1, LIN-2, ...)",
    )
    ap.add_argument(
        "--start-number",
        type=int,
        default=1,
        help="First numeric suffix in ID column (default 1)",
    )
    ap.add_argument(
        "--priority",
        default="No priority",
        help=(
            "CSV Priority column. Linear UI maps: No priority=0, Urgent=1, High=2, Medium=3, Low=4. "
            "Use the label text (e.g. Medium) or a number if your export/import expects it (default: No priority)."
        ),
    )
    ap.add_argument(
        "--labels",
        default="Feature",
        help="Comma-separated team labels (default: Feature). Example: Feature,docs",
    )
    ap.add_argument(
        "--append-epic-label",
        action="store_true",
        help='Append a per-row label "Epic {N}" after --labels (e.g. Feature,Epic 1).',
    )
    ap.add_argument("-o", "--output", type=Path, default=DEFAULT_OUT)
    args = ap.parse_args()

    template = load_template_row()
    headers = list(template.keys())

    epic1_project_id = template.get("Project ID", "")

    base_labels = ",".join(s.strip() for s in args.labels.split(",") if s.strip())

    rows: list[dict[str, str]] = []
    n = args.start_number
    prefix = args.id_prefix.strip() or "LIN"
    for path in story_paths():
        e = epic_num(path)
        body = path.read_text(encoding="utf-8")
        labels = base_labels
        if args.append_epic_label:
            epic_label = f"Epic {e}"
            labels = f"{labels},{epic_label}" if labels else epic_label
        row = {k: template.get(k, "") for k in headers}
        row["ID"] = f"{prefix}-{n}"
        row["Team"] = template.get("Team", "")
        row["Title"] = path.stem
        row["Description"] = body
        row["Status"] = "Backlog"
        row["Estimate"] = template.get("Estimate", "")
        row["Priority"] = args.priority.strip() or template.get("Priority", "")
        row["Project"] = f"Epic {e}"
        row["Project ID"] = epic1_project_id if e == 1 else ""
        row["Creator"] = template.get("Creator", "")
        row["Assignee"] = ""
        row["Labels"] = labels
        row["Cycle Number"] = ""
        row["Cycle Name"] = ""
        row["Cycle Start"] = ""
        row["Cycle End"] = ""
        row["Created"] = template.get("Created", "")
        row["Updated"] = template.get("Updated", "")
        row["Started"] = ""
        row["Triaged"] = ""
        row["Completed"] = ""
        row["Canceled"] = ""
        row["Archived"] = ""
        row["Due Date"] = ""
        row["Parent issue"] = ""
        row["Initiatives"] = ""
        row["Project Milestone ID"] = ""
        row["Project Milestone"] = ""
        row["SLA Status"] = ""
        row["UUID"] = ""
        row["Time in status (minutes)"] = ""
        row["Related to"] = ""
        row["Blocked by"] = ""
        row["Duplicate of"] = ""
        rows.append(row)
        n += 1

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=headers, quoting=csv.QUOTE_MINIMAL)
        w.writeheader()
        w.writerows(rows)

    last = n - 1 if rows else args.start_number - 1
    print(f"Wrote {len(rows)} rows to {args.output} ({prefix}-{args.start_number} .. {prefix}-{last})")


if __name__ == "__main__":
    main()
