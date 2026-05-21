"use client";

import { useState } from "react";
import { AlertTriangle, CheckCircle2, ChevronDown, ChevronUp, Info, XCircle } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { ApiPaths } from "@healthlens/shared/constants";

import { apiClient } from "@/lib/api/apiClient";
import { toThreeLineExplanation } from "@/lib/utils/explanationFormatter";

import { ReferenceRangeIndicator, type RangeContext, type ReferenceRange } from "./ReferenceRangeIndicator";

type MetricStatus = "normal" | "attention" | "abnormal" | "no_data";

type HealthMetricCardProps = {
  recordId?: string;
  metricName: string;
  displayNameVi?: string;
  value: string;
  unit: string;
  referenceRange?: ReferenceRange | null;
  rangeContext?: RangeContext | null;
  referenceRangeSource?: "document" | "system" | "none";
  status: MetricStatus;
  critical?: boolean;
  explanation?: string;
};

const STATUS_META: Record<MetricStatus, { label: string; color: string; icon: typeof CheckCircle2 }> = {
  normal: {
    label: "Bình thường",
    color: "#10B981",
    icon: CheckCircle2,
  },
  attention: {
    label: "Cần chú ý",
    color: "#F59E0B",
    icon: AlertTriangle,
  },
  abnormal: {
    label: "Bất thường",
    color: "#EF4444",
    icon: XCircle,
  },
  no_data: {
    label: "Không có dữ liệu tham chiếu",
    color: "#6B7280",
    icon: Info,
  },
};

export function HealthMetricCard({
  recordId,
  metricName,
  displayNameVi,
  value,
  unit,
  referenceRange,
  rangeContext,
  referenceRangeSource,
  status,
  critical,
  explanation,
}: HealthMetricCardProps) {
  const [expanded, setExpanded] = useState(false);
  const meta = STATUS_META[status];
  const StatusIcon = meta.icon;
  const staticExplanation = explanation?.trim() ? explanation.trim() : "";

  const explanationQuery = useQuery({
    queryKey: ["metric-explanation", recordId, metricName, value, status],
    queryFn: async () => {
      if (!recordId) {
        return { explanation: "", source: "fallback" };
      }
      const res = await apiClient.get(ApiPaths.HEALTH_RECORDS.EXPLANATION(recordId, metricName));
      const payload = res.data?.data;
      return {
        explanation: (payload?.explanation as string | undefined) ?? "",
        source: (payload?.source as string | undefined) ?? "fallback",
      };
    },
    enabled: expanded && !staticExplanation && Boolean(recordId),
    staleTime: 7 * 24 * 60 * 60 * 1000,
  });

  const explanationText = staticExplanation
    ? toThreeLineExplanation(staticExplanation)
    : expanded && !explanationQuery.isLoading
      ? toThreeLineExplanation(explanationQuery.data?.explanation)
      : "";
  const showExplanationSkeleton = expanded && !staticExplanation && explanationQuery.isLoading;
  const detailsId = `health-metric-${metricName.replace(/[^a-zA-Z0-9_-]+/g, "-")}-details`;

  return (
    <button
      type="button"
      onClick={() => setExpanded((prev) => !prev)}
      aria-expanded={expanded}
      aria-controls={detailsId}
      className="w-full rounded-2xl border border-[#c5dfd9] bg-white p-4 text-left shadow-sm transition hover:border-[#8ec4ba]"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold text-[#005049]">{displayNameVi || metricName}</h3>
          <p className="mt-1 text-xl font-bold text-[#00332f]">
            {value || "-"} {unit}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span
            className="inline-flex items-center gap-1 rounded-full border px-3 py-1 text-xs font-semibold"
            style={{
              color: meta.color,
              borderColor: `${meta.color}66`,
              backgroundColor: `${meta.color}1A`,
            }}
          >
            <StatusIcon className="h-3.5 w-3.5" />
            <span>{meta.label}</span>
          </span>
          {expanded ? <ChevronUp className="h-4 w-4 text-[#4e6360]" /> : <ChevronDown className="h-4 w-4 text-[#4e6360]" />}
        </div>
      </div>

      <div
        id={detailsId}
        className={`grid transition-all duration-200 ${expanded ? "grid-rows-[1fr] pt-3" : "grid-rows-[0fr] pt-0"}`}
      >
        <div className="overflow-hidden">
          <ReferenceRangeIndicator
            referenceRange={referenceRange}
            unit={unit}
            referenceRangeSource={referenceRangeSource}
            rangeContext={rangeContext}
            critical={critical}
            explanation={showExplanationSkeleton ? undefined : explanationText}
            isExplanationLoading={showExplanationSkeleton}
            className="rounded-xl bg-[#f7fbfa] p-3 text-sm text-[#35514c]"
            skeletonLines={2}
          />
        </div>
      </div>
    </button>
  );
}
