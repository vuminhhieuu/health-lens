"use client";

import { useMemo } from "react";
import { formatActivityBucketLabel, formatActivityBucketTooltip } from "@/lib/admin/utcPeriodLabels";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export type UploadVolumeBucket = {
  periodStart: string;
  count: number;
  retryCount: number;
};

type ChartRow = UploadVolumeBucket & {
  label: string;
  previousCount?: number;
  previousRetryCount?: number;
};

type UploadVolumeBarChartProps = {
  buckets: UploadVolumeBucket[];
  previousBuckets?: UploadVolumeBucket[];
  granularity: "day" | "week";
  comparePrevious: boolean;
};

function VolumeTooltip({
  active,
  payload,
  granularity,
  comparePrevious,
}: {
  active?: boolean;
  payload?: { payload?: ChartRow }[];
  granularity: "day" | "week";
  comparePrevious: boolean;
}) {
  if (!active || !payload?.length) return null;
  const row = payload[0]?.payload;
  if (!row) return null;

  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 shadow-md text-sm">
      <p className="font-semibold text-slate-900 mb-1">
        {formatActivityBucketTooltip(row.periodStart, granularity)}
      </p>
      <p className="text-teal-700">
        Upload: <span className="font-semibold">{row.count}</span>
      </p>
      {comparePrevious && row.previousCount != null && (
        <p className="text-slate-500 mt-0.5">
          Kỳ trước: <span className="font-semibold">{row.previousCount}</span>
        </p>
      )}
      {row.retryCount > 0 && (
        <p className="text-slate-500 mt-0.5">
          Thử lại: <span className="font-semibold">{row.retryCount}</span>
        </p>
      )}
    </div>
  );
}

export function UploadVolumeBarChart({
  buckets,
  previousBuckets = [],
  granularity,
  comparePrevious,
}: UploadVolumeBarChartProps) {
  const rows = useMemo(() => {
    const previousByPeriod = new Map(previousBuckets.map((bucket) => [bucket.periodStart, bucket]));
    return buckets.map((bucket) => {
      const previous = comparePrevious ? previousByPeriod.get(bucket.periodStart) : undefined;
      return {
        ...bucket,
        label: formatActivityBucketLabel(bucket.periodStart, granularity),
        previousCount: previous?.count,
        previousRetryCount: previous?.retryCount,
      };
    });
  }, [buckets, previousBuckets, granularity, comparePrevious]);

  const showPrevious = comparePrevious && previousBuckets.length > 0;

  if (rows.length === 0) {
    return (
      <p className="text-sm text-slate-500 text-center py-16">
        Chưa có upload được xác nhận trong khoảng đã chọn.
      </p>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={rows} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" vertical={false} />
        <XAxis dataKey="label" tick={{ fontSize: 12, fill: "#64748B" }} />
        <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: "#64748B" }} width={40} />
        <Tooltip
          content={
            <VolumeTooltip granularity={granularity} comparePrevious={comparePrevious} />
          }
        />
        {showPrevious && (
          <Legend
            formatter={(value) => (value === "count" ? "Upload" : "Kỳ trước")}
            wrapperStyle={{ fontSize: 12 }}
          />
        )}
        <Bar dataKey="count" name="count" fill="#14B8A6" radius={[4, 4, 0, 0]} maxBarSize={showPrevious ? 32 : 48} />
        {showPrevious && (
          <Bar
            dataKey="previousCount"
            name="previousCount"
            fill="#94A3B8"
            radius={[4, 4, 0, 0]}
            maxBarSize={32}
          />
        )}
      </BarChart>
    </ResponsiveContainer>
  );
}
