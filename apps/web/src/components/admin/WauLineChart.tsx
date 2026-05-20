"use client";

import { useMemo } from "react";
import { format, parseISO } from "date-fns";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export type WauBucket = {
  periodStart: string;
  wau: number;
};

type ChartRow = {
  periodStart: string;
  wau: number;
  label: string;
  previousWau?: number;
};

type WauLineChartProps = {
  buckets: WauBucket[];
  previousBuckets?: WauBucket[];
  comparePrevious: boolean;
};

function formatWeekLabel(periodStart: string) {
  try {
    return `T${format(parseISO(periodStart), "w")}`;
  } catch {
    return periodStart;
  }
}

function formatWeekTooltip(periodStart: string) {
  try {
    return `Tuần bắt đầu ${format(parseISO(periodStart), "dd/MM/yyyy")}`;
  } catch {
    return periodStart;
  }
}

function buildRows(current: WauBucket[], previous: WauBucket[]): ChartRow[] {
  const previousByPeriod = new Map(previous.map((bucket) => [bucket.periodStart, bucket.wau]));
  return current.map((bucket) => ({
    periodStart: bucket.periodStart,
    wau: bucket.wau,
    label: formatWeekLabel(bucket.periodStart),
    previousWau: previousByPeriod.get(bucket.periodStart),
  }));
}

function WauTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { payload?: ChartRow }[];
}) {
  if (!active || !payload?.length) return null;
  const row = payload[0]?.payload;
  if (!row) return null;

  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 shadow-md text-sm">
      <p className="font-semibold text-slate-900 mb-1">{formatWeekTooltip(row.periodStart)}</p>
      <p className="text-teal-700">
        WAU: <span className="font-semibold">{row.wau}</span> người
      </p>
      {row.previousWau != null && (
        <p className="text-slate-500 mt-0.5">
          Kỳ trước: <span className="font-semibold">{row.previousWau}</span>
        </p>
      )}
    </div>
  );
}

export function WauLineChart({ buckets, previousBuckets = [], comparePrevious }: WauLineChartProps) {
  const rows = useMemo(
    () => buildRows(buckets, comparePrevious ? previousBuckets : []),
    [buckets, previousBuckets, comparePrevious],
  );

  if (rows.length === 0) {
    return (
      <p className="text-sm text-slate-500 text-center py-16">
        Chưa có hoạt động người dùng trong khoảng đã chọn.
      </p>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={280}>
      <LineChart data={rows} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" vertical={false} />
        <XAxis dataKey="label" tick={{ fontSize: 12, fill: "#64748B" }} />
        <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: "#64748B" }} width={40} />
        <Tooltip content={<WauTooltip />} />
        {comparePrevious && previousBuckets.length > 0 && (
          <Legend
            formatter={(value) => (value === "wau" ? "WAU" : "Kỳ trước")}
            wrapperStyle={{ fontSize: 12 }}
          />
        )}
        <Line
          type="monotone"
          dataKey="wau"
          name="wau"
          stroke="#0D9488"
          strokeWidth={2}
          dot={{ r: 3, fill: "#0D9488" }}
          activeDot={{ r: 5 }}
        />
        {comparePrevious && previousBuckets.length > 0 && (
          <Line
            type="monotone"
            dataKey="previousWau"
            name="previousWau"
            stroke="#94A3B8"
            strokeWidth={2}
            strokeDasharray="4 4"
            dot={false}
            connectNulls
          />
        )}
      </LineChart>
    </ResponsiveContainer>
  );
}
