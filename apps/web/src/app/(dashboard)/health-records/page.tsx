"use client";

import { useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  Activity,
  Calendar,
  Check,
  ChevronRight,
  FileText,
  Landmark,
  Loader2,
  User,
  Users,
} from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { API_ROUTES } from "@/lib/api/routes";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { UploadButton } from "@/components/features/upload/UploadButton";
import { apiClient } from "@/lib/api/apiClient";

type Profile = {
  id: string;
  displayName: string;
};

type UserProfile = {
  fullName?: string;
};

type MetricDto = { status?: string | null };

type HealthRecordRow = {
  id: string;
  status: string;
  recordType?: string | null;
  hospitalName?: string | null;
  examDate?: string | null;
  metrics?: MetricDto[] | null;
};

export default function HealthRecordsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [selectedProfileId, setSelectedProfileId] = useState("");
  const showRetryTips = searchParams.get("retry") === "1";

  const isLabRecord = (recordType?: string | null) => {
    if (!recordType) return false;
    const normalized = recordType.toUpperCase();
    return normalized.includes("XET NGHIEM") || normalized.includes("XÉT NGHIỆM");
  };

  const { data: profiles = [], isLoading } = useQuery({
    queryKey: ["profiles-for-health-records-hub"],
    queryFn: async () => {
      await apiClient.post(ApiPaths.PROFILES.ENSURE_DEFAULT);
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
  });

  const { data: currentUser } = useQuery({
    queryKey: ["currentUser-for-health-records-hub"],
    queryFn: async () => {
      const response = await apiClient.get(API_ROUTES.USERS.ME);
      return response.data?.data as UserProfile;
    },
  });

  const selectableProfiles = useMemo(() => profiles, [profiles]);
  const activeProfileId = selectedProfileId || selectableProfiles[0]?.id || "";

  const { data: records = [], isLoading: isLoadingRecords } = useQuery({
    queryKey: ["health-records-list", activeProfileId],
    enabled: Boolean(activeProfileId),
    queryFn: async () => {
      const response = await apiClient.get(`${ApiPaths.HEALTH_RECORDS.BASE}/profiles/${activeProfileId}`);
      return (response.data?.data ?? []) as HealthRecordRow[];
    },
  });

  return (
    <DashboardPageShell
      title="Kết quả khám và xét nghiệm"
      subtitle="Quản lý và theo dõi lịch sử khám bệnh của bạn và người thân."
    >
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-6">
        {showRetryTips ? (
          <div className="rounded-2xl border border-[#e6b144] bg-[#fff4dd] px-4 py-3 text-sm text-[#825500]">
            <p className="font-semibold">Tips chụp lại để OCR ổn định:</p>
            <p>- Đặt giấy phẳng, ánh sáng đều, tránh bóng đổ, xoay ngang nếu cần.</p>
          </div>
        ) : null}

        <section className="rounded-3xl border border-[#b7d8d1] bg-white p-6 shadow-sm overflow-hidden">
          <div className="flex flex-col gap-6">
            <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
              <div className="flex items-center gap-2">
                <Users className="h-5 w-5 text-[#00685f]" />
                <h3 className="font-bold text-[#005049]">Chọn hồ sơ thành viên</h3>
              </div>
              <UploadButton profileId={activeProfileId} />
            </div>

            {isLoading ? (
              <div className="flex justify-center py-10">
                <Loader2 className="h-8 w-8 animate-spin text-[#00685f]" />
              </div>
            ) : selectableProfiles.length > 0 ? (
              <div className="flex flex-wrap gap-3">
                {selectableProfiles.map((profile) => {
                  const isActive = activeProfileId === profile.id;
                  const label =
                    currentUser?.fullName &&
                    profile.displayName.trim().toLowerCase() === currentUser.fullName.trim().toLowerCase()
                      ? `${profile.displayName} (Chính chủ)`
                      : profile.displayName;
                  return (
                    <button
                      key={profile.id}
                      type="button"
                      onClick={() => setSelectedProfileId(profile.id)}
                      className={`relative flex items-center gap-3 rounded-2xl border-2 px-5 py-3 transition-all duration-200 ${
                        isActive
                          ? "border-[#00685f] bg-[#effcf9] text-[#00685f] shadow-sm"
                          : "border-[#e0f0ed] bg-white text-[#4e6360] hover:border-[#b7d8d1]"
                      }`}
                    >
                      <div
                        className={`flex h-8 w-8 items-center justify-center rounded-full ${
                          isActive ? "bg-[#00685f] text-white" : "bg-[#effcf9] text-[#00685f]"
                        }`}
                      >
                        <User className="h-4 w-4" />
                      </div>
                      <span className="font-bold">{label}</span>
                      {isActive ? (
                        <div className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-[#00685f] text-white shadow-sm ring-2 ring-white">
                          <Check className="h-3 w-3" />
                        </div>
                      ) : null}
                    </button>
                  );
                })}
              </div>
            ) : (
              <div className="rounded-[32px] border-2 border-dashed border-[#bcc9c6]/30 bg-white/40 p-16 text-center">
                <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-full bg-[#e9f6f3] text-[#00685f]">
                  <Users className="h-10 w-10" />
                </div>
                <h3 className="text-2xl font-black text-[#121e1c]">Chưa có hồ sơ nào</h3>
                <p className="mt-2 text-[#6d7a77]">Vui lòng tạo hồ sơ để theo dõi lịch sử khám bệnh.</p>
              </div>
            )}
          </div>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-bold text-[#005049]">Lịch sử kết quả</h2>

          {isLoadingRecords || isLoading ? (
            <div className="flex justify-center py-10">
              <Loader2 className="h-8 w-8 animate-spin text-[#00685f]" />
            </div>
          ) : !activeProfileId ? (
            <div className="rounded-2xl border-2 border-dashed border-[#b7d8d1] bg-white/40 p-10 text-center">
              <p className="text-[#4e6360]">Chọn hồ sơ để xem kết quả.</p>
            </div>
          ) : records.length === 0 ? (
            <div className="rounded-2xl border-2 border-dashed border-[#b7d8d1] bg-white/40 p-10 text-center">
              <p className="text-[#4e6360]">Chưa có kết quả khám nào cho hồ sơ này.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4">
              {records.map((record) => (
                <div
                  key={record.id}
                  className="group flex flex-col sm:flex-row sm:items-center justify-between rounded-2xl border border-[#b7d8d1] bg-white p-6 shadow-sm transition-all duration-300 hover:border-[#00685f]/30 hover:shadow-md"
                >
                  <div className="flex gap-4 items-start">
                    <div
                      className={`hidden sm:flex h-12 w-12 items-center justify-center rounded-xl ${
                        isLabRecord(record.recordType) ? "bg-[#effcf9] text-[#00685f]" : "bg-[#fff7ed] text-[#c2410c]"
                      }`}
                    >
                      {isLabRecord(record.recordType) ? (
                        <Activity className="h-6 w-6" />
                      ) : (
                        <FileText className="h-6 w-6" />
                      )}
                    </div>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-lg text-[#005049]">{record.recordType || "Phiếu khám bệnh"}</span>
                        <span
                          className={`rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                            record.status === "done" ? "bg-[#ccfbf1] text-[#0f766e]" : "bg-[#fef3c7] text-[#92400e]"
                          }`}
                        >
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
                        {Array.isArray(record.metrics) ? (
                          <div className="flex items-center gap-1.5 text-xs font-semibold text-[#00685f]">
                            <Check className="h-3.5 w-3.5" />
                            <span>{record.metrics.length} chỉ số</span>
                          </div>
                        ) : null}
                      </div>
                    </div>
                  </div>
                  <div className="mt-6 sm:mt-0">
                    <button
                      type="button"
                      onClick={() =>
                        router.push(
                          record.status === "done"
                            ? `/records/${record.id}`
                            : `/health-records/review/${record.id}`
                        )
                      }
                      className="flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl border-2 border-[#00685f] bg-white px-5 py-2.5 text-sm font-bold text-[#00685f] transition-all duration-200 hover:bg-[#00685f] hover:text-white"
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
      </div>
    </DashboardPageShell>
  );
}
