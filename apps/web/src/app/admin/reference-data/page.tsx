"use client";

import { Database } from "lucide-react";

export default function ReferenceDataPage() {
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold flex items-center gap-3">
          <Database className="h-7 w-7 text-[#e94560]" />
          Reference Data
        </h1>
        <p className="text-gray-400 mt-1">
          Quản lý danh mục chỉ số và ngưỡng tham chiếu — Sẽ được implement ở Story 7.2-7.5
        </p>
      </div>

      <div className="rounded-2xl bg-[#1a1a2e] border border-white/5 p-8 text-center text-gray-500">
        Chức năng này sẽ được phát triển trong các story tiếp theo.
      </div>
    </div>
  );
}
