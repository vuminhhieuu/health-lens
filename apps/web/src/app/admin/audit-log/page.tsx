"use client";

import { ClipboardList } from "lucide-react";

export default function AuditLogPage() {
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold flex items-center gap-3">
          <ClipboardList className="h-7 w-7 text-[#e94560]" />
          Audit Log
        </h1>
        <p className="text-gray-400 mt-1">
          Lịch sử thay đổi reference data — Sẽ được implement ở Story 7.5
        </p>
      </div>

      <div className="rounded-2xl bg-[#1a1a2e] border border-white/5 p-8 text-center text-gray-500">
        Chức năng này sẽ được phát triển trong Story 7.5.
      </div>
    </div>
  );
}
