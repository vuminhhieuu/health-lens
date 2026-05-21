"use client";

import { CheckCircle } from "lucide-react";

import { EmptyState } from "./StateComponents";

type HealthMetricGridItem = {
  name: string;
  value: string;
  unit: string;
  displayNameVi?: string;
  status?: "normal" | "attention" | "abnormal" | "no_data";
  referenceRange?: {
    min: number;
    max: number;
  } | null;
};

type HealthMetricsGridProps = {
  metrics: HealthMetricGridItem[];
  onSelectMetric: (index: number) => void;
  emptyTitle?: string;
  emptyDescription?: string;
};

export function HealthMetricsGrid({
  metrics,
  onSelectMetric,
  emptyTitle = "Chưa có chỉ số nào",
  emptyDescription = "Hệ thống chưa trích xuất được chỉ số nào từ kết quả này.",
}: HealthMetricsGridProps) {
  if (metrics.length === 0) {
    return (
      <EmptyState
        title={emptyTitle}
        description={emptyDescription}
        className="min-h-48"
      />
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {metrics.map((metric, index) => {
        const metricValue = metric.value?.trim() || "--";
        const metricUnit = metric.unit?.trim() || "";
        const metricRangeText = compactRangeText(metric);
        const metricPercent = compactMetricPercent(metric);
        const isNormal = (metric.status ?? "no_data") === "normal";
        const metricLabel = metric.displayNameVi || metric.name;

        return (
          <button
            key={`${metric.name}-${index}`}
            type="button"
            className="group w-full cursor-pointer rounded-3xl bg-white p-5 text-left shadow-sm transition-all duration-200 hover:shadow-md hover:ring-2 hover:ring-[#00685f]/15 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
            onClick={() => onSelectMetric(index)}
            aria-label={`${metricLabel}, ${metricValue} ${metricUnit}, ngưỡng ${metricRangeText}, ${recordStatusLabel(metric.status)}`}
          >
            <div className="mb-5 flex items-start justify-between gap-2">
              <span className="line-clamp-2 text-xs font-extrabold uppercase text-[#3d4947]">
                {metricLabel}
              </span>
              <CheckCircle
                className={`h-4 w-4 shrink-0 ${isNormal ? "text-[#00685f]" : "text-[#6d7a77]"}`}
                aria-hidden="true"
              />
            </div>
            <div className="flex items-end gap-1.5">
              <span className="text-[44px] leading-none font-black text-[#121e1c]">
                {metricValue}
              </span>
              <span className="pb-1 text-2xs font-semibold text-[#3d4947]">
                {metricUnit}
              </span>
            </div>
            <div className="mt-4 h-2 w-full rounded-full bg-[#deebe8]">
              <div
                className="h-full rounded-full bg-[#008378] transition-all"
                style={{ width: `${metricPercent}%` }}
                aria-hidden="true"
              />
            </div>
            <div className="mt-3 flex items-center justify-between gap-3 text-xs font-extrabold uppercase">
              <span className="truncate text-[#4e6360]">
                Ngưỡng: {metricRangeText}
              </span>
              <span className={isNormal ? "text-[#00685f]" : "text-[#773215]"}>
                {recordStatusLabel(metric.status)}
              </span>
            </div>
          </button>
        );
      })}
    </div>
  );
}

function compactRangeText(metric: HealthMetricGridItem): string {
  const min = metric.referenceRange?.min;
  const max = metric.referenceRange?.max;
  if (typeof min === "number" && typeof max === "number") {
    return `${min}-${max}`;
  }
  if (typeof max === "number") {
    return `<${max}`;
  }
  return "N/A";
}

function compactMetricPercent(metric: HealthMetricGridItem): number {
  const raw = metric.value?.replace(",", ".").trim() ?? "";
  const numericValue = Number(raw);
  const min = metric.referenceRange?.min;
  const max = metric.referenceRange?.max;
  if (
    !Number.isFinite(numericValue) ||
    typeof min !== "number" ||
    typeof max !== "number" ||
    max <= min
  ) {
    return 60;
  }
  const ratio = ((numericValue - min) / (max - min)) * 100;
  return Math.max(8, Math.min(100, Math.round(ratio)));
}

function recordStatusLabel(status?: HealthMetricGridItem["status"]): string {
  if (status === "abnormal") return "Bất thường";
  if (status === "attention") return "Cần chú ý";
  if (status === "normal") return "Bình thường";
  return "Không rõ";
}
