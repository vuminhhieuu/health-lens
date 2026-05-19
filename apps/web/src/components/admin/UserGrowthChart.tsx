"use client";

import { format, parse } from "date-fns";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export type MonthlyGrowthPoint = {
  month: string;
  newUsers: number;
};

type ChartRow = MonthlyGrowthPoint & {
  label: string;
};

type UserGrowthChartProps = {
  monthlyGrowth: MonthlyGrowthPoint[];
};

function formatMonthLabel(month: string) {
  try {
    const parsed = parse(month, "yyyy-MM", new Date());
    return format(parsed, "MM/yyyy");
  } catch {
    return month;
  }
}

function chartRowFromTooltipPayload(payload: unknown): ChartRow | null {
  if (!Array.isArray(payload) || payload.length === 0) return null;
  const item = payload[0] as { payload?: ChartRow };
  return item?.payload ?? null;
}

function ChartTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: unknown;
}) {
  if (!active) return null;
  const row = chartRowFromTooltipPayload(payload);
  if (!row) return null;

  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 shadow-md text-sm">
      <p className="font-medium text-slate-900">{formatMonthLabel(row.month)}</p>
      <p className="text-slate-600 mt-0.5">
        Người dùng mới: <span className="font-semibold text-teal-700 tabular-nums">{row.newUsers}</span>
      </p>
    </div>
  );
}

export function UserGrowthChart({ monthlyGrowth }: UserGrowthChartProps) {
  const chartData: ChartRow[] = monthlyGrowth.map((point) => ({
    ...point,
    label: formatMonthLabel(point.month),
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" vertical={false} />
        <XAxis
          dataKey="label"
          tick={{ fill: "#64748B", fontSize: 12 }}
          axisLine={{ stroke: "#E2E8F0" }}
          tickLine={false}
        />
        <YAxis
          allowDecimals={false}
          tick={{ fill: "#64748B", fontSize: 12 }}
          axisLine={false}
          tickLine={false}
          width={40}
        />
        <Tooltip
          content={(props) => (
            <ChartTooltip active={props.active} payload={props.payload} />
          )}
        />
        <Line
          type="monotone"
          dataKey="newUsers"
          stroke="#0D9488"
          strokeWidth={2}
          dot={{ fill: "#0D9488", r: 4 }}
          activeDot={{ r: 6 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
