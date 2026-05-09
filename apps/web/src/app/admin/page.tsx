"use client";

import { BarChart3 } from "lucide-react";

export default function AdminDashboardPage() {
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold flex items-center gap-3">
          <BarChart3 className="h-7 w-7 text-[#e94560]" />
          Thống kê tổng quan
        </h1>
        <p className="text-gray-400 mt-1">
          Dashboard quản trị — Sẽ được implement ở Story 8.x
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
          { label: "Người dùng", value: "—", desc: "Story 8.1" },
          { label: "Upload tuần này", value: "—", desc: "Story 8.2" },
          { label: "Tỷ lệ thành công", value: "—", desc: "Story 8.3" },
        ].map((stat) => (
          <div
            key={stat.label}
            className="rounded-2xl bg-[#1a1a2e] border border-white/5 p-6"
          >
            <p className="text-sm text-gray-400">{stat.label}</p>
            <p className="text-3xl font-bold mt-2">{stat.value}</p>
            <p className="text-xs text-gray-600 mt-1">{stat.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
