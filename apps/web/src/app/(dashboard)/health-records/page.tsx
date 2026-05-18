"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Mail, Share2 } from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import {
  ProfileCard,
  HealthStatus,
} from "@/components/features/profiles/ProfileCard";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { apiClient } from "@/lib/api/apiClient";
import { notify } from "@/lib/notify";
import { InviteMemberModal } from "@/components/features/profiles/InviteMemberModal";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui";
import {
  buildSharedRecordProfileGroups,
  latestValidDateValue,
  mapRecordStateToCardStatus,
  sortHubCardsByLatestActivity,
  type HealthRecordHubCard,
  type SharedHealthRecord,
} from "@/lib/healthRecordHub";

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

type IncomingHealthRecordInvitation = {
  id: string;
  healthRecordId: string;
  profileId: string;
  inviterName: string;
  expiresAt?: string;
  createdAt?: string;
  acceptPath: string;
};

function mapSharedStatusToCardStatus(status?: string): HealthStatus | undefined {
  return mapRecordStateToCardStatus({ recordStatus: "done", overallStatus: status });
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

  const {
    data: profiles = [],
    isLoading: isProfilesLoading,
    isError: isProfilesError,
    refetch: refetchProfiles,
  } = useQuery({
    queryKey: ["profiles-for-health-records-hub"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const {
    data: sharedProfiles = [],
    isLoading: isSharedProfilesLoading,
    isError: isSharedProfilesError,
    refetch: refetchSharedProfiles,
  } = useQuery({
    queryKey: ["shared-profiles"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.SHARED_PROFILES.LIST);
      return (response.data?.data ?? []) as SharedProfile[];
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const {
    data: sharedHealthRecords = [],
    isLoading: isSharedHealthRecordsLoading,
    isError: isSharedHealthRecordsError,
    refetch: refetchSharedHealthRecords,
  } = useQuery({
    queryKey: ["shared-health-records"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.HEALTH_RECORDS.SHARED);
      return (response.data?.data ?? []) as SharedHealthRecord[];
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const { data: incomingHealthRecordInvitations = [] } = useQuery({
    queryKey: ["health-record-invitations-incoming"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.HEALTH_RECORD_INVITATIONS.INCOMING);
      return (response.data?.data ?? []) as IncomingHealthRecordInvitation[];
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
      notify.success(`Đã cập nhật quyền ${variables.accessLevel === "edit" ? "chỉnh sửa" : "chỉ xem"} cho ${variables.email}.`);
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Không thể cập nhật quyền truy cập."));
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
        notify.success("Đã gửi lời mời chia sẻ thành công.");
      }
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Không thể gửi lời mời chia sẻ."));
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
      notify.success("Đã thu hồi quyền truy cập thành công.");
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Không thể thu hồi quyền truy cập."));
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

  const profileItems = useMemo<HealthRecordHubCard[]>(() => {
    const selfProfile = profiles.find((profile) => profile.isDefault);

    const familyProfiles = profiles
      .filter((profile) => !profile.isDefault)
      .map((profile) => ({
        id: profile.id,
        displayName: profile.displayName,
        notes: profile.notes,
        updatedAt: profile.updatedAt,
        latestStatus: mapSharedStatusToCardStatus(profile.latestStatus),
        lastRecordAt: profile.lastRecordAt,
        relationship: "Người thân",
        isSharedProfile: false,
        href: `/profiles/${profile.id}/history`,
      }));

    const selfItem = selfProfile
      ? [{
          id: selfProfile.id,
          displayName: selfProfile.displayName,
          notes: selfProfile.notes,
          updatedAt: selfProfile.updatedAt,
          latestStatus: mapSharedStatusToCardStatus(selfProfile.latestStatus),
          lastRecordAt: selfProfile.lastRecordAt,
          relationship: "Chính chủ",
          isSharedProfile: false,
          href: `/profiles/${selfProfile.id}/history`,
        }]
      : [];

    const familyItems = familyProfiles;

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
      href: `/profiles/${profile.profileId}/history`,
    }));

    return sortHubCardsByLatestActivity([...selfItem, ...familyItems, ...sharedItems]);
  }, [profiles, sharedProfiles]);

  const sharedRecordItems = useMemo<HealthRecordHubCard[]>(() => {
    return sortHubCardsByLatestActivity(buildSharedRecordProfileGroups(sharedHealthRecords));
  }, [sharedHealthRecords]);

  const hubItems = useMemo(
    () => sortHubCardsByLatestActivity([...sharedRecordItems, ...profileItems]),
    [profileItems, sharedRecordItems]
  );
  const isSharedDataLoading = isSharedProfilesLoading || isSharedHealthRecordsLoading;
  const hasSharedDataError = isSharedProfilesError || isSharedHealthRecordsError;
  const hasHubItems = hubItems.length > 0;
  const isHubLoading = isProfilesLoading || (!hasHubItems && isSharedDataLoading);
  const shouldShowFullHubError = !hasHubItems && (isProfilesError || hasSharedDataError);

  return (
    <DashboardPageShell
      title="Kết quả khám"
      subtitle="Chọn hồ sơ để xem lịch sử khám bệnh của từng thành viên."
    >
      {incomingHealthRecordInvitations.length > 0 ? (
        <div className="mb-8 flex flex-col gap-4">
          {incomingHealthRecordInvitations.map((invitation) => (
            <div
              key={invitation.id}
              className="flex flex-col gap-4 rounded-3xl border border-[#00685f]/20 bg-[#e9f6f3]/80 p-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="flex gap-4">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white text-[#00685f] shadow-sm">
                  <Mail className="h-6 w-6" />
                </div>
                <div>
                  <p className="font-black text-[#121e1c]">Lời mời xem kết quả khám</p>
                  <p className="mt-1 text-sm font-medium text-[#3d4947]">
                    <span className="font-bold text-[#005049]">{invitation.inviterName}</span> đã mời bạn xem một kết
                    quả khám.
                  </p>
                  <p className="mt-1 text-xs text-[#6d7a77]">
                    Chấp nhận lời mời để mở trực tiếp kết quả được chia sẻ.
                  </p>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-3">
                <Link
                  href={invitation.acceptPath}
                  className="inline-flex items-center justify-center rounded-2xl bg-[#008378] px-6 py-3 text-sm font-bold text-white shadow-md transition hover:brightness-110"
                >
                  Chấp nhận và xem
                </Link>
              </div>
            </div>
          ))}
        </div>
      ) : null}

      {isHubLoading ? (
        <LoadingState
          title="Đang tải kết quả khám"
          description="Hệ thống đang đồng bộ hồ sơ và kết quả được chia sẻ."
        />
      ) : shouldShowFullHubError ? (
        <ErrorState
          title="Không thể tải kết quả khám"
          description="Vui lòng thử lại để cập nhật danh sách hồ sơ và kết quả được chia sẻ."
          actionLabel="Thử lại"
          onAction={() => {
            void refetchProfiles();
            void refetchSharedProfiles();
            void refetchSharedHealthRecords();
          }}
        />
      ) : hasHubItems ? (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {hubItems.map((profile) => {
            const canOpenHistory = Boolean(profile.id);
            const safeLastUpdated = latestValidDateValue(profile.updatedAt, profile.lastRecordAt);
            const safeLastRecordAt = latestValidDateValue(profile.lastRecordAt);
            return (
              <ProfileCard
                key={`${profile.relationship}-${profile.displayName}-${profile.id || "no-id"}`}
                name={profile.displayName}
                relationship={profile.relationship}
                notes={profile.notes}
                latestStatus={profile.latestStatus}
                lastUpdated={safeLastUpdated}
                lastRecordAt={safeLastRecordAt}
                onPress={
                  canOpenHistory
                    ? () => router.push(profile.href)
                    : undefined
                }
                secondaryAction={
                  canOpenHistory && !profile.isSharedProfile
                    ? {
                        label: "Chia sẻ",
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
        <EmptyState
          title="Chưa có hồ sơ nào"
          description="Vui lòng tạo hồ sơ để theo dõi lịch sử khám bệnh."
        />
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
