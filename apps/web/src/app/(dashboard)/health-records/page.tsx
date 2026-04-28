"use client";

import { useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Loader2, User, Users, Check, FileText, Activity, ChevronRight, Calendar, Landmark } from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { UploadButton } from "@/components/features/upload/UploadButton";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { apiClient } from "@/lib/api/apiClient";

type Profile = {
  id: string;
  displayName: string;
};

type HealthRecord = {
  id: string;
  recordType?: string | null;
  status?: string | null;
  hospitalName?: string | null;
  examDate?: string | null;
  metrics?: Array<unknown> | null;
};

export default function HealthRecordsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [selectedProfileId, setSelectedProfileId] = useState<string>(() => searchParams.get("profileId") ?? "");
  const showRetryTips = searchParams.get("retry") === "1";
  const preselectedProfileId = searchParams.get("profileId");
  const isLabRecord = (recordType?: string | null) => {
    if (!recordType) return false;
    const normalized = recordType.toUpperCase();
    return normalized.includes("XET NGHIEM") || normalized.includes("XÉT NGHIỆM");
  };

  const { data: profiles = [], isLoading } = useQuery({
    queryKey: ["profiles-for-upload"],
    queryFn: async () => {
      // Trang "Hồ sơ gia đình" có thể hiển thị "Tôi" từ /users/me (không có trong bảng profiles).
      // Upload cần profileId thật → đảm bảo có ít nhất một hồ sơ DB trước khi lấy danh sách.
      await apiClient.post(ApiPaths.PROFILES.ENSURE_DEFAULT);
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
  });

  const selectableProfiles = useMemo(() => profiles, [profiles]);
  const activeProfileId = useMemo(() => {
    if (selectedProfileId && selectableProfiles.some((profile) => profile.id === selectedProfileId)) {
      return selectedProfileId;
    }
    if (preselectedProfileId && selectableProfiles.some((profile) => profile.id === preselectedProfileId)) {
      return preselectedProfileId;
    }
    return selectableProfiles[0]?.id || "";
  }, [preselectedProfileId, selectableProfiles, selectedProfileId]);

  const { data: records = [], isLoading: isLoadingRecords } = useQuery({
    queryKey: ["health-records", activeProfileId],
    queryFn: async () => {
      if (!activeProfileId) return [];
      const response = await apiClient.get(`${ApiPaths.HEALTH_RECORDS.BASE}/profiles/${activeProfileId}`);
      return response.data?.data ?? [];
    },
    enabled: !!activeProfileId,
  });

  return (
    <DashboardPageShell
      title="Kết quả khám và xét nghiệm"
      subtitle="Quản lý và theo dõi lịch sử khám bệnh của bạn và người thân."
    >

      <section className="rounded-3xl border border-[#b7d8d1] bg-white p-6 shadow-sm overflow-hidden">
        <div className="flex flex-col gap-6">
          {showRetryTips ? (
            <div className="rounded-2xl border border-[#e6b144] bg-[#fff4dd] px-4 py-3 text-sm text-[#825500]">
              <p className="font-semibold">Tips chup lai de OCR on dinh:</p>
              <p>- Dat giay phang, anh sang deu, tranh bong do, xoay ngang neu can.</p>
            </div>
          ) : null}
          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
            <div className="flex items-center gap-2">
              <Users className="h-5 w-5 text-[#00685f]" />
              <h3 className="font-bold text-[#005049]">Chọn hồ sơ thành viên</h3>
            </div>
            <UploadButton profileId={activeProfileId} />
          </div>

          <div className="flex flex-wrap gap-3">
            {selectableProfiles.map((profile) => {
              const isActive = activeProfileId === profile.id;
              return (
                <button
                  key={profile.id}
                  onClick={() => setSelectedProfileId(profile.id)}
                  className={`relative flex items-center gap-3 rounded-2xl border-2 px-5 py-3 transition-all duration-200 ${
                    isActive
                      ? "border-[#00685f] bg-[#effcf9] text-[#00685f] shadow-sm"
                      : "border-[#e0f0ed] bg-white text-[#4e6360] hover:border-[#b7d8d1]"
                  }`}
                >
                  <div className={`flex h-8 w-8 items-center justify-center rounded-full ${
                    isActive ? "bg-[#00685f] text-white" : "bg-[#effcf9] text-[#00685f]"
                  }`}>
                    <User className="h-4 w-4" />
                  </div>
                  <span className="font-bold">{profile.displayName}</span>
                  {isActive && (
                    <div className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-[#00685f] text-white shadow-sm ring-2 ring-white">
                      <Check className="h-3 w-3" />
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </section>

      <section className="mt-4 space-y-6">
        <h2 className="text-xl font-bold text-[#005049]">Lịch sử kết quả</h2>
        
        {isLoadingRecords || isLoading ? (
          <div className="flex justify-center py-10">
            <Loader2 className="h-8 w-8 animate-spin text-[#00685f]" />
          </div>
        ) : records.length === 0 ? (
          <div className="rounded-2xl border-2 border-dashed border-[#b7d8d1] bg-white/40 p-10 text-center">
            <p className="text-[#4e6360]">Chưa có kết quả khám nào cho hồ sơ này.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-5">
            {records.map((record: HealthRecord) => (
              <div 
                key={record.id} 
                className="group flex flex-col justify-between rounded-2xl border border-[#b7d8d1] bg-white p-7 shadow-sm transition-all duration-300 hover:border-[#00685f]/30 hover:shadow-md sm:flex-row sm:items-center"
              >
                <div className="flex gap-4 items-start">
                  <div className={`hidden sm:flex h-12 w-12 items-center justify-center rounded-xl ${
                    isLabRecord(record.recordType) ? "bg-[#effcf9] text-[#00685f]" : "bg-[#fff7ed] text-[#c2410c]"
                  }`}>
                    {isLabRecord(record.recordType) ? <Activity className="h-6 w-6" /> : <FileText className="h-6 w-6" />}
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-lg text-[#005049]">{record.recordType || "Phiếu khám bệnh"}</span>
                      <span className={`rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                        record.status === "done" ? "bg-[#ccfbf1] text-[#0f766e]" : "bg-[#fef3c7] text-[#92400e]"
                      }`}>
                        {record.status === "done" ? "Đã xác nhận" : "Đang chờ rà soát"}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-[#4e6360]">
                      <Landmark className="h-3.5 w-3.5 opacity-60" />
                      <p className="text-sm">{record.hospitalName || "Không rõ bệnh viện"}</p>
                    </div>
                    <div className="flex items-center gap-4 pt-2">
                      <div className="flex items-center gap-1.5 text-xs text-[#6d7a77]">
                        <Calendar className="h-3.5 w-3.5 opacity-60" />
                        <span>{record.examDate || "Ngày không xác định"}</span>
                      </div>
                      {record.metrics && (
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-[#00685f]">
                          <Check className="h-3.5 w-3.5" />
                          <span>{record.metrics.length} chỉ số</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
                <div className="mt-6 sm:mt-0">
                  <button 
                    onClick={() => router.push(`/health-records/review/${record.id}`)}
                    className="flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-white border-2 border-[#00685f] px-5 py-2.5 text-sm font-bold text-[#00685f] transition-all duration-200 hover:bg-[#00685f] hover:text-white"
                  >
                    Xem chi tiết
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </DashboardPageShell>
  );
}
