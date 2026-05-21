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

        return (
          // Accessibility: prefer using a native <button> (or an <a> with href)
          // instead of changing semantics of a non-interactive element via
          // `role="button"` + `tabIndex={0}`. Native controls expose correct
          // keyboard behaviour (Space/Enter), focus handling, and built-in
          // accessibility APIs to assistive tech. If you must use `role="button"`,
          // ensure you fully implement keyboard handlers, focus styles, and ARIA
          // states consistently. The simplest and most robust change is:
          //   <button type="button" className="..." onClick={...}>...</button>
          // which preserves the present visuals while improving semantics.
          <article
            key={`${metric.name}-${index}`}
            role="button"
            tabIndex={0}
            className="group cursor-pointer rounded-3xl bg-white p-5 shadow-sm transition-all duration-200 hover:shadow-md hover:ring-2 hover:ring-[#00685f]/15"
            onClick={() => onSelectMetric(index)}
            // Accessibility: activate on Enter (keydown) and Space (keyup) to match
            // native button semantics. Handling Space on keydown can cause
            // inconsistent behaviour with screen readers; keep Enter on keydown
            // and Space on keyup.
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                onSelectMetric(index);
              }
            }}
            onKeyUp={(event) => {
              if (event.key === " ") {
                event.preventDefault();
                onSelectMetric(index);
              }
            }}
          >
            <div className="mb-5 flex items-start justify-between gap-2">
              <span className="line-clamp-2 text-xs font-extrabold uppercase text-[#3d4947]">
                {metric.displayNameVi || metric.name}
              </span>
              <CheckCircle
                className={`h-4 w-4 shrink-0 ${isNormal ? "text-[#00685f]" : "text-[#6d7a77]"}`}
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
          </article>
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
