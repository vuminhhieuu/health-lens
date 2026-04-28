"use client";

import { useState } from "react";
import { AlertTriangle, CheckCircle2, ChevronDown, ChevronUp, Info, XCircle } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib/api/apiClient";
import { ApiPaths } from "@healthlens/shared/constants";

type MetricStatus = "normal" | "attention" | "abnormal" | "no_data";

type ReferenceRange = {
  min: number;
  max: number;
  attentionMin: number;
  attentionMax: number;
  unit?: string;
};

type RangeContext = {
  gender?: string | null;
  ageRange?: string | null;
};

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

const UI_FALLBACK_EXPLANATION =
  "Chỉ số này cần được bác sĩ giải thích thêm để đánh giá chính xác.";

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

  const explanationText = staticExplanation || explanationQuery.data?.explanation || "";
  const showExplanationSkeleton = expanded && !staticExplanation && explanationQuery.isLoading;
  const showQueryFallback = expanded && !staticExplanation && explanationQuery.isError;

  const displayReference = referenceRange
    ? `${referenceRange.min} - ${referenceRange.max} ${referenceRange.unit ?? unit}`
    : "Không có dữ liệu tham chiếu";
  const contextNote = buildRangeContextNote(rangeContext);

  return (
    <button
      type="button"
      onClick={() => setExpanded((prev) => !prev)}
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

      <div className={`grid transition-all duration-200 ${expanded ? "grid-rows-[1fr] pt-3" : "grid-rows-[0fr] pt-0"}`}>
        <div className="overflow-hidden">
          <div className="rounded-xl bg-[#f7fbfa] p-3 text-sm text-[#35514c]">
            <p>
              <span className="font-semibold">Ngưỡng tham chiếu: </span>
              {displayReference}
            </p>
            {referenceRangeSource && referenceRangeSource !== "none" ? (
              <p className="mt-2">
                <span className="font-semibold">Nguồn ngưỡng: </span>
                {referenceRangeSource === "document" ? "Theo phiếu xét nghiệm" : "Theo hệ thống tham chiếu"}
              </p>
            ) : null}
            {referenceRangeSource === "system" ? (
              contextNote ? (
                <p className="mt-2">
                  <span className="font-semibold">Ngữ cảnh ngưỡng: </span>
                  {contextNote}
                </p>
              ) : (
                <p className="mt-2">
                  <span className="font-semibold">Ngữ cảnh ngưỡng: </span>
                  Ngưỡng tham chiếu chung
                </p>
              )
            ) : null}
            {critical ? (
              <p className="mt-2 rounded-lg bg-[#fff2f2] px-2 py-1 text-[#ba1a1a]">
                Chỉ số có dấu hiệu vượt ngưỡng nguy cấp, nên liên hệ bác sĩ để được tư vấn sớm.
              </p>
            ) : null}
            {showExplanationSkeleton ? (
              <div className="mt-2 space-y-2" data-testid="explanation-skeleton">
                <div className="h-3 w-full animate-pulse rounded bg-[#d4e7e3]" />
                <div className="h-3 w-4/5 animate-pulse rounded bg-[#d4e7e3]" />
              </div>
            ) : null}
            {showQueryFallback ? (
              <p className="mt-2">
                <span className="font-semibold">Giải thích: </span>
                {UI_FALLBACK_EXPLANATION}
              </p>
            ) : null}
            {explanationText ? (
              <p className="mt-2 whitespace-pre-line">
                <span className="font-semibold">Giải thích: </span>
                {explanationText}
              </p>
            ) : null}
          </div>
        </div>
      </div>
    </button>
  );
}

function buildRangeContextNote(rangeContext?: RangeContext | null): string | null {
  if (!rangeContext || (!rangeContext.gender && !rangeContext.ageRange)) {
    return null;
  }
  const genderLabel =
    rangeContext.gender === "female"
      ? "Nữ"
      : rangeContext.gender === "male"
      ? "Nam"
      : null;
  const ageLabel = rangeContext.ageRange ? `${rangeContext.ageRange} tuổi` : null;
  const parts = [genderLabel, ageLabel].filter(Boolean);
  return parts.length > 0 ? `Ngưỡng áp dụng cho: ${parts.join(", ")}` : null;
}
