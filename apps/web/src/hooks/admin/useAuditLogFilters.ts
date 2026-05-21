"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";

import { adminApiClient } from "@/lib/api/adminApiClient";
import {
  ACTION_FILTER_GROUPS,
  auditHasActiveFilters,
  buildAppliedFilterChips,
  buildAuditLogUrl,
  buildListParams,
  buildPageList,
  buildTraceOnlyFilters,
  DEFAULT_LIMIT,
  REFERENCE_ACTION_GROUP,
  REFERENCE_ACTIONS,
  RESOURCE_TYPE_REFERENCE_DATA,
  resolveInitialAuditState,
  validateDateRange,
  type AuditLogPage as AuditLogPageData,
  type AuditViewScope,
  type ListQuery,
  type ReferenceMetricOption,
} from "@/lib/admin/auditLog";
import { ApiPaths } from "@healthlens/shared/constants";

export function useAuditLogFilters(searchParams: URLSearchParams) {
  const router = useRouter();
  const initial = useMemo(() => resolveInitialAuditState(searchParams), [searchParams]);

  const [viewScope, setViewScope] = useState<AuditViewScope>(initial.viewScope);
  const [draft, setDraft] = useState(initial.filters);
  const [applied, setApplied] = useState<ListQuery>({
    ...initial.filters,
    page: 0,
    limit: DEFAULT_LIMIT,
  });
  const [dateRangeError, setDateRangeError] = useState<string | null>(null);

  const searchParamsKey = searchParams.toString();

  useEffect(() => {
    const resolved = resolveInitialAuditState(searchParams);
    queueMicrotask(() => {
      setViewScope(resolved.viewScope);
      setDraft(resolved.filters);
      setApplied((prev) => ({
        ...resolved.filters,
        page: 0,
        limit: prev.limit,
      }));
    });
  }, [searchParams, searchParamsKey]);

  const { data: metrics = [], isLoading: metricsLoading } = useQuery({
    queryKey: ["admin-reference-metrics", "audit-filters"],
    queryFn: async () => {
      const res = await adminApiClient.get<ReferenceMetricOption[]>(
        ApiPaths.ADMIN.REFERENCE_METRICS,
      );
      return res.data;
    },
  });

  const listQuery = useQuery({
    queryKey: ["admin-audit-logs", applied],
    queryFn: async () => {
      const res = await adminApiClient.get<AuditLogPageData>(ApiPaths.ADMIN.AUDIT_LOGS, {
        params: buildListParams(applied),
      });
      return res.data;
    },
  });

  const normalizeFilters = useCallback(
    (filters: Omit<ListQuery, "page" | "limit">): Omit<ListQuery, "page" | "limit"> => ({
      ...filters,
      resourceType:
        viewScope === "reference" ? RESOURCE_TYPE_REFERENCE_DATA : filters.resourceType,
      action:
        viewScope === "reference" && filters.action && !REFERENCE_ACTIONS.has(filters.action)
          ? ""
          : filters.action,
    }),
    [viewScope],
  );

  const applyFilters = useCallback(() => {
    const rangeError = validateDateRange(draft.from, draft.to);
    if (rangeError) {
      setDateRangeError(rangeError);
      return false;
    }
    setDateRangeError(null);
    const next = normalizeFilters(draft);
    setDraft(next);
    setApplied((prev) => ({
      ...prev,
      ...next,
      page: 0,
    }));
    router.replace(buildAuditLogUrl(viewScope, next), { scroll: false });
    return true;
  }, [draft, normalizeFilters, router, viewScope]);

  const resetDraft = useCallback(() => {
    const cleared: Omit<ListQuery, "page" | "limit"> = {
      resourceType: viewScope === "reference" ? RESOURCE_TYPE_REFERENCE_DATA : "",
      resourceId: "",
      actorEmail: "",
      action: "",
      correlationId: "",
      from: "",
      to: "",
    };
    setDateRangeError(null);
    setDraft(cleared);
    setApplied((prev) => ({
      ...prev,
      ...cleared,
      page: 0,
    }));
    router.replace(buildAuditLogUrl(viewScope, cleared), { scroll: false });
  }, [router, viewScope]);

  const handleViewScopeChange = useCallback(
    (scope: AuditViewScope) => {
      setViewScope(scope);
      setDateRangeError(null);
      const next: Omit<ListQuery, "page" | "limit"> = {
        ...draft,
        resourceType: scope === "reference" ? RESOURCE_TYPE_REFERENCE_DATA : "",
        resourceId: scope === "reference" ? draft.resourceId : "",
        action:
          scope === "reference" && draft.action && !REFERENCE_ACTIONS.has(draft.action)
            ? ""
            : draft.action,
      };
      setDraft(next);
      setApplied((prev) => ({
        ...prev,
        ...next,
        page: 0,
      }));
      router.replace(buildAuditLogUrl(scope, next), { scroll: false });
    },
    [draft, router],
  );

  const applyTraceFilter = useCallback(
    (correlationId: string) => {
      const trimmedCorrelationId = correlationId.trim();
      if (!trimmedCorrelationId) {
        return;
      }
      const next = buildTraceOnlyFilters(trimmedCorrelationId);
      setViewScope("all");
      setDateRangeError(null);
      setDraft(next);
      setApplied((prev) => ({
        ...prev,
        ...next,
        page: 0,
      }));
      router.replace(buildAuditLogUrl("all", next), { scroll: false });
    },
    [router],
  );

  const totalPages = useMemo(() => {
    if (!listQuery.data) {
      return 0;
    }
    const { totalElements, limit } = listQuery.data;
    if (limit <= 0) {
      return 0;
    }
    return Math.max(1, Math.ceil(totalElements / limit));
  }, [listQuery.data]);

  const pageButtons = useMemo(
    () => buildPageList(applied.page, totalPages),
    [applied.page, totalPages],
  );

  const rows = listQuery.data?.content ?? [];
  const total = listQuery.data?.totalElements ?? 0;

  const showMetricFilter =
    viewScope === "reference" ||
    draft.resourceType === "" ||
    draft.resourceType === RESOURCE_TYPE_REFERENCE_DATA;

  const hasActiveFilters = useMemo(
    () => auditHasActiveFilters(applied, viewScope),
    [applied, viewScope],
  );

  const actionFilterGroups =
    viewScope === "reference" ? [REFERENCE_ACTION_GROUP] : ACTION_FILTER_GROUPS;

  const appliedFilterChips = useMemo(
    () => buildAppliedFilterChips(applied, metrics),
    [applied, metrics],
  );

  return {
    viewScope,
    draft,
    setDraft,
    applied,
    setApplied,
    dateRangeError,
    setDateRangeError,
    applyFilters,
    resetDraft,
    handleViewScopeChange,
    applyTraceFilter,
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
  };
}
