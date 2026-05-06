"use client";

import React, { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Plus,
  Users,
  Search,
  Filter,
  Loader2,
  AlertCircle,
  Mail,
} from "lucide-react";
import Link from "next/link";

import { ApiPaths } from "@healthlens/shared/constants";

import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { ProfileCard, HealthStatus } from "@/components/features/profiles/ProfileCard";
import { CreateProfileModal } from "@/components/features/profiles/CreateProfileModal";
import { EditProfileModal } from "@/components/features/profiles/EditProfileModal";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { CreateProfileInput, UpdateProfileInput } from "@healthlens/shared";

type Profile = {
  id: string;
  displayName: string;
  birthDate?: string;
  gender?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  latestStatus?: HealthStatus;
};

type UserProfile = {
  id: string;
  fullName: string;
  email: string;
  birthDate: string;
  gender: string;
};

type IncomingInvitation = {
  id: string;
  profileId: string;
  profileDisplayName: string;
  inviterName: string;
  expiresAt: string;
  createdAt: string;
  accessLevel: string;
  acceptPath: string;
};

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
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingProfileId, setEditingProfileId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  const { data: currentUser, isLoading: isUserLoading } = useQuery({
    queryKey: ["currentUser"],
    queryFn: async () => {
      const resp = await apiClient.get(API_ROUTES.USERS.ME);
      return resp.data.data as UserProfile;
    },
  });

  const { data: otherProfiles = [], isLoading: isProfilesLoading } = useQuery({
    queryKey: ["profiles"],
    queryFn: async () => {
      const resp = await apiClient.get(API_ROUTES.PROFILES.BASE);
      return resp.data.data as Profile[];
    },
  });

  const { data: incomingInvitations = [] } = useQuery({
    queryKey: ["profile-invitations-incoming"],
    queryFn: async () => {
      const resp = await apiClient.get(ApiPaths.INVITATIONS.INCOMING);
      return (resp.data?.data ?? []) as IncomingInvitation[];
    },
  });

  const rejectIncomingInvitationMutation = useMutation({
    mutationFn: async (invitationId: string) => {
      await apiClient.post(ApiPaths.INVITATIONS.REJECT(invitationId));
      return invitationId;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["profile-invitations-incoming"] });
    },
    onError: (error: unknown) => {
      alert(extractApiDetail(error, "Không thể từ chối lời mời lúc này."));
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
    },
    onError: (error: unknown) => {
      alert(extractApiDetail(error, "Đã xảy ra lỗi khi tạo hồ sơ."));
    },
  });

  const updateProfileMutation = useMutation({
    mutationFn: async ({ profileId, data }: { profileId: string; data: UpdateProfileInput }) => {
      const payload = {
        ...data,
        birthDate: data.birthDate || null,
        gender: data.gender || null,
        notes: data.notes || null,
      };
      const response = await apiClient.put(API_ROUTES.PROFILES.UPDATE(profileId), payload);
      return response.data.data as Profile;
    },
    onMutate: async ({ profileId, data }) => {
      await queryClient.cancelQueries({ queryKey: ["profiles"] });

      const previousProfiles = queryClient.getQueryData<Profile[]>(["profiles"]);
      queryClient.setQueryData<Profile[]>(["profiles"], (old = []) =>
        old.map((profile) => {
          if (profile.id !== profileId) {
            return profile;
          }

          return {
            ...profile,
            displayName: data.displayName.trim(),
            birthDate: data.birthDate || undefined,
            gender: data.gender || undefined,
            notes: data.notes?.trim() || undefined,
            updatedAt: new Date().toISOString(),
          };
        }),
      );

      return { previousProfiles };
    },
    onError: (error: unknown, _variables, context) => {
      if (context?.previousProfiles) {
        queryClient.setQueryData(["profiles"], context.previousProfiles);
      }

      alert(extractApiDetail(error, "Đã xảy ra lỗi khi cập nhật hồ sơ."));
    },
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData<Profile[]>(["profiles"], (old = []) =>
        old.map((profile) => (profile.id === updatedProfile.id ? updatedProfile : profile)),
      );
      queryClient.invalidateQueries({ queryKey: ["profiles"] });
      queryClient.invalidateQueries({ queryKey: ["profile", updatedProfile.id] });
      setIsEditModalOpen(false);
      setEditingProfileId(null);
    },
  });

  const allProfiles = useMemo(() => {
    const normalizedSelfName = currentUser?.fullName.trim().toLowerCase();

    return otherProfiles
      .filter((profile) => {
        const normalizedProfileName = profile.displayName.trim().toLowerCase();
        const isSelfProfile = !!normalizedSelfName && normalizedProfileName === normalizedSelfName;
        return !isSelfProfile;
      })
      .map((profile) => ({
        ...profile,
        relationship: "Người thân",
        isSelf: false,
      }))
      .filter((profile) => profile.displayName.toLowerCase().includes(searchQuery.toLowerCase()));
  }, [currentUser, otherProfiles, searchQuery]);

  const editingProfile = useMemo(
    () => otherProfiles.find((profile) => profile.id === editingProfileId) ?? null,
    [otherProfiles, editingProfileId],
  );

  const isLimitReached = otherProfiles.length >= 10;

  const isLoading = isUserLoading || isProfilesLoading;

  if (isLoading) {
    return (
      <div className="flex grow items-center justify-center bg-[#effcf9] p-8">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-10 w-10 animate-spin text-[#00685f]" />
          <p className="font-bold text-[#6d7a77]">Đang tải danh sách hồ sơ...</p>
        </div>
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
                    <span className="font-bold text-[#005049]">{inv.inviterName}</span> mời bạn xem hồ sơ{" "}
                    <span className="font-bold text-[#005049]">{inv.profileDisplayName}</span>
                    {inv.accessLevel === "edit" ? " (quyền chỉnh sửa)" : ""}.
                  </p>
                  <p className="mt-1 text-xs text-[#6d7a77]">
                    Kiểm tra email hoặc chấp nhận trực tiếp tại đây — cùng một lời mời.
                  </p>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-3">
                <button
                  type="button"
                  onClick={() => rejectIncomingInvitationMutation.mutate(inv.id)}
                  disabled={rejectIncomingInvitationMutation.isPending}
                  className="inline-flex items-center justify-center rounded-2xl border border-[#c5dfd9] bg-white px-6 py-3 text-sm font-bold text-[#3d4947] shadow-sm transition hover:bg-[#f6fbfa] disabled:opacity-60"
                >
                  Từ chối
                </button>
                <Link
                  href={inv.acceptPath}
                  className="inline-flex items-center justify-center rounded-2xl bg-[#008378] px-6 py-3 text-sm font-bold text-white shadow-md transition hover:brightness-110"
                >
                  Chấp nhận và xem
                </Link>
              </div>
            </div>
          ))}
        </div>
      ) : null}

      <div className="mb-8 flex flex-col gap-4 sm:flex-row">
        <div className="relative grow">
          <Search className="absolute top-1/2 left-4 h-5 w-5 -translate-y-1/2 text-[#6d7a77]" />
          <input
            type="text"
            placeholder="Tìm kiếm hồ sơ..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-14 w-full rounded-2xl border-2 border-transparent bg-white/60 pr-4 pl-12 font-medium text-[#121e1c] transition-all outline-none focus:border-[#00685f]/20 focus:bg-white"
          />
        </div>
        <button
          type="button"
          className="flex h-14 items-center gap-2 rounded-2xl border-2 border-transparent bg-white/60 px-6 font-bold text-[#3d4947] transition-all hover:border-[#bcc9c6]/20 hover:bg-white"
        >
          <Filter size={18} />
          Sắp xếp
        </button>
      </div>

      {allProfiles.length > 0 ? (
        <div className="grid grid-cols-1 gap-6 duration-500 animate-in slide-in-from-bottom-4 sm:grid-cols-2 lg:grid-cols-3">
          {allProfiles.map((profile) => (
            <ProfileCard
              key={profile.id}
              name={profile.displayName}
              relationship={profile.relationship}
              notes={profile.notes}
              latestStatus={profile.latestStatus}
              lastUpdated={profile.updatedAt}
              onPress={
                profile.isSelf
                  ? undefined
                  : () => {
                      setEditingProfileId(profile.id);
                      setIsEditModalOpen(true);
                    }
              }
            />
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center rounded-[40px] border-2 border-dashed border-[#bcc9c6]/30 bg-white/40 p-20 text-center">
          <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-[#e9f6f3] text-[#00685f]">
            <Users size={48} />
          </div>
          <h3 className="mb-2 text-2xl font-black text-[#121e1c]">Chưa tìm thấy hồ sơ nào</h3>
          <p className="mb-8 max-w-xs font-medium text-[#6d7a77]">
            Bắt đầu quản lý sức khỏe bằng cách thêm hồ sơ cho các thành viên trong gia đình.
          </p>
          <button
            type="button"
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 rounded-2xl border-2 border-[#00685f] px-8 py-3 font-bold text-[#00685f] transition-colors hover:bg-[#e9f6f3]"
          >
            Tạo hồ sơ đầu tiên
          </button>
        </div>
      )}

      {isLimitReached && (
        <div className="mt-12 flex items-start gap-4 rounded-4xl border border-[#f59e0b]/20 bg-[#fffbeb] p-6">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#f59e0b]/10 text-[#92400e]">
            <AlertCircle size={24} />
          </div>
          <div>
            <h4 className="font-black text-[#92400e]">Bạn đã đạt giới hạn 10 hồ sơ người thân</h4>
            <p className="text-sm font-medium text-[#92400e]/80">
              Vui lòng liên hệ hỗ trợ hoặc nâng cấp tài khoản để quản lý nhiều hồ sơ hơn.
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
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingProfileId(null);
        }}
        onSubmit={(profileId, data) => updateProfileMutation.mutate({ profileId, data })}
        isLoading={updateProfileMutation.isPending}
      />
    </DashboardPageShell>
  );
}
