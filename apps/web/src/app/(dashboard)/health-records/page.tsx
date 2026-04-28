"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Loader2, Users } from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { API_ROUTES } from "@/lib/api/routes";
import { ProfileCard, HealthStatus } from "@/components/features/profiles/ProfileCard";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { apiClient } from "@/lib/api/apiClient";

type Profile = {
  id: string;
  displayName: string;
  notes?: string;
  updatedAt?: string;
  latestStatus?: HealthStatus;
};

type UserProfile = {
  id: string;
  fullName: string;
};

export default function HealthRecordsPage() {
  const router = useRouter();
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

  const profileItems = useMemo(() => {
    const normalizedSelfName = currentUser?.fullName?.trim().toLowerCase();
    const selfProfile = profiles.find((profile) => {
      if (!normalizedSelfName) return false;
      return profile.displayName.trim().toLowerCase() === normalizedSelfName;
    });

    const familyProfiles = profiles
      .filter((profile) => profile.id !== selfProfile?.id)
      .map((profile) => ({
        ...profile,
        relationship: "Người thân",
      }));

    const selfItem = currentUser
      ? [{
          id: selfProfile?.id ?? "",
          displayName: currentUser.fullName || "Tôi",
          notes: selfProfile?.notes,
          updatedAt: selfProfile?.updatedAt,
          latestStatus: selfProfile?.latestStatus,
          relationship: "Chính chủ",
        }]
      : [];

    return [...selfItem, ...familyProfiles];
  }, [currentUser, profiles]);

  return (
    <DashboardPageShell
      title="Kết quả khám"
      subtitle="Chọn hồ sơ để xem lịch sử khám bệnh của từng thành viên."
    >
      {isLoading ? (
        <div className="flex justify-center py-10">
          <Loader2 className="h-8 w-8 animate-spin text-[#00685f]" />
        </div>
      ) : profileItems.length > 0 ? (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {profileItems.map((profile) => {
            const canOpenHistory = Boolean(profile.id);
            return (
              <ProfileCard
                key={`${profile.relationship}-${profile.displayName}-${profile.id || "no-id"}`}
                name={profile.displayName}
                relationship={profile.relationship}
                notes={profile.notes}
                latestStatus={profile.latestStatus}
                lastUpdated={profile.updatedAt}
                onPress={canOpenHistory ? () => router.push(`/profiles/${profile.id}/history`) : undefined}
              />
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
    </DashboardPageShell>
  );
}
