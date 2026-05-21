"use client";

import { ClipboardList, Loader2 } from "lucide-react";

import type { AuditViewScope } from "@/lib/admin/auditLog";

export function AuditLogPageHeader({
  viewScope,
  onViewScopeChange,
  exporting,
  exportMessage,
  onExportCsv,
  exportDisabled,
}: {
  viewScope: AuditViewScope;
  onViewScopeChange: (scope: AuditViewScope) => void;
  exporting: boolean;
  exportMessage: string | null;
  onExportCsv: () => void;
  exportDisabled: boolean;
}) {
  return (
    <header className="flex flex-col gap-4 rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200 lg:flex-row lg:items-start lg:justify-between">
      <div className="min-w-0">
        <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-900">
          <ClipboardList className="h-7 w-7 shrink-0 text-teal-600" aria-hidden="true" />
          Nhật ký hoạt động
        </h1>
        <p className="mt-2 max-w-3xl text-sm text-slate-500">
          {viewScope === "reference"
            ? "Truy vết ai đã thay đổi dữ liệu tham chiếu, khi nào và nội dung trước/sau."
            : "Xem toàn bộ hoạt động quản trị và người dùng trên hệ thống."}
        </p>
        <div
          className="mt-4 inline-flex rounded-full bg-slate-100 p-1"
          role="group"
          aria-label="Phạm vi nhật ký"
        >
          <button
            type="button"
            aria-pressed={viewScope === "reference"}
            onClick={() => onViewScopeChange("reference")}
            className={`rounded-full px-4 py-2 text-xs font-semibold transition ${
              viewScope === "reference"
                ? "bg-white text-teal-800 shadow-sm"
                : "text-slate-600 hover:text-teal-700"
            }`}
          >
            Dữ liệu tham chiếu
          </button>
          <button
            type="button"
            aria-pressed={viewScope === "all"}
            onClick={() => onViewScopeChange("all")}
            className={`rounded-full px-4 py-2 text-xs font-semibold transition ${
              viewScope === "all"
                ? "bg-white text-teal-800 shadow-sm"
                : "text-slate-600 hover:text-teal-700"
            }`}
          >
            Toàn hệ thống
          </button>
        </div>
      </div>
      <div className="flex flex-col items-stretch gap-2 sm:items-end">
        {exportMessage ? (
          <p className="max-w-xs text-right text-xs font-medium text-amber-800" role="status">
            {exportMessage}
          </p>
        ) : null}
        <button
          type="button"
          onClick={onExportCsv}
          disabled={exportDisabled}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-teal-700 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {exporting ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <span className="material-symbols-outlined text-lg">download</span>
          )}
          Xuất CSV
        </button>
      </div>
    </header>
  );
}
