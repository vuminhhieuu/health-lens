"use client";

import { useCallback, useState } from "react";

import { adminApiClient } from "@/lib/api/adminApiClient";
import { buildListParams, type ListQuery } from "@/lib/admin/auditLog";
import { ApiPaths } from "@healthlens/shared/constants";

export function useAuditLogExport(applied: ListQuery, total: number) {
  const [exporting, setExporting] = useState(false);
  const [exportMessage, setExportMessage] = useState<string | null>(null);

  const clearExportMessage = useCallback(() => {
    setExportMessage(null);
  }, []);

  const handleExportCsv = useCallback(async () => {
    if (total === 0) {
      setExportMessage("Không có dữ liệu để xuất với bộ lọc hiện tại.");
      return;
    }
    setExportMessage(null);
    setExporting(true);
    try {
      const params = buildListParams(applied);
      delete params.page;
      delete params.limit;

      const search = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        search.set(k, String(v));
      });

      const url = `${ApiPaths.ADMIN.AUDIT_LOGS_EXPORT}?${search.toString()}`;
      const response = await adminApiClient.get(url, { responseType: "blob" });

      const blob = new Blob([response.data], { type: "text/csv;charset=utf-8" });
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      a.download = "system-audit-logs.csv";
      a.click();
      URL.revokeObjectURL(href);
    } catch {
      setExportMessage("Không thể xuất CSV. Vui lòng thử lại sau.");
    } finally {
      setExporting(false);
    }
  }, [applied, total]);

  return {
    exporting,
    exportMessage,
    handleExportCsv,
    clearExportMessage,
  };
}
