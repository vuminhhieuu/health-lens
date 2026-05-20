"use client";

import { useMemo, useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { Calendar, Info, Loader2, TrendingDown, TrendingUp } from "lucide-react";

import { UploadVolumeBarChart } from "@/components/admin/UploadVolumeBarChart";
import { WauLineChart } from "@/components/admin/WauLineChart";
import { adminApiClient } from "@/lib/api/adminApiClient";
import {
  getActivityRangeError,
  isActivityRangeValid,
  MAX_ACTIVITY_DAYS,
  utcActivityDefaultFrom,
  utcActivityDefaultTo,
} from "@/lib/admin/activityAnalytics";
import { utcAnalyticsToday } from "@/lib/admin/userAnalytics";
import { API_ROUTES } from "@/lib/api/routes";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui";

type ActivitySummary = {
  wauCurrentWeek: number;
  wauPreviousWeek: number;
  wauChangePercent: number | null;
  uploadsInRange: number;
  uploadsPreviousRange: number;
  uploadChangePercent: number | null;
  uploadsCurrentWeek: number;
  uploadsPreviousWeek: number;
  uploadsWeekChangePercent: number | null;
};

type ActivityAnalyticsResponse = {
  summary: ActivitySummary;
  wauBuckets: { periodStart: string; wau: number }[];
  wauBucketsPrevious: { periodStart: string; wau: number }[];
  uploadBuckets: { periodStart: string; count: number; retryCount: number }[];
  uploadBucketsPrevious: { periodStart: string; count: number; retryCount: number }[];
};

function formatDisplayDate(isoDate: string) {
  try {
    return format(parseISO(isoDate), "dd/MM/yyyy");
  } catch {
    return isoDate;
  }
}

function formatChange(percent: number | null | undefined) {
  if (percent == null) return "—";
  const rounded = Math.round(percent * 10) / 10;
  return `${rounded > 0 ? "+" : ""}${rounded}%`;
}

function ChangeHint({
  percent,
  label,
}: {
  percent: number | null | undefined;
  label: string;
}) {
  if (percent == null) {
    return <p className="text-xs text-slate-400 mt-2">{label}</p>;
  }
  const positive = percent >= 0;
  return (
    <p
      className={`text-xs mt-2 flex items-center gap-1 ${
        positive ? "text-emerald-600" : "text-red-500"
      }`}
    >
      {positive ? (
        <TrendingUp className="h-3.5 w-3.5 shrink-0" />
      ) : (
        <TrendingDown className="h-3.5 w-3.5 shrink-0" />
      )}
      {formatChange(percent)} {label}
    </p>
  );
}

export function ActivityVolumePanel() {
  const [granularity, setGranularity] = useState<"day" | "week">("day");
  const [from, setFrom] = useState(() => utcActivityDefaultFrom());
  const [to, setTo] = useState(() => utcActivityDefaultTo());
  const [comparePrevious, setComparePrevious] = useState(true);

  const todayUtc = utcAnalyticsToday();
  const rangeValid = useMemo(() => isActivityRangeValid(from, to), [from, to]);
  const rangeError = useMemo(() => getActivityRangeError(from, to), [from, to]);
  const fromMax = to && to < todayUtc ? to : todayUtc;

  const queryKey = useMemo(
    () => ["admin-activity", from, to, granularity, comparePrevious],
    [from, to, granularity, comparePrevious],
  );

  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey,
    enabled: rangeValid,
    queryFn: async () => {
      const response = await adminApiClient.get<{ data: ActivityAnalyticsResponse }>(
        API_ROUTES.ADMIN_ANALYTICS.ACTIVITY,
        {
          params: { from, to, granularity, comparePrevious },
        },
      );
      return response.data.data;
    },
  });

  const hasWauActivity =
    data != null && data.wauBuckets.some((bucket) => bucket.wau > 0);
  const hasUploadActivity =
    data != null && data.uploadBuckets.some((bucket) => bucket.count > 0);

  const avgUploadsPerDay =
    data && granularity === "day" && data.uploadBuckets.length > 0
      ? Math.round((data.summary.uploadsInRange / data.uploadBuckets.length) * 10) / 10
      : null;

  return (
    <section
      id="activity-volume"
      className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden"
    >
      <div className="p-6 pb-4 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between border-b border-slate-100">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Hoạt động &amp; tải lên</h2>
          <p className="text-sm text-slate-500 mt-1">
            WAU và số lượt upload xác nhận theo thời gian (ngày lịch UTC)
          </p>
        </div>

        <div className="flex flex-col items-end gap-2">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex rounded-lg border border-slate-200 bg-slate-50 p-0.5">
            {(["day", "week"] as const).map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => setGranularity(value)}
                className={`px-4 py-2 text-sm font-medium rounded-md transition ${
                  granularity === value
                    ? "bg-white text-blue-600 shadow-sm"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                {value === "day" ? "Theo ngày" : "Theo tuần"}
              </button>
            ))}
          </div>

          <label className="relative flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 cursor-pointer">
            <span className="text-slate-400 shrink-0">Từ:</span>
            <span className="font-medium text-slate-800 tabular-nums">{formatDisplayDate(from)}</span>
            <Calendar className="h-4 w-4 text-slate-400 shrink-0" />
            <input
              type="date"
              value={from}
              max={fromMax}
              onChange={(event) => setFrom(event.target.value)}
              className="absolute inset-0 opacity-0 cursor-pointer"
              aria-label="Từ ngày (UTC)"
            />
          </label>

          <label className="relative flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 cursor-pointer">
            <span className="text-slate-400 shrink-0">Đến:</span>
            <span className="font-medium text-slate-800 tabular-nums">{formatDisplayDate(to)}</span>
            <Calendar className="h-4 w-4 text-slate-400 shrink-0" />
            <input
              type="date"
              value={to}
              min={from}
              max={todayUtc}
              onChange={(event) => setTo(event.target.value)}
              className="absolute inset-0 opacity-0 cursor-pointer"
              aria-label="Đến ngày (UTC)"
            />
          </label>

          <label className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={comparePrevious}
              onChange={(event) => setComparePrevious(event.target.checked)}
              className="rounded border-slate-300 text-teal-600 focus:ring-teal-500"
            />
            So sánh kỳ trước
          </label>

          {isFetching && <Loader2 className="h-4 w-4 animate-spin text-slate-400" />}
        </div>
          <p className="text-xs text-slate-400 flex items-center gap-1">
            <Info className="h-3.5 w-3.5 shrink-0" />
            Tối đa {MAX_ACTIVITY_DAYS} ngày · múi giờ UTC
          </p>
        </div>
      </div>

      <div className="p-6 space-y-8">
        {!rangeValid ? (
          <ErrorState
            title="Khoảng thời gian không hợp lệ"
            description={rangeError ?? "Khoảng thời gian không hợp lệ."}
            className="min-h-80"
          />
        ) : isLoading ? (
          <LoadingState title="Đang tải dữ liệu hoạt động" className="min-h-80" />
        ) : isError ? (
          <ErrorState
            title="Không tải được dữ liệu"
            description="Vui lòng thử lại để xem hoạt động và khối lượng tải lên."
            actionLabel="Thử lại"
            onAction={() => void refetch()}
            className="min-h-80"
          />
        ) : data ? (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <SummaryCard
                label="WAU tuần này"
                value={String(data.summary.wauCurrentWeek)}
                hint={
                  <ChangeHint
                    percent={data.summary.wauChangePercent}
                    label="so với tuần trước"
                  />
                }
              />
              <SummaryCard
                label="Upload (khoảng đã chọn)"
                value={String(data.summary.uploadsInRange)}
                hint={
                  <ChangeHint
                    percent={data.summary.uploadChangePercent}
                    label="so với kỳ trước"
                  />
                }
              />
              <SummaryCard
                label={granularity === "day" ? "TB upload / ngày" : "Upload tuần này"}
                value={
                  granularity === "day"
                    ? avgUploadsPerDay != null
                      ? String(avgUploadsPerDay)
                      : "—"
                    : String(data.summary.uploadsCurrentWeek)
                }
                hint={
                  granularity === "week" ? (
                    <ChangeHint
                      percent={data.summary.uploadsWeekChangePercent}
                      label="so với tuần trước"
                    />
                  ) : (
                    <p className="text-xs text-slate-400 mt-2">Trong khoảng đã chọn</p>
                  )
                }
              />
            </div>

            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-semibold text-slate-800">Người dùng hoạt động (WAU)</h3>
                <span
                  className="inline-flex items-center gap-1 text-xs text-slate-500"
                  title="WAU = user có ít nhất một API call đã đăng nhập trong tuần lịch. Luôn hiển thị theo tuần."
                >
                  <Info className="h-3.5 w-3.5" />
                  Theo tuần
                </span>
              </div>
              {granularity === "day" && (
                <p className="text-xs text-slate-500">
                  WAU luôn tính theo tuần — biểu đồ bên dưới không đổi khi chọn &quot;Theo ngày&quot; cho
                  upload.
                </p>
              )}
              <div className="rounded-xl border border-slate-100 bg-slate-50/40 p-4 sm:p-5">
                {!hasWauActivity ? (
                  <EmptyState
                    title="Chưa có hoạt động"
                    description="Cần sự kiện AUTHENTICATED_API_CALL từ người dùng đã đăng nhập."
                    className="min-h-48"
                  />
                ) : (
                  <WauLineChart
                    buckets={data.wauBuckets}
                    previousBuckets={data.wauBucketsPrevious}
                    comparePrevious={comparePrevious}
                  />
                )}
              </div>
            </div>

            <div className="space-y-3">
              <h3 className="text-sm font-semibold text-slate-800">Khối lượng upload (đã xác nhận)</h3>
              <div className="rounded-xl border border-slate-100 bg-slate-50/40 p-4 sm:p-5">
                {!hasUploadActivity ? (
                  <EmptyState
                    title="Chưa có upload"
                    description="Thử đổi khoảng thời gian hoặc kiểm tra lại khi có dữ liệu mới."
                    className="min-h-48"
                  />
                ) : (
                  <UploadVolumeBarChart
                    buckets={data.uploadBuckets}
                    previousBuckets={data.uploadBucketsPrevious}
                    granularity={granularity}
                    comparePrevious={comparePrevious}
                  />
                )}
              </div>
            </div>
          </>
        ) : null}
      </div>
    </section>
  );
}

function SummaryCard({
  label,
  value,
  hint,
}: {
  label: string;
  value: string;
  hint: ReactNode;
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className="text-4xl font-bold text-slate-900 mt-2 tabular-nums">{value}</p>
      {hint}
    </div>
  );
}
