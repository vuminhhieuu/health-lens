"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import type { PieLabelRenderProps } from "recharts";

export type PieSlice = {
  key: string;
  name: string;
  value: number;
  fill: string;
};

type UploadQualityDonutChartProps = {
  successCount: number;
  failedCount: number;
  height?: number;
  innerRadius?: string | number;
  showLegend?: boolean;
  centerLabel?: string;
  centerSubLabel?: string;
  onSliceClick?: (key: string) => void;
};

const SLICES = [
  { key: "success", name: "Thành công", fill: "#166534", labelColor: "#ffffff" },
  { key: "failed", name: "Thất bại", fill: "#B91C1C", labelColor: "#ffffff" },
] as const;

const RADIAN = Math.PI / 180;

function formatPercent(percent: number) {
  return `${(percent * 100).toFixed(1)}%`;
}

function renderInsidePercentLabel(props: PieLabelRenderProps) {
  const {
    cx = 0,
    cy = 0,
    midAngle = 0,
    innerRadius = 0,
    outerRadius = 0,
    percent = 0,
    payload,
  } = props;

  if (percent < 0.08) return null;

  const slice = payload as { labelColor?: string } | undefined;
  const textFill = slice?.labelColor ?? "#ffffff";

  const radius = Number(innerRadius) + (Number(outerRadius) - Number(innerRadius)) * 0.55;
  const angle = -midAngle * RADIAN;
  const x = Number(cx) + radius * Math.cos(angle);
  const y = Number(cy) + radius * Math.sin(angle);

  return (
    <text
      x={x}
      y={y}
      fill={textFill}
      textAnchor="middle"
      dominantBaseline="central"
      fontSize={12}
      fontWeight={600}
    >
      {formatPercent(percent)}
    </text>
  );
}

type TooltipPayloadItem = {
  name?: string;
  value?: number;
  payload?: { key?: string };
};

type UploadQualityTooltipProps = {
  active?: boolean;
  payload?: TooltipPayloadItem[];
  successCount: number;
  failedCount: number;
};

function UploadQualityTooltip({
  active,
  payload,
  successCount,
  failedCount,
}: UploadQualityTooltipProps) {
  if (!active) return null;

  const total = successCount + failedCount;
  const hovered = payload?.[0];
  const hoveredKey =
    (hovered?.payload as { key?: string } | undefined)?.key ??
    (hovered?.name === "Thành công" ? "success" : hovered?.name === "Thất bại" ? "failed" : null);

  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 shadow-md text-sm min-w-[160px]">
      {hoveredKey === "success" ? (
        <p className="font-semibold text-emerald-800">
          Thành công: {successCount}
          {total > 0 && (
            <span className="font-normal text-slate-500"> ({formatPercent(successCount / total)})</span>
          )}
        </p>
      ) : hoveredKey === "failed" ? (
        <p className="font-semibold text-red-700">
          Thất bại: {failedCount}
          {total > 0 && (
            <span className="font-normal text-slate-500"> ({formatPercent(failedCount / total)})</span>
          )}
        </p>
      ) : (
        <>
          <p className="font-semibold text-slate-800 mb-1.5">Chi tiết upload</p>
          <p className="text-emerald-800">
            Thành công: <span className="font-medium">{successCount}</span>
          </p>
          <p className="text-red-700 mt-1">
            Thất bại: <span className="font-medium">{failedCount}</span>
          </p>
        </>
      )}
    </div>
  );
}

function formatPieTooltip(value: unknown, name: unknown, total: number): [string, string] {
  const numericValue = typeof value === "number" ? value : Number(value ?? 0);
  const label = typeof name === "string" ? name : String(name ?? "");
  const pct = total === 0 ? 0 : Math.round((numericValue / total) * 10000) / 100;
  return [`${numericValue} lượt (${pct}%)`, label];
}

export function UploadQualityDonutChart({
  successCount,
  failedCount,
  height = 320,
  innerRadius = "58%",
  showLegend = true,
  centerLabel,
  centerSubLabel = "THÀNH CÔNG",
  onSliceClick,
}: UploadQualityDonutChartProps) {
  const total = successCount + failedCount;

  const pieData = [
    { ...SLICES[0], value: successCount },
    { ...SLICES[1], value: failedCount },
  ].filter((item) => item.value > 0);

  if (total === 0) {
    return (
      <div
        className="flex items-center justify-center text-xs text-slate-400"
        style={{ height }}
      >
        Chưa có dữ liệu
      </div>
    );
  }

  return (
    <div className="w-full">
      <div className="relative" style={{ height }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart margin={{ top: 4, right: 4, bottom: 4, left: 4 }}>
            <Pie
              data={pieData}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="50%"
              innerRadius={innerRadius}
              outerRadius="88%"
              startAngle={90}
              endAngle={-270}
              paddingAngle={pieData.length > 1 ? 2 : 0}
              stroke="#fff"
              strokeWidth={3}
              label={centerLabel ? false : renderInsidePercentLabel}
              labelLine={false}
              onClick={(_, index) => {
                const slice = pieData[index];
                if (slice) onSliceClick?.(slice.key);
              }}
              style={{ cursor: onSliceClick ? "pointer" : "default" }}
            >
              {pieData.map((entry) => (
                <Cell key={entry.key} fill={entry.fill} />
              ))}
            </Pie>
            <Tooltip
              content={(props) => (
                <UploadQualityTooltip
                  active={props.active}
                  payload={props.payload as unknown as TooltipPayloadItem[] | undefined}
                  successCount={successCount}
                  failedCount={failedCount}
                />
              )}
            />
          </PieChart>
        </ResponsiveContainer>
        {centerLabel && (
          <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center px-4">
            <p className="text-2xl sm:text-3xl font-bold text-slate-900 leading-tight">{centerLabel}</p>
            {centerSubLabel && (
              <p className="text-[10px] sm:text-xs font-bold tracking-widest text-slate-500 mt-1">
                {centerSubLabel}
              </p>
            )}
          </div>
        )}
      </div>
      {showLegend && (
        <div className="mt-4 flex justify-center gap-8 text-sm text-slate-600">
          <span className="inline-flex items-center gap-2">
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-800" />
            Thành công ({successCount})
          </span>
          <span className="inline-flex items-center gap-2">
            <span className="h-2.5 w-2.5 rounded-full bg-red-700" />
            Thất bại ({failedCount})
          </span>
        </div>
      )}
    </div>
  );
}

export function UploadQualityPieChart({
  data,
  height = 300,
  onSliceClick,
}: {
  data: PieSlice[];
  height?: number;
  onSliceClick?: (key: string) => void;
}) {
  const total = data.reduce((sum, item) => sum + item.value, 0);
  if (total === 0) {
    return (
      <div
        className="flex items-center justify-center text-sm text-slate-500"
        style={{ height }}
      >
        Không có dữ liệu
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart margin={{ top: 8, right: 8, bottom: 8, left: 8 }}>
        <Pie
          data={data}
          dataKey="value"
          nameKey="name"
          cx="50%"
          cy="50%"
          outerRadius="88%"
          startAngle={90}
          endAngle={-270}
          stroke="#fff"
          strokeWidth={2}
          label={renderInsidePercentLabel}
          labelLine={false}
          onClick={(_, index) => {
            const slice = data[index];
            if (slice) onSliceClick?.(slice.key);
          }}
        >
          {data.map((entry) => (
            <Cell key={entry.key} fill={entry.fill} />
          ))}
        </Pie>
        <Tooltip formatter={(value, name) => formatPieTooltip(value, name, total)} />
      </PieChart>
    </ResponsiveContainer>
  );
}
