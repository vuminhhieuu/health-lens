"use client";

import { useQuery } from "@tanstack/react-query";
import { format, subDays } from "date-fns";
import { ImageIcon, TrendingDown, TrendingUp } from "lucide-react";

import { adminApiClient } from "@/lib/api/adminApiClient";
import { API_ROUTES } from "@/lib/api/routes";

const DEFAULT_FROM = format(subDays(new Date(), 29), "yyyy-MM-dd");
const DEFAULT_TO = format(new Date(), "yyyy-MM-dd");

function formatRate(rate: number) {
  return `${Math.round(rate * 1000) / 10}%`;
}

export function AdminGeneralStats() {
  const { data: qualityData } = useQuery({
    queryKey: ["admin-upload-quality-overview", DEFAULT_FROM, DEFAULT_TO],
    queryFn: async () => {
      const response = await adminApiClient.get<{
        data: { summary: { successRate: number } };
      }>(API_ROUTES.ADMIN_ANALYTICS.UPLOAD_QUALITY, {
        params: { from: DEFAULT_FROM, to: DEFAULT_TO, granularity: "day" },
      });
      return response.data.data.summary;
    },
  });

  const qualityRate =
    qualityData != null ? formatRate(qualityData.successRate) : "—";
  const meetsTarget = qualityData != null && qualityData.successRate >= 0.95;

  return (
    <section className="space-y-4">
      <h2 className="text-xs font-bold uppercase tracking-widest text-slate-400">
        Thống kê chung
      </h2>
      <div className="grid grid-cols-1 gap-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm flex items-start justify-between gap-4">
          <div className="min-w-0">
            <p className="text-sm text-slate-500">Chất lượng tải ảnh</p>
            <p className="text-3xl font-bold text-slate-900 mt-1 tabular-nums">{qualityRate}</p>
            <p
              className={`text-xs mt-2 flex items-center gap-1 ${
                qualityData == null
                  ? "text-slate-400"
                  : meetsTarget
                    ? "text-emerald-600"
                    : "text-red-500"
              }`}
            >
              {qualityData != null &&
                (meetsTarget ? (
                  <TrendingUp className="h-3.5 w-3.5 shrink-0" />
                ) : (
                  <TrendingDown className="h-3.5 w-3.5 shrink-0" />
                ))}
              30 ngày gần nhất
            </p>
          </div>
          <div className="shrink-0 flex h-12 w-12 items-center justify-center rounded-full bg-teal-50">
            <ImageIcon className="h-6 w-6 text-teal-700" />
          </div>
        </div>
      </div>
    </section>
  );
}
