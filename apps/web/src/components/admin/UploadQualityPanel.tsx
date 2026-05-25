"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { formatActivityBucketTooltip } from "@/lib/admin/utcPeriodLabels";
import { AlertCircle, Calendar, Info, Loader2, X } from "lucide-react";

import {
  UploadHistoryModal,
  type UploadHistoryFilter,
} from "@/components/admin/UploadHistoryModal";
import {
  UploadQualityChart,
  type UploadQualityBucket,
} from "@/components/admin/UploadQualityChart";
import {
  UploadQualityPieChart,
  type PieSlice,
} from "@/components/admin/UploadQualityDonutChart";
import { adminApiClient } from "@/lib/api/adminApiClient";
import {
  getUploadQualityRangeError,
  isUploadQualityRangeValid,
  utcUploadQualityDefaultFrom,
  utcUploadQualityDefaultTo,
} from "@/lib/admin/uploadQualityAnalytics";
import { utcAnalyticsToday } from "@/lib/admin/userAnalytics";
import { API_ROUTES } from "@/lib/api/routes";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui";

const SLA_TARGET = 0.95;

const FAILURE_COLORS: Record<string, string> = {
  timeout: "#F59E0B",
  low_confidence: "#8B5CF6",
  api_error: "#EF4444",
  invalid_file: "#64748B",
};

const FAILURE_LABELS: Record<string, string> = {
  timeout: "Timeout",
  low_confidence: "Độ tin cậy thấp",
  api_error: "Lỗi API",
  invalid_file: "Tệp không hợp lệ",
};

type UploadQualitySummary = {
  totalUploads: number;
  successCount: number;
  failedCount: number;
  successRate: number;
  failureRate: number;
};

type UploadQualityResponse = {
  summary: UploadQualitySummary;
  buckets: UploadQualityBucket[];
};

function formatRate(rate: number) {
  return `${Math.round(rate * 1000) / 10}%`;
}

function formatDisplayDate(isoDate: string) {
  try {
    return format(parseISO(isoDate), "dd/MM/yyyy");
  } catch {
    return isoDate;
  }
}

function formatBucketModalTitle(date: string, granularity: "day" | "week") {
  return formatActivityBucketTooltip(date, granularity);
}

function bucketToPieData(bucket: UploadQualityBucket): PieSlice[] {
  return Object.entries(bucket.failureBreakdown).map(([key, value]) => ({
    key,
    name: FAILURE_LABELS[key] ?? key,
    value,
    fill: FAILURE_COLORS[key] ?? "#94A3B8",
  }));
}

export function UploadQualityPanel() {
  const [granularity, setGranularity] = useState<"day" | "week">("day");
  const [from, setFrom] = useState(() => utcUploadQualityDefaultFrom());
  const [to, setTo] = useState(() => utcUploadQualityDefaultTo());
  const [selectedBucket, setSelectedBucket] = useState<UploadQualityBucket | null>(null);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [historyFilter, setHistoryFilter] = useState<UploadHistoryFilter>({});
  const [historyPage, setHistoryPage] = useState(0);
  const todayUtc = utcAnalyticsToday();
  const rangeValid = useMemo(() => isUploadQualityRangeValid(from, to), [from, to]);
  const rangeError = useMemo(() => getUploadQualityRangeError(from, to), [from, to]);
  const fromMax = to && to < todayUtc ? to : todayUtc;

  const openHistory = (filter: UploadHistoryFilter = {}) => {
    setHistoryFilter(filter);
    setHistoryPage(0);
    setHistoryOpen(true);
  };

  const queryKey = useMemo(() => ["admin-upload-quality", from, to, granularity], [from, to, granularity]);

  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey,
    enabled: rangeValid,
    queryFn: async () => {
      const response = await adminApiClient.get<{ data: UploadQualityResponse }>(
        API_ROUTES.ADMIN_ANALYTICS.UPLOAD_QUALITY,
        { params: { from, to, granularity } },
      );
      return response.data.data;
    },
  });

  const belowTarget =
    data != null && data.summary.totalUploads > 0 && data.summary.successRate < SLA_TARGET;

  const slaGapPercent =
    data != null && belowTarget
      ? Math.round((SLA_TARGET - data.summary.successRate) * 1000) / 10
      : 0;

  const breakdownPieData = useMemo(
    () => (selectedBucket ? bucketToPieData(selectedBucket) : []),
    [selectedBucket],
  );

  const anomalyBuckets = useMemo(
    () =>
      data?.buckets.filter(
        (b: UploadQualityBucket) => b.success + b.failed > 0 && b.successRate < SLA_TARGET,
      ) ?? [],
    [data],
  );

  return (
    <section
      id="upload-quality"
      className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden"
    >
      <div className="p-6 pb-4 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between border-b border-slate-100">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Chất lượng tải lên</h2>
          <p className="text-sm text-slate-500 mt-1">
            Xu hướng thành công/thất bại theo thời gian — phát hiện spike và drill-down theo kỳ
          </p>
        </div>

        <div className="flex flex-col items-end gap-2">
          <div className="flex flex-nowrap items-center gap-3 overflow-x-auto max-w-full">
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

            <label className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 cursor-pointer relative">
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

            {isFetching && <Loader2 className="h-4 w-4 animate-spin text-slate-400" />}
          </div>
          <p className="text-xs text-slate-400 flex items-center gap-1">
            <Info className="h-3.5 w-3.5 shrink-0" />
            Múi giờ UTC
          </p>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {!rangeValid ? (
          <ErrorState
            title="Khoảng thời gian không hợp lệ"
            description={rangeError ?? "Khoảng thời gian không hợp lệ."}
            className="min-h-80"
          />
        ) : isLoading ? (
          <LoadingState title="Đang tải dữ liệu chất lượng tải lên" className="min-h-80" />
        ) : isError ? (
          <ErrorState
            title="Không tải được dữ liệu"
            description="Vui lòng thử lại để xem chất lượng tải lên trong khoảng đã chọn."
            actionLabel="Thử lại"
            onAction={() => void refetch()}
            className="min-h-80"
          />
        ) : data ? (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <MetricCard
                label="Tổng OCR kết thúc"
                value={String(data.summary.totalUploads)}
                variant="neutral"
                onClick={() => openHistory()}
              />
              <MetricCard
                label="Tỉ lệ OCR thành công"
                value={formatRate(data.summary.successRate)}
                variant="success"
                onClick={() => openHistory({ status: "done" })}
              />
              <MetricCard
                label="Tỉ lệ OCR thất bại"
                value={formatRate(data.summary.failureRate)}
                variant="danger"
                onClick={() => openHistory({ status: "ocr_failed" })}
              />
            </div>

            {belowTarget && (
              <div className="flex items-start gap-3 rounded-lg border border-red-100 bg-red-50 px-4 py-3.5">
                <div className="w-1 self-stretch rounded-full bg-red-500 shrink-0" aria-hidden />
                <AlertCircle className="h-5 w-5 text-red-500 shrink-0 mt-0.5" />
                <div>
                  <p className="text-sm text-red-800 leading-relaxed font-medium">
                    Tỷ lệ thành công trung bình thấp hơn mục tiêu 95% trong khoảng đã chọn.
                  </p>
                  {anomalyBuckets.length > 0 && (
                    <p className="text-sm text-red-700/90 mt-1">
                      {anomalyBuckets.length} kỳ dưới ngưỡng trên biểu đồ — nhấp cột đỏ để điều tra.
                    </p>
                  )}
                </div>
              </div>
            )}

            {data.summary.totalUploads === 0 ? (
              <EmptyState
                title="Chưa có kết quả OCR trong khoảng"
                description="Thử đổi khoảng thời gian hoặc kiểm tra lại sau khi có phiên OCR kết thúc."
                className="min-h-72"
              />
            ) : (
              <div className="grid grid-cols-1 xl:grid-cols-[1fr_280px] gap-6 items-start">
                <div className="rounded-xl border border-slate-100 bg-slate-50/40 p-4 sm:p-5">
                  <UploadQualityChart
                    buckets={data.buckets}
                    granularity={granularity}
                    successRateTarget={SLA_TARGET}
                    onBucketSelect={(bucket) => setSelectedBucket(bucket)}
                  />
                </div>

                <SlaTargetCard
                  successRate={data.summary.successRate}
                  belowTarget={belowTarget}
                  gapPercent={slaGapPercent}
                  anomalyCount={anomalyBuckets.length}
                />
              </div>
            )}
          </>
        ) : null}
      </div>

      {selectedBucket && (
        <div
          className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="failure-breakdown-title"
          onClick={() => setSelectedBucket(null)}
        >
          <div
            className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="mb-4 flex items-start justify-between gap-4">
              <div>
                <h3 id="failure-breakdown-title" className="text-lg font-semibold text-slate-900">
                  Nguyên nhân thất bại
                </h3>
                <p className="text-sm text-slate-500">
                  {formatBucketModalTitle(selectedBucket.date, granularity)}
                  {" · "}
                  {selectedBucket.failed} lượt thất bại
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedBucket(null)}
                className="rounded-lg p-1 text-slate-500 hover:bg-slate-100"
                aria-label="Đóng"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {breakdownPieData.length === 0 ? (
              <p className="text-sm text-slate-500">Không có dữ liệu breakdown cho kỳ này.</p>
            ) : (
              <>
                <UploadQualityPieChart data={breakdownPieData} height={260} />
                <ul className="mt-4 space-y-1.5 text-sm text-slate-600">
                  {breakdownPieData.map((item) => (
                    <li key={item.key}>
                      <button
                        type="button"
                        className="flex w-full justify-between rounded-lg px-2 py-1.5 hover:bg-slate-50 text-left"
                        onClick={() => {
                          setSelectedBucket(null);
                          openHistory({ status: "ocr_failed", failureReason: item.key });
                        }}
                      >
                        <span className="inline-flex items-center gap-2">
                          <span
                            className="h-2.5 w-2.5 rounded-full shrink-0"
                            style={{ backgroundColor: item.fill }}
                          />
                          {item.name}
                        </span>
                        <span className="font-semibold text-slate-900">{item.value}</span>
                      </button>
                    </li>
                  ))}
                </ul>
                <button
                  type="button"
                  className="mt-4 w-full rounded-lg border border-slate-200 py-2 text-sm font-medium text-teal-700 hover:bg-teal-50"
                  onClick={() => {
                    setSelectedBucket(null);
                    openHistory({ status: "ocr_failed" });
                  }}
                >
                  Xem tất cả upload thất bại
                </button>
              </>
            )}
          </div>
        </div>
      )}

      <UploadHistoryModal
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        from={from}
        to={to}
        filter={historyFilter}
        page={historyPage}
        onPageChange={setHistoryPage}
      />
    </section>
  );
}

function MetricCard({
  label,
  value,
  variant,
  onClick,
}: {
  label: string;
  value: string;
  variant: "neutral" | "success" | "danger";
  onClick?: () => void;
}) {
  const styles = {
    neutral: {
      wrap: "bg-white border-slate-200",
      label: "text-slate-500",
      value: "text-slate-900",
    },
    success: {
      wrap: "bg-emerald-50/80 border-emerald-100",
      label: "text-emerald-700/80",
      value: "text-emerald-800",
    },
    danger: {
      wrap: "bg-red-50/80 border-red-100",
      label: "text-red-700/80",
      value: "text-red-800",
    },
  }[variant];

  const Component = onClick ? "button" : "div";

  return (
    <Component
      type={onClick ? "button" : undefined}
      onClick={onClick}
      className={`rounded-xl border p-5 text-left w-full transition ${
        onClick ? "hover:shadow-md hover:ring-2 hover:ring-teal-200/60 cursor-pointer" : ""
      } ${styles.wrap}`}
    >
      <p className={`text-xs font-semibold uppercase tracking-wide ${styles.label}`}>{label}</p>
      <p className={`text-4xl font-bold mt-2 tabular-nums ${styles.value}`}>{value}</p>
      {onClick && <p className="text-xs text-slate-400 mt-2">Bấm để xem lịch sử</p>}
    </Component>
  );
}

function SlaTargetCard({
  successRate,
  belowTarget,
  gapPercent,
  anomalyCount,
}: {
  successRate: number;
  belowTarget: boolean;
  gapPercent: number;
  anomalyCount: number;
}) {
  const currentPercent = Math.round(successRate * 1000) / 10;
  const targetPercent = SLA_TARGET * 100;
  const fillWidth = Math.min(100, Math.max(0, (currentPercent / targetPercent) * 100));

  return (
    <div className="rounded-2xl border border-blue-100 bg-blue-50/60 p-6 flex flex-col sticky top-4">
      <div className="flex items-center gap-2 text-blue-800">
        <Info className="h-4 w-4 shrink-0" />
        <span className="text-sm font-semibold">Mục tiêu SLA</span>
      </div>

      <p className="text-5xl font-bold text-blue-900 mt-4 tabular-nums">{targetPercent}%</p>
      <p className="text-sm text-slate-600 mt-2">
        Trung bình kỳ:{" "}
        <span className={`font-semibold ${belowTarget ? "text-red-600" : "text-emerald-700"}`}>
          {formatRate(successRate)}
        </span>
      </p>

      <div className="mt-5 h-2.5 w-full rounded-full bg-blue-100 overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${
            belowTarget ? "bg-red-500" : "bg-emerald-500"
          }`}
          style={{ width: `${fillWidth}%` }}
        />
      </div>

      {belowTarget ? (
        <>
          <p className="mt-4 flex items-center gap-2 text-sm font-semibold text-red-600">
            <AlertCircle className="h-4 w-4 shrink-0" />
            Dưới mức mục tiêu
          </p>
          <p className="mt-2 text-sm text-slate-600 leading-relaxed">
            Thiếu hụt{" "}
            <span className="font-semibold text-slate-800">{gapPercent}%</span> so với SLA phòng xét
            nghiệm.
          </p>
          {anomalyCount > 0 && (
            <p className="mt-3 text-xs text-amber-800 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2">
              {anomalyCount} kỳ có đường tỷ lệ (chấm đỏ) dưới ngưỡng {targetPercent}% trên biểu đồ.
            </p>
          )}
        </>
      ) : (
        <p className="mt-4 text-sm text-emerald-700 font-medium">Đạt mục tiêu SLA trong khoảng đã chọn</p>
      )}

      <p className="mt-auto pt-6 text-xs text-slate-500 leading-relaxed">
        Đường vàng nét đứt = ngưỡng {targetPercent}%. Cột xanh/đỏ = số lượt; đường xanh = tỷ lệ từng
        kỳ.
      </p>
    </div>
  );
}
