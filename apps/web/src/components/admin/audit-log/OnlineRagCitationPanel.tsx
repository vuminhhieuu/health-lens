"use client";

import { useCallback, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { Loader2 } from "lucide-react";

import { adminApiClient } from "@/lib/api/adminApiClient";
import {
  buildCitationParams,
  DEFAULT_CITATION_LIMIT,
  reviewStatusClass,
  reviewStatusDetail,
  reviewStatusLabel,
  type CitationFilters,
  type OnlineRagCitationPage,
} from "@/lib/admin/auditLog";
import { ApiPaths } from "@healthlens/shared/constants";

export function OnlineRagCitationPanel() {
  const [draft, setDraft] = useState<CitationFilters>({
    healthRecordId: "",
    metricName: "",
    sourceUrl: "",
    publisher: "",
    snapshotHash: "",
    answerHash: "",
    reviewStatus: "",
    page: 0,
    limit: DEFAULT_CITATION_LIMIT,
  });
  const [applied, setApplied] = useState(draft);
  const [exporting, setExporting] = useState(false);

  const query = useQuery({
    queryKey: ["admin-online-rag-citations", applied],
    queryFn: async () => {
      const res = await adminApiClient.get<OnlineRagCitationPage>(
        ApiPaths.ADMIN.ONLINE_RAG_CITATIONS,
        { params: buildCitationParams(applied) },
      );
      return res.data;
    },
  });

  const rows = query.data?.content ?? [];
  const total = query.data?.totalElements ?? 0;
  const totalPages = query.data?.limit
    ? Math.max(1, Math.ceil(total / query.data.limit))
    : 1;

  const apply = useCallback(() => {
    setApplied((prev) => ({ ...prev, ...draft, page: 0 }));
  }, [draft]);

  const reset = useCallback(() => {
    const cleared: CitationFilters = {
      healthRecordId: "",
      metricName: "",
      sourceUrl: "",
      publisher: "",
      snapshotHash: "",
      answerHash: "",
      reviewStatus: "",
      page: 0,
      limit: DEFAULT_CITATION_LIMIT,
    };
    setDraft(cleared);
    setApplied(cleared);
  }, []);

  const exportCsv = useCallback(async () => {
    if (total === 0) return;
    setExporting(true);
    try {
      const params = buildCitationParams(applied);
      delete params.page;
      delete params.limit;
      const search = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => search.set(key, String(value)));
      const response = await adminApiClient.get(
        `${ApiPaths.ADMIN.ONLINE_RAG_CITATIONS_EXPORT}?${search.toString()}`,
        { responseType: "blob" },
      );
      const blob = new Blob([response.data], { type: "text/csv;charset=utf-8" });
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      a.download = "online-rag-citations.csv";
      a.click();
      URL.revokeObjectURL(href);
    } finally {
      setExporting(false);
    }
  }, [applied, total]);

  return (
    <section className="flex flex-col overflow-hidden rounded-[28px] bg-white shadow-sm ring-1 ring-slate-200">
      <div className="flex flex-col justify-between gap-3 border-b border-slate-200 bg-slate-50 px-6 py-4 lg:flex-row lg:items-start">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-teal-700">travel_explore</span>
            <h2 className="font-bold text-slate-900">Citation online RAG</h2>
          </div>
          <p className="mt-1 max-w-3xl text-xs leading-relaxed text-slate-500">
            Metadata nguồn online đã ảnh hưởng hoặc được cân nhắc cho giải thích AI. Nội dung snapshot thô không hiển thị ở màn này.
          </p>
        </div>
        <button
          type="button"
          onClick={exportCsv}
          disabled={exporting || total === 0}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-teal-700 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {exporting ? <Loader2 className="h-4 w-4 animate-spin" /> : <span className="material-symbols-outlined text-lg">download</span>}
          Xuất citation
        </button>
      </div>

      <div className="grid grid-cols-1 gap-3 border-b border-slate-100 p-5 md:grid-cols-2 xl:grid-cols-4">
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Health record ID"
          value={draft.healthRecordId}
          onChange={(e) => setDraft((d) => ({ ...d, healthRecordId: e.target.value }))}
        />
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Tên chỉ số"
          value={draft.metricName}
          onChange={(e) => setDraft((d) => ({ ...d, metricName: e.target.value }))}
        />
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Source URL hoặc domain"
          value={draft.sourceUrl}
          onChange={(e) => setDraft((d) => ({ ...d, sourceUrl: e.target.value }))}
        />
        <div className="relative">
          <select
            className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
            value={draft.reviewStatus}
            onChange={(e) => setDraft((d) => ({ ...d, reviewStatus: e.target.value }))}
          >
            <option value="">Mọi trạng thái</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="REVIEW_REQUIRED">Cần rà soát</option>
            <option value="REJECTED">Từ chối</option>
          </select>
          <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#00685f]">▾</span>
        </div>
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Publisher"
          value={draft.publisher}
          onChange={(e) => setDraft((d) => ({ ...d, publisher: e.target.value }))}
        />
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Snapshot hash"
          value={draft.snapshotHash}
          onChange={(e) => setDraft((d) => ({ ...d, snapshotHash: e.target.value }))}
        />
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Answer hash"
          value={draft.answerHash}
          onChange={(e) => setDraft((d) => ({ ...d, answerHash: e.target.value }))}
        />
        <div className="flex items-center justify-end gap-2">
          <button type="button" onClick={reset} className="rounded-full px-4 py-2 text-sm font-semibold text-[#3d4947] transition hover:bg-[#e9f6f3]">
            Đặt lại
          </button>
          <button type="button" onClick={apply} className="rounded-full bg-[#00685f] px-5 py-2 text-sm font-bold text-white transition hover:bg-[#005049]">
            Lọc
          </button>
        </div>
      </div>

      {query.isLoading ? (
        <div className="flex items-center justify-center gap-2 py-14 text-[#3d4947]">
          <Loader2 className="h-5 w-5 animate-spin text-[#00685f]" />
          Đang tải citation…
        </div>
      ) : query.isError ? (
        <div className="p-8 text-center text-[#ba1a1a]">Không thể tải citation online RAG.</div>
      ) : rows.length === 0 ? (
        <div className="p-10 text-center text-sm text-[#3d4947]">Chưa có citation online RAG phù hợp.</div>
      ) : (
        <div className="hl-custom-scrollbar overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50">
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Nguồn</th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Record / chỉ số</th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Review</th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Hash</th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Thu thập</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((row) => (
                <tr key={row.id} className="transition-colors hover:bg-teal-50/30">
                  <td className="max-w-md px-6 py-4">
                    <p className="text-sm font-bold text-[#121e1c]">{row.publisher || "—"}</p>
                    <p className="truncate font-mono text-[11px] text-slate-500" title={row.sourceUrl}>{row.sourceUrl}</p>
                    <p className="mt-1 text-[11px] text-slate-500">
                      {row.cacheHit ? "Cache hit" : "Fetch mới"} · {row.retrievalSource}
                    </p>
                  </td>
                  <td className="px-6 py-4">
                    <p className="font-mono text-[11px] text-slate-600">{row.healthRecordId}</p>
                    <p className="mt-1 text-sm font-semibold text-slate-900">{row.metricName}</p>
                  </td>
                  <td className="whitespace-nowrap px-6 py-4">
                    <span className={`inline-block rounded px-2 py-1 text-xs font-semibold ${reviewStatusClass(row)}`}>
                      {reviewStatusLabel(row.reviewStatus)}
                    </span>
                    {reviewStatusDetail(row) ? (
                      <p className="mt-1 max-w-[13rem] text-[11px] leading-snug text-[#924628]">
                        {reviewStatusDetail(row)}
                      </p>
                    ) : null}
                  </td>
                  <td className="px-6 py-4">
                    <p className="font-mono text-[11px] text-slate-600" title={row.snapshotHash}>
                      {row.snapshotHash.slice(0, 12)}…
                    </p>
                    <p className="mt-1 font-mono text-[10px] text-slate-400" title={row.answerHash}>
                      answer {row.answerHash.slice(0, 10)}…
                    </p>
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-xs text-slate-600">
                    {row.retrievedAt ? format(new Date(row.retrievedAt), "HH:mm · dd/MM/yyyy", { locale: vi }) : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {rows.length > 0 ? (
        <div className="flex items-center justify-between border-t border-[#e9f6f3] bg-[#e9f6f3]/20 px-6 py-4">
          <span className="text-xs font-medium text-[#3d4947]">
            {rows.length.toLocaleString("vi-VN")} / {total.toLocaleString("vi-VN")} citation
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={applied.page <= 0}
              onClick={() => setApplied((p) => ({ ...p, page: Math.max(0, p.page - 1) }))}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
              aria-label="Trang citation trước"
              title="Trang citation trước"
            >
              <span className="material-symbols-outlined text-base">chevron_left</span>
            </button>
            <span className="text-xs font-semibold text-slate-600">{applied.page + 1}/{totalPages}</span>
            <button
              type="button"
              disabled={applied.page + 1 >= totalPages}
              onClick={() => setApplied((p) => ({ ...p, page: p.page + 1 }))}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
              aria-label="Trang citation sau"
              title="Trang citation sau"
            >
              <span className="material-symbols-outlined text-base">chevron_right</span>
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
}
