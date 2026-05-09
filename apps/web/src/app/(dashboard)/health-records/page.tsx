"use client";

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Loader2, Share2, Users } from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { API_ROUTES } from "@/lib/api/routes";
import { ProfileCard, HealthStatus } from "@/components/features/profiles/ProfileCard";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { apiClient } from "@/lib/api/apiClient";
import { InviteMemberModal } from "@/components/features/profiles/InviteMemberModal";

type Profile = {
  id: string;
  displayName: string;
  notes?: string;
  updatedAt?: string;
  latestStatus?: HealthStatus;
};

type ProfileInvitation = {
  id: string;
  email: string;
  status: string;
  accessLevel: "view" | "edit" | string;
};

type SharedProfile = {
  profileId: string;
  displayName: string;
  accessLevel: "view" | "edit" | string;
  latestStatus?: string;
  lastUpdated?: string;
};



function mapSharedStatusToCardStatus(status?: string): HealthStatus | undefined {
  if (!status) return undefined;
  const normalized = status.toLowerCase();
  if (normalized === "normal") return "normal";
  if (normalized === "attention" || normalized === "warning") return "warning";
  if (normalized === "abnormal") return "critical";
  return undefined;
}

function extractApiDetail(error: unknown, fallback: string): string {
  if (error && typeof error === "object" && "response" in error) {
    const axiosError = error as {
      response?: {
        data?: {
          detail?: string;
          title?: string;
          message?: string;
        };
      };
    };
    const data = axiosError.response?.data;
    return data?.detail ?? data?.title ?? data?.message ?? fallback;
  }
  return fallback;
}

type UserProfile = {
  id: string;
  fullName: string;
};

export default function HealthRecordsPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [inviteModalOpen, setInviteModalOpen] = useState(false);
  const [invitingProfileId, setInvitingProfileId] = useState<string | null>(null);
  const [pendingAccessUpdates, setPendingAccessUpdates] = useState<Record<string, "view" | "edit">>({});
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
  const { data: sharedProfiles = [] } = useQuery({
    queryKey: ["shared-profiles"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.SHARED_PROFILES.LIST);
      return (response.data?.data ?? []) as SharedProfile[];
    },
  });

  const inviteMutation = useMutation({
    mutationFn: async (payload: { email?: string; accessLevel?: "view" | "edit" }) => {
      if (!invitingProfileId) {
        throw new Error("Thiếu profile để chia sẻ.");
      }
      let invitedCount = 0;
      if (payload.email && payload.accessLevel) {
        await apiClient.post(ApiPaths.PROFILES.INVITATIONS(invitingProfileId), payload);
        invitedCount += 1;
      }
      const updates = Object.entries(pendingAccessUpdates);
      for (const [email, accessLevel] of updates) {
        await apiClient.post(ApiPaths.PROFILES.INVITATIONS(invitingProfileId), { email, accessLevel });
      }
      return {
        invitedCount,
        updatedCount: updates.length,
      };
    },
    onSuccess: (result) => {
      void queryClient.invalidateQueries({ queryKey: ["profile-shared-members", invitingProfileId] });
      setInviteModalOpen(false);
      setInvitingProfileId(null);
      setPendingAccessUpdates({});
      if (result.invitedCount > 0 && result.updatedCount > 0) {
        alert("Đã gửi lời mời và lưu thay đổi quyền truy cập thành công.");
        return;
      }
      if (result.invitedCount > 0) {
        alert("Đã gửi lời mời chia sẻ thành công.");
        return;
      }
      if (result.updatedCount > 0) {
        alert("Đã lưu thay đổi quyền truy cập thành công.");
      }
    },
    onError: (error: unknown) => {
      alert(extractApiDetail(error, "Không thể gửi lời mời chia sẻ."));
    },
  });

  const { data: sharedMembers = [], isFetching: isSharedMembersLoading } = useQuery({
    queryKey: ["profile-shared-members", invitingProfileId],
    enabled: inviteModalOpen && Boolean(invitingProfileId),
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.INVITATIONS(invitingProfileId as string));
      const invitations = (response.data?.data ?? []) as ProfileInvitation[];
      return invitations
        .filter((invitation) => invitation.status === "accepted")
        .map((invitation) => ({
          id: invitation.id,
          email: invitation.email,
          accessLevel: invitation.accessLevel,
        }));
    },
  });

  const displaySharedMembers = useMemo(
    () =>
      sharedMembers.map((member) => ({
        ...member,
        accessLevel: pendingAccessUpdates[member.email.toLowerCase()] ?? member.accessLevel,
      })),
    [pendingAccessUpdates, sharedMembers]
  );

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
          isSharedProfile: false,
        }]
      : [];

    const familyItems = familyProfiles.map((profile) => ({
      ...profile,
      isSharedProfile: false,
    }));

    const sharedItems = sharedProfiles.map((profile) => ({
      id: profile.profileId,
      displayName: profile.displayName,
      notes:
        profile.accessLevel === "edit"
          ? "Bạn có thể chỉnh sửa dữ liệu hồ sơ này."
          : "Bạn chỉ có quyền xem hồ sơ này.",
      updatedAt: profile.lastUpdated,
      latestStatus: mapSharedStatusToCardStatus(profile.latestStatus),
      relationship:
        profile.accessLevel === "edit"
          ? "Được chia sẻ (chỉnh sửa)"
          : "Được chia sẻ (chỉ xem)",
      isSharedProfile: true,
    }));

    return [...selfItem, ...familyItems, ...sharedItems];
  }, [currentUser, profiles, sharedProfiles]);

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
                secondaryAction={
                  canOpenHistory && !profile.isSharedProfile
                    ? {
                        label: "Share",
                        icon: Share2,
                        onClick: () => {
                          setInvitingProfileId(profile.id);
                          setInviteModalOpen(true);
                        },
                      }
                    : undefined
                }
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
      <InviteMemberModal
        isOpen={inviteModalOpen}
        onClose={() => {
          setInviteModalOpen(false);
          setInvitingProfileId(null);
          setPendingAccessUpdates({});
        }}
        isLoading={inviteMutation.isPending}
        sharedMembers={displaySharedMembers}
        isSharedMembersLoading={isSharedMembersLoading}
        hasPendingAccessChanges={Object.keys(pendingAccessUpdates).length > 0}
        onChangeAccessLevel={(member) =>
          setPendingAccessUpdates((prev) => ({
            ...prev,
            [member.email.toLowerCase()]: member.accessLevel,
          }))
        }
        title="Chia sẻ quyền xem kết quả khám"
        description="Nhập email người nhận."
        onSubmit={(payload) => inviteMutation.mutate(payload)}
      />
    </DashboardPageShell>
  );
}
