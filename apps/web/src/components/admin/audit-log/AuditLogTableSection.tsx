"use client";

import type { UseQueryResult } from "@tanstack/react-query";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { Loader2 } from "lucide-react";

import {
  actionBadgeClass,
  actionLabelVi,
  avatarBadgeClass,
  displayNameFromEmail,
  initialsFromEmail,
  outcomeBadgeClass,
  outcomeLabelVi,
  resolveOutcome,
  type AuditLogEntry,
  type AuditLogPage,
  type AuditViewScope,
  type ListQuery,
} from "@/lib/admin/auditLog";

export function AuditLogTableSection({
  listQuery,
  rows,
  total,
  applied,
  setApplied,
  setDetailEntry,
  pageButtons,
  totalPages,
  hasActiveFilters,
  viewScope,
  onRefresh,
}: {
  listQuery: UseQueryResult<AuditLogPage, Error>;
  rows: AuditLogEntry[];
  total: number;
  applied: ListQuery;
  setApplied: React.Dispatch<React.SetStateAction<ListQuery>>;
  setDetailEntry: (entry: AuditLogEntry) => void;
  pageButtons: (number | "ellipsis")[];
  totalPages: number;
  hasActiveFilters: boolean;
  viewScope: AuditViewScope;
  onRefresh: () => void;
}) {
  return (
    <div className="flex flex-col overflow-hidden rounded-[28px] bg-white shadow-sm ring-1 ring-slate-200">
      <div className="flex flex-col justify-between gap-3 border-b border-slate-200 bg-slate-50 px-6 py-4 sm:flex-row sm:items-center">
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-teal-700">history</span>
          <span className="font-bold text-slate-800">
            {listQuery.isLoading
              ? "Đang tải…"
              : `${total.toLocaleString("vi-VN")} bản ghi`}
          </span>
          {applied.correlationId.trim() ? (
            <span className="rounded-full bg-teal-100 px-3 py-1 font-mono text-[11px] font-semibold text-teal-800">
              Trace chronological · {applied.correlationId.trim()}
            </span>
          ) : null}
        </div>
        <button
          type="button"
          onClick={onRefresh}
          className="inline-flex items-center justify-center rounded-lg p-2 text-slate-600 transition hover:bg-teal-50"
          title="Tải lại"
          aria-label="Tải lại"
        >
          <span
            className={`material-symbols-outlined text-[20px] ${listQuery.isFetching ? "inline-block animate-spin" : ""}`}
          >
            refresh
          </span>
        </button>
      </div>
      {listQuery.isLoading ? (
        <div className="flex items-center justify-center gap-2 py-20 text-[#3d4947]">
          <Loader2 className="h-6 w-6 animate-spin text-[#00685f]" />
          Đang tải…
        </div>
      ) : listQuery.isError ? (
        <div className="p-10 text-center text-[#ba1a1a]">
          Không thể tải nhật ký. Vui lòng thử lại sau.
        </div>
      ) : rows.length === 0 ? (
        <div className="p-14 text-center text-sm text-[#3d4947]">
          {hasActiveFilters
            ? "Không có bản ghi phù hợp với bộ lọc hiện tại."
            : viewScope === "reference"
              ? "Chưa có hoạt động nào trên dữ liệu tham chiếu."
              : "Chưa có hoạt động được ghi nhận."}
        </div>
      ) : (
        <div className="hl-custom-scrollbar overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50">
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Thời gian
                </th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Người dùng
                </th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Hành động
                </th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Trạng thái
                </th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Tóm tắt
                </th>
                <th className="whitespace-nowrap px-6 py-3 text-center text-xs font-semibold uppercase tracking-wider text-slate-500">
                  IP
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((row) => {
                const at = new Date(row.createdAt);
                const email = row.actorEmail?.trim() || "";
                return (
                  <tr key={row.id} className="transition-colors hover:bg-teal-50/30">
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="text-sm font-bold text-[#121e1c]">
                          {format(at, "HH:mm:ss", { locale: vi })}
                        </span>
                        <span className="text-[10px] font-medium text-[#3d4947]">
                          {format(at, "dd/MM/yyyy", { locale: vi })}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div
                          className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${avatarBadgeClass(email)}`}
                          aria-hidden
                        >
                          {initialsFromEmail(email)}
                        </div>
                        <div className="min-w-0 leading-tight">
                          <span className="block truncate text-sm font-bold text-[#121e1c]">
                            {displayNameFromEmail(email)}
                          </span>
                          <span className="block truncate text-[10px] italic text-[#3d4947]">
                            {email || "—"}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <span
                        className={`inline-block rounded px-2 py-1 text-xs font-semibold ${actionBadgeClass(row.action)}`}
                      >
                        {actionLabelVi(row.action)}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <span
                        className={`inline-block rounded px-2 py-1 text-xs font-semibold ${outcomeBadgeClass(resolveOutcome(row))}`}
                      >
                        {outcomeLabelVi(resolveOutcome(row))}
                      </span>
                    </td>
                    <td className="max-w-md px-6 py-4">
                      <p
                        className="line-clamp-2 text-xs leading-relaxed text-slate-600"
                        title={row.detailSummary?.trim() || undefined}
                      >
                        {row.detailSummary?.trim() || "—"}
                      </p>
                      <button
                        type="button"
                        onClick={() => setDetailEntry(row)}
                        className="mt-1 text-xs font-semibold text-teal-700 hover:underline"
                        aria-label={`Xem chi tiết: ${row.entityLabel || row.action}`}
                      >
                        Xem chi tiết
                      </button>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className="inline-block rounded bg-slate-100 px-2 py-0.5 font-mono text-[11px] text-slate-600">
                        {row.ipAddress ?? "—"}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {!listQuery.isLoading &&
      !listQuery.isError &&
      rows.length > 0 ? (
        <div className="flex flex-col items-center justify-between gap-4 border-t border-[#e9f6f3] bg-[#e9f6f3]/20 px-6 py-4 sm:flex-row">
          <span className="text-xs font-medium text-[#3d4947]">
            Đang hiển thị {rows.length.toLocaleString("vi-VN")} trên {total.toLocaleString("vi-VN")}{" "}
            nhật ký
          </span>
          <div className="flex flex-wrap items-center justify-center gap-1">
            <button
              type="button"
              disabled={applied.page <= 0}
              onClick={() => setApplied((p) => ({ ...p, page: 0 }))}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
              aria-label="Trang đầu"
              title="Trang đầu"
            >
              <span className="material-symbols-outlined text-base" aria-hidden="true">first_page</span>
            </button>
            <button
              type="button"
              disabled={applied.page <= 0}
              onClick={() => setApplied((p) => ({ ...p, page: Math.max(0, p.page - 1) }))}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
              aria-label="Trang trước"
              title="Trang trước"
            >
              <span className="material-symbols-outlined text-base" aria-hidden="true">chevron_left</span>
            </button>
            {pageButtons.map((item, idx) =>
              item === "ellipsis" ? (
                <span key={`ellipsis-${idx}`} className="px-2 text-[#3d4947]">
                  ...
                </span>
              ) : (
                <button
                  key={`page-${item}`}
                  type="button"
                  onClick={() => setApplied((p) => ({ ...p, page: item }))}
                  className={`flex h-8 w-8 items-center justify-center rounded-lg text-xs font-bold transition ${
                    applied.page === item
                      ? "bg-[#00685f] text-white shadow-sm"
                      : "text-[#3d4947] hover:bg-[#deebe8]"
                  }`}
                >
                  {item + 1}
                </button>
              ),
            )}
            <button
              type="button"
              disabled={applied.page + 1 >= totalPages}
              onClick={() => setApplied((p) => ({ ...p, page: p.page + 1 }))}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
              aria-label="Trang sau"
              title="Trang sau"
            >
              <span className="material-symbols-outlined text-base" aria-hidden="true">chevron_right</span>
            </button>
            <button
              type="button"
              disabled={applied.page + 1 >= totalPages}
              onClick={() => setApplied((p) => ({ ...p, page: totalPages - 1 }))}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
              aria-label="Trang cuối"
              title="Trang cuối"
            >
              <span className="material-symbols-outlined text-base" aria-hidden="true">last_page</span>
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
