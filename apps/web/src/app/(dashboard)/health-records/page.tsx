"use client";

import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Loader2, Share2, Users } from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import {
  ProfileCard,
  HealthStatus,
} from "@/components/features/profiles/ProfileCard";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { apiClient } from "@/lib/api/apiClient";
import { InviteMemberModal } from "@/components/features/profiles/InviteMemberModal";

type Profile = {
  id: string;
  displayName: string;
  notes?: string;
  updatedAt?: string;
  isDefault: boolean;
  latestStatus?: HealthStatus;
  lastRecordAt?: string;
};

type SharedMember = {
  id: string;
  viewerId: string;
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
  lastRecordAt?: string;
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

export default function HealthRecordsPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [inviteModalOpen, setInviteModalOpen] = useState(false);
  const [invitingProfileId, setInvitingProfileId] = useState<string | null>(null);
  useEffect(() => {
    void apiClient.post(ApiPaths.PROFILES.ENSURE_DEFAULT);
  }, []);

  const { data: profiles = [], isLoading } = useQuery({
    queryKey: ["profiles-for-health-records-hub"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const { data: sharedProfiles = [] } = useQuery({
    queryKey: ["shared-profiles"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.SHARED_PROFILES.LIST);
      return (response.data?.data ?? []) as SharedProfile[];
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const updateAccessMutation = useMutation({
    mutationFn: async (payload: { profileId: string; email: string; accessLevel: "view" | "edit" }) => {
      await apiClient.post(ApiPaths.PROFILES.INVITATIONS(payload.profileId), {
        email: payload.email,
        accessLevel: payload.accessLevel,
      });
    },
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["profile-shared-members", variables.profileId] });
      alert(`Đã cập nhật quyền ${variables.accessLevel === "edit" ? "chỉnh sửa" : "chỉ xem"} cho ${variables.email}.`);
    },
    onError: (error: unknown) => {
      alert(extractApiDetail(error, "Không thể cập nhật quyền truy cập."));
    },
  });

  const inviteMutation = useMutation({
    mutationFn: async (payload: { profileId: string; email?: string; accessLevel?: "view" | "edit" }) => {
      let invitedCount = 0;
      if (payload.email && payload.accessLevel) {
        await apiClient.post(ApiPaths.PROFILES.INVITATIONS(payload.profileId), {
          email: payload.email,
          accessLevel: payload.accessLevel,
        });
        invitedCount += 1;
      }
      return {
        invitedCount,
      };
    },
    onSuccess: (result, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["profile-shared-members", variables.profileId] });
      setInviteModalOpen(false);
      setInvitingProfileId(null);
      if (result.invitedCount > 0) {
        alert("Đã gửi lời mời chia sẻ thành công.");
      }
    },
    onError: (error: unknown) => {
      alert(extractApiDetail(error, "Không thể gửi lời mời chia sẻ."));
    },
  });

  const revokeShareMutation = useMutation({
    mutationFn: async (payload: { viewerId: string; email: string }) => {
      if (!invitingProfileId) {
        throw new Error("Thiếu profile để thu hồi quyền.");
      }
      await apiClient.delete(ApiPaths.PROFILES.REVOKE_SHARE(invitingProfileId, payload.viewerId));
      return payload.email;
    },
    onSuccess: (email) => {
      queryClient.setQueryData(
        ["profile-shared-members", invitingProfileId],
        (previous: SharedMember[] | undefined) => (previous ?? []).filter((member) => member.email.toLowerCase() !== email.toLowerCase())
      );
      void queryClient.invalidateQueries({ queryKey: ["profile-shared-members", invitingProfileId] });
      void queryClient.invalidateQueries({ queryKey: ["shared-profiles"] });
      alert("Đã thu hồi quyền truy cập thành công.");
    },
    onError: (error: unknown) => {
      alert(extractApiDetail(error, "Không thể thu hồi quyền truy cập."));
    },
  });

  const { data: sharedMembers = [], isFetching: isSharedMembersLoading } = useQuery({
    queryKey: ["profile-shared-members", invitingProfileId],
    enabled: inviteModalOpen && Boolean(invitingProfileId),
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.INVITATIONS(invitingProfileId as string));
      const invitations = (response.data?.data ?? []) as SharedMember[];
      return invitations
        .filter((invitation) => invitation.status === "accepted")
        .map((invitation) => ({
          id: invitation.id,
          viewerId: invitation.viewerId,
          email: invitation.email,
          accessLevel: invitation.accessLevel,
        }));
    },
  });

  const displaySharedMembers = sharedMembers;

  const profileItems = useMemo(() => {
    const selfProfile = profiles.find((profile) => profile.isDefault);

    const familyProfiles = profiles
      .filter((profile) => !profile.isDefault)
      .map((profile) => ({
        ...profile,
        relationship: "Người thân",
      }));

    const selfItem = selfProfile
      ? [{
          id: selfProfile.id,
          displayName: selfProfile.displayName,
          notes: selfProfile.notes,
          updatedAt: selfProfile.updatedAt,
          latestStatus: selfProfile.latestStatus,
          lastRecordAt: selfProfile.lastRecordAt,
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
      lastRecordAt: profile.lastRecordAt,
      relationship:
        profile.accessLevel === "edit"
          ? "Được chia sẻ (chỉnh sửa)"
          : "Được chia sẻ (chỉ xem)",
      isSharedProfile: true,
    }));

    return [...selfItem, ...familyItems, ...sharedItems];
  }, [profiles, sharedProfiles]);

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
                lastRecordAt={profile.lastRecordAt}
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
        <div className="rounded-4xl border-2 border-dashed border-[#bcc9c6]/30 bg-white/40 p-16 text-center">
          <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-full bg-[#e9f6f3] text-[#00685f]">
            <Users className="h-10 w-10" />
          </div>
          <h3 className="text-2xl font-black text-[#121e1c]">
            Chưa có hồ sơ nào
          </h3>
          <p className="mt-2 text-[#6d7a77]">
            Vui lòng tạo hồ sơ để theo dõi lịch sử khám bệnh.
          </p>
        </div>
      )}
      <InviteMemberModal
        isOpen={inviteModalOpen}
        onClose={() => {
          setInviteModalOpen(false);
          setInvitingProfileId(null);
        }}
        isLoading={inviteMutation.isPending || revokeShareMutation.isPending}
        sharedMembers={displaySharedMembers}
        isSharedMembersLoading={isSharedMembersLoading}
        isRevokingMember={revokeShareMutation.isPending}
        hasPendingAccessChanges={false}
        onRevokeMember={(member) => revokeShareMutation.mutate(member)}
        onChangeAccessLevel={(member) =>
          invitingProfileId &&
          updateAccessMutation.mutate({
            profileId: invitingProfileId,
            email: member.email,
            accessLevel: member.accessLevel,
          })
        }
        isUpdatingAccessLevel={updateAccessMutation.isPending}
        title="Chia sẻ quyền xem kết quả khám"
        description="Nhập email người nhận."
        onSubmit={(payload) =>
          invitingProfileId &&
          inviteMutation.mutate({
            ...payload,
            profileId: invitingProfileId,
          })
        }
      />
    </DashboardPageShell>
  );
}
