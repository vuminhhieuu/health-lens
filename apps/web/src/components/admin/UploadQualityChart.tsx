"use client";

import { useMemo } from "react";
import { formatActivityBucketLabel, formatActivityBucketTooltip } from "@/lib/admin/utcPeriodLabels";
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
export type UploadQualityBucket = {
  date: string;
  success: number;
  failed: number;
  successRate: number;
  failureBreakdown: Record<string, number>;
};

type ChartRow = UploadQualityBucket & {
  label: string;
  total: number;
  successRatePercent: number;
  belowTarget: boolean;
};

type UploadQualityChartProps = {
  buckets: UploadQualityBucket[];
  granularity?: "day" | "week";
  successRateTarget?: number;
  onBucketSelect?: (bucket: UploadQualityBucket) => void;
};

type ChartTooltipProps = {
  active?: boolean;
  payload?: unknown;
  granularity: "day" | "week";
  successRateTarget: number;
};

function chartRowFromTooltipPayload(payload: unknown): ChartRow | null {
  if (!Array.isArray(payload) || payload.length === 0) return null;
  const item = payload[0] as { payload?: ChartRow };
  return item?.payload ?? null;
}

function ChartTooltip({ active, payload, granularity, successRateTarget }: ChartTooltipProps) {
  if (!active) return null;

  const row = chartRowFromTooltipPayload(payload);
  if (!row) return null;

  const targetPercent = Math.round(successRateTarget * 100);

  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 shadow-md text-sm min-w-[200px]">
      <p className="font-semibold text-slate-900 mb-2">
        {formatActivityBucketTooltip(row.date, granularity)}
      </p>
      <p className="text-emerald-700">
        Thành công: <span className="font-semibold">{row.success}</span>
      </p>
      <p className="text-red-600 mt-0.5">
        Thất bại: <span className="font-semibold">{row.failed}</span>
      </p>
      <p className="text-slate-600 mt-1.5 border-t border-slate-100 pt-1.5">
        Tỷ lệ thành công:{" "}
        <span className={`font-semibold ${row.belowTarget ? "text-red-600" : "text-emerald-700"}`}>
          {row.successRatePercent.toFixed(1)}%
        </span>
        <span className="text-slate-400"> / mục tiêu {targetPercent}%</span>
      </p>
      {row.failed > 0 && (
        <p className="text-xs text-slate-500 mt-2">Nhấp cột đỏ để xem nguyên nhân lỗi</p>
      )}
    </div>
  );
}

function RateDot({
  cx,
  cy,
  payload,
}: {
  cx?: number;
  cy?: number;
  payload?: ChartRow;
}) {
  if (cx == null || cy == null) return null;

  const fill = payload?.belowTarget ? "#EF4444" : "#2563EB";

  return <circle cx={cx} cy={cy} r={5} fill={fill} stroke="#fff" strokeWidth={2} />;
}

function renderRateDot(props: { cx?: number; cy?: number; payload?: unknown }) {
  return (
    <RateDot
      cx={props.cx}
      cy={props.cy}
      payload={props.payload as ChartRow | undefined}
    />
  );
}

export function UploadQualityChart({
  buckets,
  granularity = "day",
  successRateTarget = 0.95,
  onBucketSelect,
}: UploadQualityChartProps) {
  const chartData = useMemo<ChartRow[]>(
    () =>
      buckets.map((bucket) => {
        const total = bucket.success + bucket.failed;
        const successRatePercent = total === 0 ? 0 : bucket.successRate * 100;
        return {
          ...bucket,
          label: formatActivityBucketLabel(bucket.date, granularity),
          total,
          successRatePercent,
          belowTarget: total > 0 && bucket.successRate < successRateTarget,
        };
      }),
    [buckets, granularity, successRateTarget],
  );

  const maxCount = useMemo(
    () => Math.max(1, ...chartData.map((row) => row.total)),
    [chartData],
  );

  if (chartData.length === 0) {
    return (
      <p className="text-sm text-slate-500 py-16 text-center">
        Chưa có upload hoàn tất trong khoảng thời gian này.
      </p>
    );
  }

  const targetPercent = Math.round(successRateTarget * 100);
  const hasAnomaly = chartData.some((row) => row.belowTarget);

  return (
    <div className="space-y-3">
      <ResponsiveContainer width="100%" height={380}>
        <ComposedChart data={chartData} margin={{ top: 12, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" vertical={false} />
          <XAxis
            dataKey="label"
            tick={{ fontSize: 12, fill: "#64748B" }}
            axisLine={{ stroke: "#CBD5E1" }}
            tickLine={false}
          />
          <YAxis
            yAxisId="count"
            allowDecimals={false}
            domain={[0, Math.ceil(maxCount * 1.15)]}
            tick={{ fontSize: 12, fill: "#64748B" }}
            axisLine={false}
            tickLine={false}
            width={36}
            label={{
              value: "Số lượt",
              angle: -90,
              position: "insideLeft",
              offset: 10,
              style: { fontSize: 11, fill: "#94A3B8" },
            }}
          />
          <YAxis
            yAxisId="rate"
            orientation="right"
            domain={[0, 100]}
            tick={{ fontSize: 12, fill: "#64748B" }}
            axisLine={false}
            tickLine={false}
            width={40}
            tickFormatter={(value) => `${value}%`}
            label={{
              value: "Tỷ lệ TC",
              angle: 90,
              position: "insideRight",
              offset: 10,
              style: { fontSize: 11, fill: "#94A3B8" },
            }}
          />
          <Tooltip
            content={(props) => (
              <ChartTooltip
                active={props.active}
                payload={props.payload}
                granularity={granularity}
                successRateTarget={successRateTarget}
              />
            )}
          />
          <Legend
            verticalAlign="top"
            height={36}
            formatter={(value) => {
              const labels: Record<string, string> = {
                success: "Thành công",
                failed: "Thất bại",
                successRatePercent: "Tỷ lệ thành công",
              };
              return <span className="text-sm text-slate-600">{labels[value] ?? value}</span>;
            }}
          />
          <ReferenceLine
            yAxisId="rate"
            y={targetPercent}
            stroke="#F59E0B"
            strokeDasharray="6 4"
            strokeWidth={2}
            label={{
              value: `Mục tiêu ${targetPercent}%`,
              position: "insideTopRight",
              fill: "#D97706",
              fontSize: 11,
            }}
          />
          <Bar
            yAxisId="count"
            dataKey="success"
            name="success"
            stackId="uploads"
            fill="#10B981"
            radius={[0, 0, 0, 0]}
            cursor={onBucketSelect ? "pointer" : "default"}
            maxBarSize={48}
          />
          <Bar
            yAxisId="count"
            dataKey="failed"
            name="failed"
            stackId="uploads"
            fill="#EF4444"
            radius={[4, 4, 0, 0]}
            cursor={onBucketSelect ? "pointer" : "default"}
            maxBarSize={48}
            onClick={(barData) => {
              const row = (barData as { payload?: ChartRow }).payload;
              if (row && row.failed > 0) onBucketSelect?.(row);
            }}
          />
          <Line
            yAxisId="rate"
            type="monotone"
            dataKey="successRatePercent"
            name="successRatePercent"
            stroke="#2563EB"
            strokeWidth={2.5}
            dot={renderRateDot}
            activeDot={renderRateDot}
          />
        </ComposedChart>
      </ResponsiveContainer>

      <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-xs text-slate-500 px-1">
        <span className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-sm bg-emerald-500" />
          Thành công
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-sm bg-red-500" />
          Thất bại — nhấp để drill-down
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-0.5 w-5 border-t-2 border-dashed border-amber-500" />
          Ngưỡng SLA {targetPercent}%
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-blue-600" />
          Tỷ lệ theo kỳ
        </span>
        {hasAnomaly && (
          <span className="inline-flex items-center gap-1.5 text-red-600 font-medium">
            <span className="h-2.5 w-2.5 rounded-full bg-red-500" />
            Điểm dưới mục tiêu
          </span>
        )}
      </div>
    </div>
  );
}
