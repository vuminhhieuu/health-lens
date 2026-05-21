"use client";

import { useCallback, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";

import { AuditLogDetailModal } from "@/components/admin/audit-log/AuditLogDetailModal";
import { AuditLogFiltersSection } from "@/components/admin/audit-log/AuditLogFiltersSection";
import { AuditLogPageHeader } from "@/components/admin/audit-log/AuditLogPageHeader";
import { AuditLogTableSection } from "@/components/admin/audit-log/AuditLogTableSection";
import { OnlineRagCitationPanel } from "@/components/admin/audit-log/OnlineRagCitationPanel";
import { useAdminMaterialSymbols } from "@/hooks/admin/useAdminMaterialSymbols";
import { useAuditLogExport } from "@/hooks/admin/useAuditLogExport";
import { useAuditLogFilters } from "@/hooks/admin/useAuditLogFilters";
import type { AuditLogEntry, AuditViewScope } from "@/lib/admin/auditLog";

export default function AuditLogPage() {
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  useAdminMaterialSymbols();

  const {
    viewScope,
    draft,
    setDraft,
    applied,
    setApplied,
    dateRangeError,
    setDateRangeError,
    applyFilters: applyFiltersInner,
    resetDraft: resetDraftInner,
    handleViewScopeChange: changeViewScope,
    applyTraceFilter: applyTraceFilterInner,
    metrics,
    metricsLoading,
    listQuery,
    showMetricFilter,
    hasActiveFilters,
    actionFilterGroups,
    appliedFilterChips,
    totalPages,
    pageButtons,
    rows,
    total,
  } = useAuditLogFilters(searchParams);

  const { exporting, exportMessage, handleExportCsv, clearExportMessage } = useAuditLogExport(
    applied,
    total,
  );

  const [detailEntry, setDetailEntry] = useState<AuditLogEntry | null>(null);

  const handleViewScopeChange = useCallback(
    (scope: AuditViewScope) => {
      clearExportMessage();
      changeViewScope(scope);
    },
    [changeViewScope, clearExportMessage],
  );

  const applyFilters = useCallback(() => {
    if (applyFiltersInner()) {
      clearExportMessage();
    }
  }, [applyFiltersInner, clearExportMessage]);

  const resetDraft = useCallback(() => {
    clearExportMessage();
    resetDraftInner();
  }, [clearExportMessage, resetDraftInner]);

  const applyTraceFilter = useCallback(
    (correlationId: string) => {
      clearExportMessage();
      setDetailEntry(null);
      applyTraceFilterInner(correlationId);
    },
    [applyTraceFilterInner, clearExportMessage],
  );

  return (
    <div className="space-y-6 text-slate-900">
      <AuditLogPageHeader
        viewScope={viewScope}
        onViewScopeChange={handleViewScopeChange}
        exporting={exporting}
        exportMessage={exportMessage}
        onExportCsv={handleExportCsv}
        exportDisabled={exporting || total === 0}
      />

      <AuditLogFiltersSection
        viewScope={viewScope}
        draft={draft}
        setDraft={setDraft}
        setDateRangeError={setDateRangeError}
        dateRangeError={dateRangeError}
        showMetricFilter={showMetricFilter}
        metrics={metrics}
        metricsLoading={metricsLoading}
        actionFilterGroups={actionFilterGroups}
        appliedFilterChips={appliedFilterChips}
        applyFilters={applyFilters}
        resetDraft={resetDraft}
      />

      <AuditLogTableSection
        listQuery={listQuery}
        rows={rows}
        total={total}
        applied={applied}
        setApplied={setApplied}
        setDetailEntry={setDetailEntry}
        pageButtons={pageButtons}
        totalPages={totalPages}
        hasActiveFilters={hasActiveFilters}
        viewScope={viewScope}
        onRefresh={() => queryClient.invalidateQueries({ queryKey: ["admin-audit-logs"] })}
      />

      <OnlineRagCitationPanel />

      {detailEntry ? (
        <AuditLogDetailModal
          entry={detailEntry}
          onClose={() => setDetailEntry(null)}
          onTraceFilter={applyTraceFilter}
        />
      ) : null}
    </div>
  );
}
