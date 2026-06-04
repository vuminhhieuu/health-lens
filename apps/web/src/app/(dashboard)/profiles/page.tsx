"use client";

import React, { Suspense, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Plus, AlertCircle, Mail, Trash2 } from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { notify } from "@/lib/notify";
import {
  mapIncomingInvitationsResponse,
  mapProfilesResponse,
  mapSharedProfilesResponse,
} from "@/lib/profileMappings";
import type { Profile } from "@/lib/profileMappings";
import {
  ProfileCard,
  HealthStatus,
} from "@/components/features/profiles/ProfileCard";
import { CreateProfileModal } from "@/components/features/profiles/CreateProfileModal";
import { EditProfileModal } from "@/components/features/profiles/EditProfileModal";
import { DeleteRecordModal } from "@/components/features/health-records/DeleteRecordModal";
import { MarkInboxReadFromUrl } from "@/components/features/notifications/MarkInboxReadFromUrl";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { useNotificationInbox } from "@/hooks/useNotificationInbox";
import { parseAcceptProfileInvitationResult } from "@/lib/sharing/acceptInvitationResult";
import { extractInvitationToken } from "@/lib/sharing/extractInvitationToken";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui";
import { CreateProfileInput, UpdateProfileInput } from "@healthlens/shared";

function mapSharedStatusToCardStatus(
  status?: string,
): HealthStatus | undefined {
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

export default function ProfilesPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { markAsReadAsync } = useNotificationInbox();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingProfileId, setEditingProfileId] = useState<string | null>(null);
  const [deletingProfileId, setDeletingProfileId] = useState<string | null>(
    null,
  );
  const {
    data: otherProfiles = [],
    isLoading: isProfilesLoading,
    isError: isProfilesError,
    refetch: refetchProfiles,
  } = useQuery({
    queryKey: ["profiles"],
    queryFn: async () => {
      const resp = await apiClient.get(API_ROUTES.PROFILES.BASE);
      return mapProfilesResponse(resp.data?.data);
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const {
    data: incomingInvitations = [],
    isLoading: isInvitationsLoading,
    isError: isInvitationsError,
    refetch: refetchIncomingInvitations,
  } = useQuery({
    queryKey: ["profile-invitations-incoming"],
    queryFn: async () => {
      const resp = await apiClient.get(ApiPaths.INVITATIONS.INCOMING);
      return mapIncomingInvitationsResponse(resp.data?.data);
    },
  });

  const {
    data: sharedProfiles = [],
    isLoading: isSharedProfilesLoading,
    isError: isSharedProfilesError,
    refetch: refetchSharedProfiles,
  } = useQuery({
    queryKey: ["shared-profiles"],
    queryFn: async () => {
      const resp = await apiClient.get(ApiPaths.SHARED_PROFILES.LIST);
      return mapSharedProfilesResponse(resp.data?.data);
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const rejectIncomingInvitationMutation = useMutation({
    mutationFn: async (invitationId: string) => {
      await apiClient.post(ApiPaths.INVITATIONS.REJECT(invitationId));
      return invitationId;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["profile-invitations-incoming"],
      });
      void queryClient.invalidateQueries({ queryKey: ["notification-inbox"] });
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Không thể từ chối lời mời lúc này."));
    },
  });

  const acceptIncomingInvitationMutation = useMutation({
    mutationFn: async ({
      acceptPath,
    }: {
      acceptPath: string;
      invitationId: string;
    }) => {
      const token = extractInvitationToken(acceptPath);
      if (!token) {
        throw new Error("invalid-token");
      }
      const response = await apiClient.post(ApiPaths.INVITATIONS.ACCEPT(token));
      const result = parseAcceptProfileInvitationResult(response.data?.data);
      if (!result) {
        throw new Error("invalid-result");
      }
      return result;
    },
    onMutate: async ({ invitationId }) => {
      await markAsReadAsync(`PROFILE_INVITATION:${invitationId}`);
    },
    onSuccess: (result) => {
      void queryClient.invalidateQueries({
        queryKey: ["profile-invitations-incoming"],
      });
      void queryClient.invalidateQueries({ queryKey: ["shared-profiles"] });
      void queryClient.invalidateQueries({ queryKey: ["notification-inbox"] });

      if (result.outcome === "accepted") {
        notify.success("Đã chấp nhận lời mời. Bạn có thể xem hồ sơ được chia sẻ.");
        const target =
          result.redirectUrl === "/" ? "/profiles" : result.redirectUrl;
        router.push(target);
        return;
      }

      if (result.outcome === "expired") {
        notify.error("Lời mời đã hết hạn.");
        return;
      }

      if (result.outcome === "require-login") {
        router.push(result.redirectUrl);
      }
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Không thể chấp nhận lời mời lúc này."));
    },
  });

  const createMutation = useMutation({
    mutationFn: async (data: CreateProfileInput) => {
      const resp = await apiClient.post(API_ROUTES.PROFILES.BASE, data);
      return resp.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profiles"] });
      setIsModalOpen(false);
      notify.success("Đã tạo hồ sơ sức khỏe thành công.");
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Đã xảy ra lỗi khi tạo hồ sơ."));
    },
  });

  const updateProfileMutation = useMutation({
    mutationFn: async ({
      profileId,
      data,
    }: {
      profileId: string;
      data: UpdateProfileInput;
    }) => {
      const payload = {
        ...data,
        birthDate: data.birthDate || null,
        gender: data.gender || null,
        notes: data.notes || null,
      };
      const response = await apiClient.put(
        API_ROUTES.PROFILES.UPDATE(profileId),
        payload,
      );
      const [updatedProfile] = mapProfilesResponse([response.data?.data]);
      if (!updatedProfile) {
        throw new Error("Invalid profile response");
      }
      return updatedProfile;
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Đã xảy ra lỗi khi cập nhật hồ sơ."));
    },
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData<Profile[]>(["profiles"], (old = []) =>
        old.map((profile) =>
          profile.id === updatedProfile.id ? updatedProfile : profile,
        ),
      );
      queryClient.invalidateQueries({ queryKey: ["profiles"] });
      queryClient.invalidateQueries({ queryKey: ["shared-profiles"] });
      queryClient.invalidateQueries({
        queryKey: ["profile", updatedProfile.id],
      });
      setIsEditModalOpen(false);
      setEditingProfileId(null);
      notify.success("Đã cập nhật hồ sơ sức khỏe thành công.");
    },
  });

  const deleteProfileMutation = useMutation({
    mutationFn: async (profileId: string) => {
      await apiClient.delete(ApiPaths.PROFILES.DELETE(profileId));
      return profileId;
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Đã xảy ra lỗi khi xóa hồ sơ."));
    },
    onSuccess: (deletedProfileId) => {
      queryClient.setQueryData<Profile[]>(["profiles"], (old = []) =>
        old.filter((profile) => profile.id !== deletedProfileId),
      );
      void queryClient.invalidateQueries({ queryKey: ["profiles"] });
      void queryClient.invalidateQueries({ queryKey: ["shared-profiles"] });
      if (editingProfileId === deletedProfileId) {
        setIsEditModalOpen(false);
        setEditingProfileId(null);
      }
      setDeletingProfileId(null);
      notify.success("Đã xóa hồ sơ gia đình.");
    },
  });

  const allProfiles = useMemo(() => {
    const familyProfiles = otherProfiles
      .filter((p) => !p.isDefault)
      .map((profile) => ({
        id: profile.id,
        cardKey: `owned-${profile.id}`,
        displayName: profile.displayName,
        relationship: "Người thân",
        notes: profile.notes,
        chronicConditions: profile.chronicConditions,
        currentMedications: profile.currentMedications,
        allergies: profile.allergies,
        latestStatus: mapSharedStatusToCardStatus(profile.latestStatus),
        lastUpdated: profile.updatedAt,
        lastRecordAt: profile.lastRecordAt,
        isSharedProfile: false,
        canEdit: true,
        canDelete: true,
        birthDate: profile.birthDate,
        gender: profile.gender,
      }));

    const sharedProfileCards = sharedProfiles.map((profile) => ({
      id: profile.profileId,
      cardKey: `shared-${profile.profileId}`,
      displayName: profile.displayName,
      relationship:
        profile.accessLevel === "edit"
          ? "Được chia sẻ (chỉnh sửa)"
          : profile.accessLevel === "unknown"
            ? "Được chia sẻ (quyền chưa xác định)"
          : "Được chia sẻ (chỉ xem)",
      notes: profile.notes,
      chronicConditions: profile.chronicConditions,
      currentMedications: profile.currentMedications,
      allergies: profile.allergies,
      latestStatus: mapSharedStatusToCardStatus(profile.latestStatus),
      lastUpdated: profile.lastUpdated,
      lastRecordAt: profile.lastRecordAt,
      isSharedProfile: true,
      canEdit: profile.accessLevel === "edit",
      canDelete: false,
      birthDate: profile.birthDate,
      gender: profile.gender,
    }));

    return [...sharedProfileCards, ...familyProfiles];
  }, [otherProfiles, sharedProfiles]);

  const editingProfile = useMemo(
    () =>
      allProfiles.find((profile) => profile.id === editingProfileId) ?? null,
    [allProfiles, editingProfileId],
  );
  const deletingProfile = useMemo(
    () =>
      allProfiles.find((profile) => profile.id === deletingProfileId) ?? null,
    [allProfiles, deletingProfileId],
  );

  const familyProfileCount = otherProfiles.filter((p) => !p.isDefault).length;
  const isLimitReached = familyProfileCount >= 10;

  const isLoading = isProfilesLoading || isInvitationsLoading || isSharedProfilesLoading;
  const hasProfileListError = isProfilesError || isInvitationsError;

  if (isLoading) {
    return (
      <div className="flex grow items-center justify-center bg-[#effcf9] p-8">
        <LoadingState
          title="Đang tải danh sách hồ sơ"
          description="HealthLens đang chuẩn bị hồ sơ của bạn và các hồ sơ được chia sẻ."
          className="w-full max-w-3xl"
        />
      </div>
    );
  }

  if (hasProfileListError) {
    return (
      <div className="flex grow items-center justify-center bg-[#effcf9] p-8">
        <ErrorState
          title="Không tải được danh sách hồ sơ"
          description="Vui lòng thử lại để tiếp tục quản lý hồ sơ sức khỏe."
          actionLabel="Thử lại"
          onAction={() => {
            void refetchProfiles();
            void refetchIncomingInvitations();
            void refetchSharedProfiles();
          }}
          className="w-full max-w-3xl"
        />
      </div>
    );
  }

  return (
    <DashboardPageShell
      title="Hồ sơ sức khỏe"
      subtitle="Quản lý hồ sơ của bạn và các thành viên trong gia đình ở một nơi thống nhất."
      actions={
        <div className="flex flex-wrap items-center justify-end gap-3">
          <button
            type="button"
            onClick={() => setIsModalOpen(true)}
            disabled={isLimitReached}
            className="flex items-center gap-2 rounded-2xl bg-linear-to-r from-[#00685f] to-[#008378] px-8 py-3 font-bold text-white shadow-lg shadow-[#00685f]/20 transition-all active:scale-95 disabled:pointer-events-none disabled:opacity-50"
          >
            <Plus size={20} />
            {isLimitReached ? "Đã đạt giới hạn" : "Tạo hồ sơ mới"}
          </button>
        </div>
      }
    >
      <Suspense fallback={null}>
        <MarkInboxReadFromUrl />
      </Suspense>

      {incomingInvitations.length > 0 ? (
        <div className="mb-8 flex flex-col gap-4">
          {incomingInvitations.map((inv) => (
            <div
              key={inv.id}
              className="flex flex-col gap-4 rounded-3xl border border-[#00685f]/20 bg-[#e9f6f3]/80 p-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="flex gap-4">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white text-[#00685f] shadow-sm">
                  <Mail className="h-6 w-6" />
                </div>
                <div>
                  <p className="font-black text-[#121e1c]">Lời mời xem hồ sơ</p>
                  <p className="mt-1 text-sm font-medium text-[#3d4947]">
                    <span className="font-bold text-[#005049]">
                      {inv.inviterName}
                    </span>{" "}
                    mời bạn xem hồ sơ{" "}
                    <span className="font-bold text-[#005049]">
                      {inv.profileDisplayName}
                    </span>
                    {inv.accessLevel === "edit" ? " (quyền chỉnh sửa)" : ""}.
                  </p>
                  <p className="mt-1 text-xs text-[#6d7a77]">
                    Kiểm tra email hoặc chấp nhận trực tiếp tại đây — cùng một
                    lời mời.
                  </p>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-3">
                <button
                  type="button"
                  onClick={() =>
                    rejectIncomingInvitationMutation.mutate(inv.id)
                  }
                  disabled={rejectIncomingInvitationMutation.isPending}
                  className="inline-flex items-center justify-center rounded-2xl border border-[#c5dfd9] bg-white px-6 py-3 text-sm font-bold text-[#3d4947] shadow-sm transition hover:bg-[#f6fbfa] disabled:opacity-60"
                >
                  Từ chối
                </button>
                <button
                  type="button"
                  onClick={() =>
                    acceptIncomingInvitationMutation.mutate({
                      acceptPath: inv.acceptPath,
                      invitationId: inv.id,
                    })
                  }
                  disabled={acceptIncomingInvitationMutation.isPending}
                  className="inline-flex items-center justify-center rounded-2xl bg-[#008378] px-6 py-3 text-sm font-bold text-white shadow-md transition hover:brightness-110 disabled:opacity-60"
                >
                  Chấp nhận và xem
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : null}

      {isSharedProfilesError ? (
        <div className="mb-8 flex items-start gap-4 rounded-3xl border border-[#f59e0b]/20 bg-[#fffbeb] p-5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#f59e0b]/10 text-[#92400e]">
            <AlertCircle size={22} />
          </div>
          <div>
            <h4 className="font-black text-[#92400e]">
              Không tải được hồ sơ được chia sẻ
            </h4>
            <p className="text-sm font-medium text-[#92400e]/80">
              Hồ sơ của bạn vẫn hiển thị bình thường. Vui lòng thử lại để cập nhật
              quyền chia sẻ mới nhất.
            </p>
            <button
              type="button"
              onClick={() => void refetchSharedProfiles()}
              className="mt-3 rounded-xl border border-[#f59e0b]/30 bg-white px-4 py-2 text-sm font-bold text-[#92400e] transition hover:bg-[#fff7df]"
            >
              Tải lại hồ sơ chia sẻ
            </button>
          </div>
        </div>
      ) : null}

      {allProfiles.length > 0 ? (
        <div className="grid grid-cols-1 gap-6 duration-500 animate-in slide-in-from-bottom-4 sm:grid-cols-2 lg:grid-cols-3">
          {allProfiles.map((profile) => (
            <ProfileCard
              key={profile.cardKey}
              name={profile.displayName}
              relationship={profile.relationship}
              lastRecordAt={profile.lastRecordAt}
              notes={
                profile.isSharedProfile
                  ? profile.notes ||
                    (profile.canEdit
                      ? "Bạn có thể chỉnh sửa dữ liệu hồ sơ này."
                      : "Bạn chỉ có quyền xem hồ sơ này.")
                  : profile.notes
              }
              latestStatus={profile.latestStatus}
              lastUpdated={profile.lastUpdated}
              onPress={() => {
                setEditingProfileId(profile.id);
                setIsEditModalOpen(true);
              }}
              secondaryAction={
                profile.canDelete
                  ? {
                      label: "Xóa",
                      icon: Trash2,
                      onClick: () => setDeletingProfileId(profile.id),
                    }
                  : undefined
              }
            />
          ))}
        </div>
      ) : (
        <EmptyState
          title="Chưa tìm thấy hồ sơ nào"
          description="Bắt đầu quản lý sức khỏe bằng cách thêm hồ sơ cho các thành viên trong gia đình."
          action={
            <button
              type="button"
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center gap-2 rounded-2xl border-2 border-[#00685f] px-8 py-3 font-bold text-[#00685f] transition-colors hover:bg-[#e9f6f3]"
            >
              Tạo hồ sơ đầu tiên
            </button>
          }
        />
      )}

      {isLimitReached && (
        <div className="mt-12 flex items-start gap-4 rounded-4xl border border-[#f59e0b]/20 bg-[#fffbeb] p-6">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#f59e0b]/10 text-[#92400e]">
            <AlertCircle size={24} />
          </div>
          <div>
            <h4 className="font-black text-[#92400e]">
              Bạn đã đạt giới hạn 10 hồ sơ người thân
            </h4>
            <p className="text-sm font-medium text-[#92400e]/80">
              Vui lòng liên hệ hỗ trợ hoặc nâng cấp tài khoản để quản lý nhiều
              hồ sơ hơn.
            </p>
          </div>
        </div>
      )}

      <CreateProfileModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={(data) => createMutation.mutate(data)}
        isLoading={createMutation.isPending}
      />

      <EditProfileModal
        isOpen={isEditModalOpen}
        profile={editingProfile}
        isReadOnly={editingProfile ? !editingProfile.canEdit : false}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingProfileId(null);
        }}
        onSubmit={(profileId, data) =>
          updateProfileMutation.mutate({ profileId, data })
        }
        isLoading={updateProfileMutation.isPending}
      />

      <DeleteRecordModal
        open={Boolean(deletingProfile)}
        title="Xóa hồ sơ gia đình?"
        description={
          deletingProfile
            ? `Bạn có chắc muốn xóa hồ sơ ${deletingProfile.displayName}? Các kết quả khám, lời mời và quyền chia sẻ liên quan đến hồ sơ này sẽ bị xóa theo.`
            : undefined
        }
        onCancel={() => {
          if (!deleteProfileMutation.isPending) {
            setDeletingProfileId(null);
          }
        }}
        onConfirm={() => {
          if (deletingProfile) {
            deleteProfileMutation.mutate(deletingProfile.id);
          }
        }}
        isPending={deleteProfileMutation.isPending}
        confirmLabel="Xóa hồ sơ"
        pendingLabel="Đang xóa hồ sơ..."
      />
    </DashboardPageShell>
  );
}
