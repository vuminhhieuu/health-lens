"use client";

import { useEffect } from "react";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { X } from "lucide-react";

import {
  actionBadgeClass,
  actionLabelVi,
  displayNameFromEmail,
  formatJsonBlock,
  jsonPanelLabel,
  rowDetailText,
  traceIdentifiersForDisplay,
  type AuditLogEntry,
} from "@/lib/admin/auditLog";

export function AuditLogDetailModal({
  entry,
  onClose,
  onTraceFilter,
}: {
  entry: AuditLogEntry;
  onClose: () => void;
  onTraceFilter: (correlationId: string) => void;
}) {
  const at = new Date(entry.createdAt);
  const email = entry.actorEmail?.trim() || "";
  const traceIdentifiers = traceIdentifiersForDisplay(entry);
  const traceCorrelationId = entry.correlationId?.trim() ?? "";

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="audit-detail-title"
      onClick={onClose}
    >
      <div
        className="flex max-h-[90vh] w-full max-w-5xl flex-col overflow-hidden rounded-[2rem] bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between border-b border-[#d8e5e2] px-6 py-5">
          <div>
            <h3 id="audit-detail-title" className="text-xl font-bold text-[#121e1c]">
              Chi tiết thay đổi
            </h3>
            <p className="mt-1 text-sm text-[#3d4947]">
              {rowDetailText(entry)}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 text-[#3d4947] transition hover:bg-[#e9f6f3]"
            aria-label="Đóng"
            title="Đóng"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <div className="flex flex-wrap gap-3 border-b border-[#e9f6f3] bg-[#f8fafc] px-6 py-4 text-xs">
          <div>
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">Thời gian</span>
            <p className="mt-0.5 font-medium text-[#121e1c]">
              {format(at, "HH:mm:ss · dd/MM/yyyy", { locale: vi })}
            </p>
          </div>
          <div className="min-w-[10rem] flex-1">
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">Người thực hiện</span>
            <p className="mt-0.5 font-medium text-[#121e1c]">{displayNameFromEmail(email)}</p>
            <p className="text-[10px] italic text-[#3d4947]">{email || "—"}</p>
          </div>
          <div>
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">Hành động</span>
            <p className="mt-1">
              <span
                className={`inline-block rounded px-2 py-0.5 text-[11px] font-semibold ${actionBadgeClass(entry.action)}`}
              >
                {actionLabelVi(entry.action)}
              </span>
            </p>
          </div>
          <div>
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">Đối tượng</span>
            <p className="mt-0.5 font-medium text-[#121e1c]">{entry.entityLabel || "—"}</p>
          </div>
          <div>
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">IP</span>
            <p className="mt-0.5 font-mono text-[#3d4947]">{entry.ipAddress ?? "—"}</p>
          </div>
        </div>

        <div className="border-b border-[#e9f6f3] bg-white px-6 py-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div className="grid flex-1 gap-3 text-xs sm:grid-cols-3">
              {traceIdentifiers.length === 0 ? (
                <div>
                  <span className="font-bold uppercase tracking-wider text-[#0d9488]">Trace</span>
                  <p className="mt-0.5 text-[#3d4947]">Không có mã trace cho bản ghi này.</p>
                </div>
              ) : (
                traceIdentifiers.map(([label, value]) => (
                  <div key={label} className="min-w-0">
                    <span className="font-bold uppercase tracking-wider text-[#0d9488]">{label}</span>
                    <p className="mt-0.5 truncate font-mono text-[11px] text-[#121e1c]" title={value}>
                      {value}
                    </p>
                  </div>
                ))
              )}
            </div>
            {traceCorrelationId ? (
              <button
                type="button"
                onClick={() => onTraceFilter(traceCorrelationId)}
                className="inline-flex shrink-0 items-center justify-center rounded-lg bg-teal-700 px-3 py-2 text-xs font-bold text-white transition hover:bg-teal-800"
              >
                Xem cùng trace
              </button>
            ) : null}
          </div>
        </div>

        <div className="grid flex-1 gap-4 overflow-hidden p-6 lg:grid-cols-3">
          <div className="flex min-h-0 flex-col rounded-2xl border border-[#d8e5e2] bg-[#f8fafc]">
            <p className="border-b border-[#d8e5e2] bg-white px-4 py-2 text-[11px] font-bold uppercase tracking-wider text-[#3d4947]">
              {jsonPanelLabel(entry.oldValueJson, entry.newValueJson, "before")}
            </p>
            <pre className="hl-custom-scrollbar max-h-[50vh] flex-1 overflow-auto p-4 text-xs leading-relaxed text-[#121e1c]">
              {formatJsonBlock(entry.oldValueJson)}
            </pre>
          </div>
          <div className="flex min-h-0 flex-col rounded-2xl border border-[#89f5e7]/40 bg-[#e9f6f3]/60">
            <p className="border-b border-[#89f5e7]/40 bg-white px-4 py-2 text-[11px] font-bold uppercase tracking-wider text-[#00685f]">
              {jsonPanelLabel(entry.oldValueJson, entry.newValueJson, "after")}
            </p>
            <pre className="hl-custom-scrollbar max-h-[50vh] flex-1 overflow-auto p-4 text-xs leading-relaxed text-[#121e1c]">
              {formatJsonBlock(entry.newValueJson)}
            </pre>
          </div>
          <div className="flex min-h-0 flex-col rounded-2xl border border-slate-200 bg-white">
            <p className="border-b border-slate-200 bg-slate-50 px-4 py-2 text-[11px] font-bold uppercase tracking-wider text-slate-600">
              Metadata an toàn
            </p>
            <pre className="hl-custom-scrollbar max-h-[50vh] flex-1 overflow-auto p-4 text-xs leading-relaxed text-[#121e1c]">
              {formatJsonBlock(entry.metadataJson)}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
}
