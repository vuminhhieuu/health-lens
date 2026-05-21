"use client";

import {
  ACTION_LABEL_VI,
  RESOURCE_TYPE_AUTH,
  RESOURCE_TYPE_CONSENT,
  RESOURCE_TYPE_HEALTH_RECORD,
  RESOURCE_TYPE_LLM_CALL,
  RESOURCE_TYPE_OCR_JOB,
  RESOURCE_TYPE_PROFILE,
  RESOURCE_TYPE_RAG_RETRIEVAL,
  RESOURCE_TYPE_REFERENCE_DATA,
  RESOURCE_TYPE_LABEL_VI,
  RESOURCE_TYPE_USER,
  type AuditViewScope,
  type ListQuery,
  type ReferenceMetricOption,
} from "@/lib/admin/auditLog";

type ActionFilterGroup = { label: string; actions: string[] };

export function AuditLogFiltersSection({
  viewScope,
  draft,
  setDraft,
  setDateRangeError,
  dateRangeError,
  showMetricFilter,
  metrics,
  metricsLoading,
  actionFilterGroups,
  appliedFilterChips,
  applyFilters,
  resetDraft,
}: {
  viewScope: AuditViewScope;
  draft: Omit<ListQuery, "page" | "limit">;
  setDraft: React.Dispatch<React.SetStateAction<Omit<ListQuery, "page" | "limit">>>;
  setDateRangeError: (error: string | null) => void;
  dateRangeError: string | null;
  showMetricFilter: boolean;
  metrics: ReferenceMetricOption[];
  metricsLoading: boolean;
  actionFilterGroups: ActionFilterGroup[];
  appliedFilterChips: string[];
  applyFilters: () => void;
  resetDraft: () => void;
}) {
  return (
    <div className="rounded-[28px] bg-white p-5 shadow-sm ring-1 ring-slate-200 sm:p-6">
      <p className="mb-4 text-xs font-bold uppercase tracking-wider text-slate-500">
        Bộ lọc
      </p>

      {dateRangeError ? (
        <p className="mb-4 rounded-xl border border-[#ffb59a]/50 bg-[#ffb59a]/10 px-3 py-2 text-sm text-[#924628]">
          {dateRangeError}
        </p>
      ) : null}

      <div
        className={`grid grid-cols-1 gap-4 md:grid-cols-2 ${
          viewScope === "all" ? "xl:grid-cols-4" : "xl:grid-cols-3"
        }`}
      >
        <div>
          <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
            Từ ngày
          </label>
          <input
            type="date"
            className="w-full rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
            value={draft.from}
            onChange={(e) => {
              setDateRangeError(null);
              setDraft((d) => ({ ...d, from: e.target.value }));
            }}
          />
        </div>
        <div>
          <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
            Đến ngày
          </label>
          <input
            type="date"
            className="w-full rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
            value={draft.to}
            onChange={(e) => {
              setDateRangeError(null);
              setDraft((d) => ({ ...d, to: e.target.value }));
            }}
          />
        </div>
        <div>
          <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
            Chỉ số
          </label>
          <div className="relative">
            <select
              className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15 disabled:cursor-not-allowed disabled:opacity-50"
              value={draft.resourceId}
              disabled={!showMetricFilter || metricsLoading}
              onChange={(e) => setDraft((d) => ({ ...d, resourceId: e.target.value }))}
            >
              <option value="">Tất cả chỉ số</option>
              {metrics.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.displayNameVi}
                </option>
              ))}
            </select>
            <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#00685f]">
              ▾
            </span>
          </div>
          {viewScope === "all" && !showMetricFilter ? (
            <p className="mt-1 text-[10px] text-[#3d4947]">
              Chỉ lọc theo chỉ số khi loại tài nguyên là dữ liệu tham chiếu.
            </p>
          ) : null}
        </div>
        {viewScope === "all" ? (
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Loại tài nguyên
            </label>
            <div className="relative">
              <select
                className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
                value={draft.resourceType}
                onChange={(e) =>
                  setDraft((d) => ({
                    ...d,
                    resourceType: e.target.value,
                    resourceId:
                      e.target.value === "" || e.target.value === RESOURCE_TYPE_REFERENCE_DATA
                        ? d.resourceId
                        : "",
                  }))
                }
              >
                <option value="">Tất cả loại</option>
                <option value={RESOURCE_TYPE_REFERENCE_DATA}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_REFERENCE_DATA]}
                </option>
                <option value={RESOURCE_TYPE_HEALTH_RECORD}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_HEALTH_RECORD]}
                </option>
                <option value={RESOURCE_TYPE_PROFILE}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_PROFILE]}
                </option>
                <option value={RESOURCE_TYPE_AUTH}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_AUTH]}
                </option>
                <option value={RESOURCE_TYPE_USER}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_USER]}
                </option>
                <option value={RESOURCE_TYPE_CONSENT}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_CONSENT]}
                </option>
                <option value={RESOURCE_TYPE_OCR_JOB}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_OCR_JOB]}
                </option>
                <option value={RESOURCE_TYPE_LLM_CALL}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_LLM_CALL]}
                </option>
                <option value={RESOURCE_TYPE_RAG_RETRIEVAL}>
                  {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_RAG_RETRIEVAL]}
                </option>
              </select>
              <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#00685f]">
                ▾
              </span>
            </div>
          </div>
        ) : null}
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
        <div>
          <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
            Người thực hiện
          </label>
          <input
            type="text"
            className="w-full rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
            placeholder="Email admin hoặc người dùng…"
            value={draft.actorEmail}
            onChange={(e) => setDraft((d) => ({ ...d, actorEmail: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                applyFilters();
              }
            }}
          />
        </div>
        <div>
          <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
            Correlation ID
          </label>
          <input
            type="text"
            className="w-full rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 font-mono text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
            placeholder="Dán X-Correlation-Id…"
            value={draft.correlationId}
            onChange={(e) => setDraft((d) => ({ ...d, correlationId: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                applyFilters();
              }
            }}
          />
        </div>
        <div>
          <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
            Loại hành động
          </label>
          <div className="relative">
            <select
              className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
              value={draft.action}
              onChange={(e) => setDraft((d) => ({ ...d, action: e.target.value }))}
            >
              <option value="">Tất cả hành động</option>
              {actionFilterGroups.map((group) => (
                <optgroup key={group.label} label={group.label}>
                  {group.actions.map((action) => (
                    <option key={action} value={action}>
                      {ACTION_LABEL_VI[action] ?? action}
                    </option>
                  ))}
                </optgroup>
              ))}
            </select>
            <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#00685f]">
              ▾
            </span>
          </div>
        </div>
      </div>

      <div className="mt-5 flex flex-col gap-3 border-t border-[#e9f6f3] pt-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-h-[1.75rem] flex-wrap gap-2">
          {appliedFilterChips.length === 0 ? (
            <span className="text-xs text-[#3d4947]">Chưa áp dụng bộ lọc nào</span>
          ) : (
            appliedFilterChips.map((chip) => (
              <span
                key={chip}
                className="rounded-full bg-[#e9f6f3] px-3 py-1 text-[11px] font-semibold text-[#134e4a]"
              >
                {chip}
              </span>
            ))
          )}
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <button
            type="button"
            onClick={resetDraft}
            className="rounded-full px-4 py-2 text-sm font-semibold text-[#3d4947] transition hover:bg-[#e9f6f3] hover:text-[#00685f]"
          >
            Đặt lại
          </button>
          <button
            type="button"
            onClick={applyFilters}
            className="rounded-full bg-[#00685f] px-5 py-2 text-sm font-bold text-white transition hover:bg-[#005049]"
          >
            Áp dụng
          </button>
        </div>
      </div>
    </div>
  );
}
