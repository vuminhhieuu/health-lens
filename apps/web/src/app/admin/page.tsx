"use client";

import { AdminGeneralStats } from "@/components/admin/AdminGeneralStats";
import { UploadQualityPanel } from "@/components/admin/UploadQualityPanel";

export default function AdminDashboardPage() {
  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <AdminGeneralStats />
      <UploadQualityPanel />
    </div>
  );
}
