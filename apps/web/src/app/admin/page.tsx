"use client";

import { BarChart3 } from "lucide-react";

import { AdminGeneralStats } from "@/components/admin/AdminGeneralStats";
import { UploadQualityPanel } from "@/components/admin/UploadQualityPanel";
import { UserGrowthPanel } from "@/components/admin/UserGrowthPanel";

export default function AdminDashboardPage() {
  return (
    <div className="space-y-6 text-slate-900">
      <header className="flex flex-col gap-4 rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div className="min-w-0">
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-900">
            <BarChart3 className="h-7 w-7 shrink-0 text-teal-600" aria-hidden="true" />
            Thống kê
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-500">
            Tổng quan tăng trưởng người dùng và chất lượng tải lên
          </p>
        </div>
      </header>

      <UserGrowthPanel />
      <AdminGeneralStats />
      <UploadQualityPanel />
    </div>
  );
}
