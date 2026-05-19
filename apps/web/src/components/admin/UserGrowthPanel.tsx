"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { Calendar, Info, Loader2, Users } from "lucide-react";

import { UserGrowthChart, type MonthlyGrowthPoint } from "@/components/admin/UserGrowthChart";
import { adminApiClient } from "@/lib/api/adminApiClient";
import { API_ROUTES } from "@/lib/api/routes";
import {
  getUserAnalyticsRangeError,
  isUserAnalyticsRangeValid,
  MAX_USER_ANALYTICS_MONTHS,
  utcAnalyticsDefaultFrom,
  utcAnalyticsDefaultTo,
  utcAnalyticsToday,
} from "@/lib/admin/userAnalytics";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui";

type UserAnalyticsResponse = {
  totalUsers: number;
  monthlyGrowth: MonthlyGrowthPoint[];
};

function formatDisplayDate(isoDate: string) {
  try {
    return format(parseISO(isoDate), "dd/MM/yyyy");
  } catch {
    return isoDate;
  }
}

export function UserGrowthPanel() {
  const [from, setFrom] = useState(() => utcAnalyticsDefaultFrom());
  const [to, setTo] = useState(() => utcAnalyticsDefaultTo());

  const todayUtc = utcAnalyticsToday();
  const rangeValid = useMemo(() => isUserAnalyticsRangeValid(from, to), [from, to]);
  const rangeError = useMemo(() => getUserAnalyticsRangeError(from, to), [from, to]);
  const fromMax = to && to < todayUtc ? to : todayUtc;

  const queryKey = useMemo(() => ["admin-user-analytics", from, to], [from, to]);

  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey,
    enabled: rangeValid,
    queryFn: async () => {
      const response = await adminApiClient.get<{ data: UserAnalyticsResponse }>(
        API_ROUTES.ADMIN_ANALYTICS.USERS,
        { params: { from, to } },
      );
      return response.data.data;
    },
  });

  const showChart = data != null && data.monthlyGrowth.length > 0;

  return (
    <section
      id="user-growth"
      className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden"
    >
      <div className="p-6 pb-4 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between border-b border-slate-100">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Tăng trưởng người dùng</h2>
          <p className="text-sm text-slate-500 mt-1">
            Tổng đăng ký (trừ tài khoản đã xóa) và xu hướng đăng ký mới theo tháng (UTC)
          </p>
        </div>

        <div className="flex flex-col items-end gap-2">
          <div className="flex flex-nowrap items-center gap-3 overflow-x-auto max-w-full">
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
                aria-label="Từ ngày"
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
                aria-label="Đến ngày"
              />
            </label>

            {isFetching && <Loader2 className="h-4 w-4 animate-spin text-slate-400" />}
          </div>
          <p className="text-xs text-slate-400 flex items-center gap-1">
            <Info className="h-3.5 w-3.5 shrink-0" />
            Tối đa {MAX_USER_ANALYTICS_MONTHS} tháng · múi giờ UTC
          </p>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {!rangeValid ? (
          <ErrorState
            title="Khoảng thời gian không hợp lệ"
            description={rangeError ?? "Khoảng thời gian không hợp lệ."}
            className="min-h-48"
          />
        ) : isLoading ? (
          <LoadingState title="Đang tải thống kê người dùng" className="min-h-80" />
        ) : isError ? (
          <ErrorState
            title="Không tải được dữ liệu"
            description="Vui lòng thử lại để xem tăng trưởng người dùng trong khoảng đã chọn."
            actionLabel="Thử lại"
            onAction={() => void refetch()}
            className="min-h-80"
          />
        ) : data ? (
          <>
            <div className="rounded-2xl border border-teal-100 bg-gradient-to-br from-teal-50 to-white p-6 flex items-center justify-between gap-4">
              <div>
                <p className="text-sm font-medium text-slate-600">Tổng người dùng đăng ký</p>
                <p className="text-4xl font-bold text-slate-900 mt-1 tabular-nums">
                  {data.totalUsers.toLocaleString("vi-VN")}
                </p>
                <p className="text-xs text-slate-500 mt-2">
                  Hiện tại · ROLE_USER, chưa xóa (ACTIVE và đang chờ xóa)
                </p>
              </div>
              <div className="shrink-0 flex h-14 w-14 items-center justify-center rounded-full bg-teal-100">
                <Users className="h-7 w-7 text-teal-700" />
              </div>
            </div>

            {showChart ? (
              <div className="rounded-xl border border-slate-100 bg-slate-50/40 p-4 sm:p-5">
                <p className="text-sm font-medium text-slate-700 mb-4">
                  Người dùng mới theo tháng
                </p>
                <UserGrowthChart monthlyGrowth={data.monthlyGrowth} />
              </div>
            ) : (
              <EmptyState
                title="Không có tháng trong khoảng đã chọn"
                description="Điều chỉnh bộ lọc để xem xu hướng tăng trưởng."
                className="min-h-48"
              />
            )}
          </>
        ) : null}
      </div>
    </section>
  );
}
