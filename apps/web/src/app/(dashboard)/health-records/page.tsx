"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { UploadButton } from "@/components/features/upload/UploadButton";
import { apiClient } from "@/lib/api/apiClient";

type Profile = {
  id: string;
  displayName: string;
};

export default function HealthRecordsPage() {
  const [selectedProfileId, setSelectedProfileId] = useState<string>("");

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
  const activeProfileId = selectedProfileId || selectableProfiles[0]?.id || "";

  return (
    <div className="mx-auto flex min-h-screen w-full max-w-4xl flex-col gap-6 bg-[#effcf9] px-6 py-10">
      <header>
        <h1 className="text-3xl font-bold text-[#005049]">Tải kết quả xét nghiệm</h1>
        <p className="mt-2 text-sm text-[#4e6360]">
          Tải file PDF hoặc ảnh (JPG/PNG, tối đa 20MB). Hệ thống sẽ xử lý OCR tự động.
        </p>
      </header>

      <section className="rounded-2xl border border-[#b7d8d1] bg-white p-6 shadow-sm">
        {isLoading ? (
          <div className="flex items-center gap-2 text-[#4e6360]">
            <Loader2 className="h-5 w-5 animate-spin" />
            Đang tải hồ sơ...
          </div>
        ) : selectableProfiles.length === 0 ? (
          <p className="text-sm text-[#ba1a1a]">Bạn chưa có hồ sơ để upload. Vui lòng tạo hồ sơ trước.</p>
        ) : (
          <div className="space-y-4">
            <label className="block text-sm font-medium text-[#3d4947]">Chọn hồ sơ</label>
            <select
              className="w-full rounded-xl border border-[#c5dfd9] px-3 py-2 outline-none focus:border-[#008378]"
              value={activeProfileId}
              onChange={(event) => setSelectedProfileId(event.target.value)}
            >
              {selectableProfiles.map((profile) => (
                <option key={profile.id} value={profile.id}>
                  {profile.displayName}
                </option>
              ))}
            </select>

            <UploadButton profileId={activeProfileId} />
          </div>
        )}
      </section>
    </div>
  );
}
