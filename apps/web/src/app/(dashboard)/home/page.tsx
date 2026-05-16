"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Droplets,
  Eye,
  FlaskConical,
  HelpCircle,
  Share2,
  ShieldPlus,
  Stethoscope,
  Upload,
  UserPlus,
  X,
} from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { apiClient } from "@/lib/api/apiClient";
import { InviteMemberModal } from "@/components/features/profiles/InviteMemberModal";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";

type Profile = {
  id: string;
  displayName: string;
  isDefault: boolean;
};

type HealthRecord = {
  id: string;
  status?: string | null;
  testType?: string | null;
  examDate?: string | null;
  overallStatus?: "normal" | "attention" | "abnormal" | string;
  abnormalCount?: number;
};

type ProfileInvitation = {
  id: string;
  viewerId?: string | null;
  email: string;
  status: string;
  accessLevel: "view" | "edit" | string;
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

export default function DashboardHomePage() {
  const queryClient = useQueryClient();
  const [profilePickerOpen, setProfilePickerOpen] = useState(false);
  const [inviteModalOpen, setInviteModalOpen] = useState(false);
  const [invitingProfileId, setInvitingProfileId] = useState<string | null>(
    null,
  );
  const [selectedProfileId, setSelectedProfileId] = useState<string>("");
  const [, setPendingAccessUpdates] = useState<Record<string, boolean>>({});
  useEffect(() => {
    void apiClient.post(ApiPaths.PROFILES.ENSURE_DEFAULT);
  }, []);

  const { data: profiles = [] } = useQuery({
    queryKey: ["home-profiles"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const primaryProfileId = profiles.find((p) => p.isDefault)?.id ?? profiles[0]?.id;

  const { data: recentRecords = [] } = useQuery({
    queryKey: ["home-recent-records", primaryProfileId],
    enabled: Boolean(primaryProfileId),
    queryFn: async () => {
      const response = await apiClient.get(
        ApiPaths.PROFILES.HEALTH_RECORDS(primaryProfileId as string),
        {
          params: { page: 0, limit: 3 },
        },
      );
      return (response.data?.data ?? []) as HealthRecord[];
    },
  });

  const historyHref = useMemo(() => {
    if (!primaryProfileId) return "/health-records";
    return `/profiles/${primaryProfileId}/history`;
  }, [primaryProfileId]);

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
    mutationFn: async (payload: { targetId: string; email: string }) => {
      if (!invitingProfileId) {
        throw new Error("Thiếu profile để thu hồi quyền.");
      }
      await apiClient.delete(ApiPaths.PROFILES.REVOKE_SHARE(invitingProfileId, payload.targetId));
      return payload.email;
    },
    onSuccess: (email) => {
      queryClient.setQueryData(
        ["profile-shared-members", invitingProfileId],
        (previous: Array<{ id: string; viewerId?: string; email: string; accessLevel: string }> | undefined) =>
          (previous ?? []).filter((member) => member.email.toLowerCase() !== email.toLowerCase())
      );
      void queryClient.invalidateQueries({ queryKey: ["profile-shared-members", invitingProfileId] });
      setPendingAccessUpdates((prev) => {
        const next = { ...prev };
        delete next[email.toLowerCase()];
        return next;
      });
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
      const invitations = (response.data?.data ?? []) as ProfileInvitation[];
      return invitations
        .filter((invitation) => invitation.status === "accepted")
        .map((invitation) => ({
          id: invitation.id,
          viewerId: invitation.viewerId ?? undefined,
          email: invitation.email,
          accessLevel: invitation.accessLevel,
        }));
    },
  });

  const displaySharedMembers = sharedMembers;

  return (
    <DashboardPageShell
      title="Chào mừng quay lại!"
      subtitle="Đây là tổng quan tình trạng sức khỏe của bạn hôm nay."
      actions={
        <div className="text-left lg:text-right">
          <p className="text-base font-bold text-[#00685f]">
            Theo dõi sức khỏe mỗi ngày
          </p>
          <p className="text-xs uppercase tracking-wider text-[#6d7a77]">
            HealthLens Dashboard
          </p>
        </div>
      }
    >
      <section className="mb-12 grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={<Activity className="h-6 w-6" />}
          value="120/80"
          unit="mmHg"
          label="Huyết áp"
          status="Bình thường"
        />
        <StatCard
          icon={<Droplets className="h-6 w-6" />}
          value="95"
          unit="mg/dL"
          label="Đường huyết"
          status="Bình thường"
        />
        <StatCard
          icon={<FlaskConical className="h-6 w-6" />}
          value="200"
          unit="mg/dL"
          label="Cholesterol"
          status="Bình thường"
        />
        <StatCard
          icon={<ShieldPlus className="h-6 w-6" />}
          value="24.5"
          unit="kg/m²"
          label="BMI"
          status="Bình thường"
        />
      </section>

      <section className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="lg:col-span-7">
          <div className="mb-5 flex items-center justify-between">
            <h2 className="text-2xl font-bold text-[#121e1c]">
              Kết quả gần đây
            </h2>
            <Link
              href={historyHref}
              className="inline-flex items-center gap-1 text-sm font-bold text-[#00685f] hover:underline"
            >
              Xem tất cả
              <ChevronRight className="h-4 w-4" />
            </Link>
          </div>

          <div className="space-y-4">
            {recentRecords.length === 0 ? (
              <div className="rounded-2xl border border-[#bcc9c6]/30 bg-white p-5 text-sm text-[#6d7a77] shadow-sm">
                Chưa có kết quả gần đây cho hồ sơ hiện tại.
              </div>
            ) : (
              recentRecords.map((record) => (
                <article
                  key={record.id}
                  className="group flex items-center gap-4 rounded-2xl bg-white p-5 shadow-sm transition-colors hover:bg-[#e9f6f3]"
                >
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f] transition-colors group-hover:bg-[#00685f] group-hover:text-white">
                    <Stethoscope className="h-7 w-7" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-lg font-bold text-[#121e1c]">
                      {record.testType || "Phiếu khám bệnh"}
                    </p>
                    <p className="mt-1 flex items-center gap-2 text-sm text-[#6d7a77]">
                      <CalendarDays className="h-4 w-4" />
                      Ngày thực hiện: {record.examDate || "Chưa có ngày khám"}
                    </p>
                    <p className="mt-1 text-sm text-[#6d7a77]">
                      Số chỉ số bất thường: {record.abnormalCount ?? 0}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    <span
                      className={`rounded-full px-3 py-1 text-xs font-bold ${recordStatusClass(resolveHomeRecordStatus(record))}`}
                    >
                      {recordStatusLabel(resolveHomeRecordStatus(record))}
                    </span>
                    <Link
                      href={`/health-records/review/${record.id}`}
                      className="inline-flex items-center gap-1 text-sm font-semibold text-[#00685f] hover:underline"
                    >
                      Xem chi tiết
                      <ChevronRight className="h-4 w-4" />
                    </Link>
                  </div>
                </article>
              ))
            )}
          </div>
        </div>

        <div className="lg:col-span-5">
          <h2 className="mb-5 text-2xl font-bold text-[#121e1c]">
            Thao tác nhanh
          </h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <ActionTile
              icon={<Upload className="h-6 w-6" />}
              label="Tải kết quả"
              href={`/health-records?profileId=${primaryProfileId ?? ""}&openUpload=1`}
            />
            <ActionTile
              icon={<CalendarDays className="h-6 w-6" />}
              label="Đặt lịch khám"
              disabled
            />
            <ActionTile
              icon={<Eye className="h-6 w-6" />}
              label="Xem kết quả"
              href={historyHref}
            />
            <ActionTile
              icon={<Share2 className="h-6 w-6" />}
              label="Chia sẻ"
              disabled={profiles.length === 0}
              onClick={() => {
                setSelectedProfileId(primaryProfileId ?? profiles[0]?.id ?? "");
                setProfilePickerOpen(true);
              }}
            />
            <ActionTile
              icon={<HelpCircle className="h-6 w-6" />}
              label="Liên hệ bác sĩ"
              disabled
            />
            <ActionTile
              icon={<ShieldPlus className="h-6 w-6" />}
              label="Trợ giúp"
              href="/help"
            />
          </div>

          <div className="relative mt-8 overflow-hidden rounded-3xl bg-linear-to-br from-[#00685f] to-[#008378] p-7 text-white shadow-xl">
            <h3 className="text-xl font-bold">Chăm sóc sức khỏe chủ động</h3>
            <p className="mt-2 max-w-sm text-sm text-[#d8fffa]">
              Dựa trên kết quả gần nhất, bạn nên duy trì uống đủ nước và theo
              dõi định kỳ các chỉ số quan trọng.
            </p>
            <Link
              href="/help"
              className="mt-5 inline-flex min-h-12 items-center rounded-full bg-white px-5 text-sm font-bold text-[#00685f] transition hover:bg-[#e9f6f3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              Đọc hướng dẫn
            </Link>
            <div className="pointer-events-none absolute -bottom-10 -right-8 opacity-20">
              <ShieldPlus className="h-36 w-36" />
            </div>
          </div>
        </div>
      </section>
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
        onRevokeMember={(member) => revokeShareMutation.mutate({ targetId: member.viewerId, email: member.email })}
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
      {profilePickerOpen ? (
        <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="flex max-h-[78vh] w-full max-w-2xl flex-col overflow-hidden rounded-4xl bg-white shadow-2xl shadow-black/20 animate-in zoom-in-95 duration-200">
            <div className="relative p-7 pb-4">
              <button
                type="button"
                onClick={() => setProfilePickerOpen(false)}
                className="absolute top-6 right-6 rounded-full p-2 text-[#6d7a77] transition-colors hover:bg-[#e9f6f3]"
                aria-label="Đóng"
              >
                <X size={20} />
              </button>
              <div className="mb-2 flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#e9f6f3] text-[#00685f]">
                  <UserPlus size={24} />
                </div>
                <div>
                  <h3 className="text-2xl font-black text-[#121e1c]">
                    Chọn hồ sơ để chia sẻ
                  </h3>
                  <p className="mt-1 text-sm font-medium text-[#6d7a77]">
                    Chọn hồ sơ sức khỏe bạn muốn chia sẻ cho người khác xem.
                  </p>
                </div>
              </div>
            </div>
            <div className="px-7 pb-6">
              <label
                className="mb-2 block text-sm font-bold text-[#121e1c]"
                htmlFor="share-profile-select"
              >
                Hồ sơ cần chia sẻ <span className="text-red-600">*</span>
              </label>
              <div className="relative">
                <select
                  id="share-profile-select"
                  value={selectedProfileId}
                  onChange={(event) => setSelectedProfileId(event.target.value)}
                  className="h-12 w-full cursor-pointer appearance-none rounded-2xl border border-[#b7e8e0] bg-[#f0faf8] pr-10 pl-4 text-sm font-semibold text-[#3d4947] outline-none focus:border-[#008378]"
                >
                  {profiles.map((profile) => (
                    <option key={profile.id} value={profile.id}>
                      {profile.displayName}
                    </option>
                  ))}
                </select>
                <span className="pointer-events-none absolute top-1/2 right-4 -translate-y-1/2 text-[#00685f]">
                  ▾
                </span>
              </div>
            </div>
            <div className="mt-auto flex items-center justify-end border-t border-[#e8eeec] px-7 py-4">
              <button
                type="button"
                onClick={() => setProfilePickerOpen(false)}
                className="mr-3 rounded-2xl border border-[#c5dfd9] bg-white px-5 py-2.5 text-sm font-bold text-[#3d4947] transition hover:bg-[#f6fbfa]"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={!selectedProfileId}
                onClick={() => {
                  if (!selectedProfileId) return;
                  setInvitingProfileId(selectedProfileId);
                  setProfilePickerOpen(false);
                  setInviteModalOpen(true);
                }}
                className="rounded-2xl bg-[#008378] px-6 py-3 text-sm font-bold text-white shadow-md transition hover:brightness-110"
              >
                Tiếp tục
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </DashboardPageShell>
  );
}

function StatCard({
  icon,
  value,
  unit,
  label,
  status,
}: {
  icon: ReactNode;
  value: string;
  unit: string;
  label: string;
  status: string;
}) {
  return (
    <article className="rounded-xl border-l-4 border-[#00685f] bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-start justify-between">
        <div className="rounded-lg bg-[#e9f6f3] p-2 text-[#00685f]">{icon}</div>
        <span className="inline-flex items-center gap-1 rounded-full bg-[#e6f6f2] px-2 py-1 text-xs font-bold text-[#00685f]">
          <CheckCircle2 className="h-3.5 w-3.5" />
          {status}
        </span>
      </div>
      <p className="text-4xl font-black tracking-tight text-[#121e1c]">
        {value}
      </p>
      <p className="mt-1 text-xs font-semibold uppercase tracking-wider text-[#6d7a77]">
        {unit} • {label}
      </p>
    </article>
  );
}

function ActionTile({
  icon,
  label,
  href,
  disabled,
  onClick,
}: {
  icon: ReactNode;
  label: string;
  href?: string;
  disabled?: boolean;
  onClick?: () => void;
}) {
  const commonClass =
    "group aspect-square rounded-2xl bg-white p-4 shadow-sm transition-all duration-300 hover:bg-[#00685f] hover:text-white";

  if (disabled || !href) {
    return (
      <button
        type="button"
        disabled={disabled}
        onClick={disabled ? undefined : onClick}
        className={`${commonClass} ${disabled ? "cursor-not-allowed opacity-60" : ""}`}
      >
        <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
          <span className="text-[#00685f] group-hover:text-white">{icon}</span>
          <span className="text-xs font-bold uppercase tracking-tight">
            {label}
          </span>
        </div>
      </button>
    );
  }

  return (
    <Link href={href} className={commonClass}>
      <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
        <span className="text-[#00685f] group-hover:text-white">{icon}</span>
        <span className="text-xs font-bold uppercase tracking-tight">
          {label}
        </span>
      </div>
    </Link>
  );
}

function recordStatusClass(status: HealthRecord["overallStatus"]) {
  if (status === "abnormal") return "bg-[#ffdad6] text-[#ba1a1a]";
  if (status === "attention") return "bg-[#ffdbce] text-[#773215]";
  if (status === "normal") return "bg-[#e6f6f2] text-[#00685f]";
  if (status === "error" || status === "failed" || status === "ocr_failed")
    return "bg-[#ffe4e6] text-[#be123c]";
  return "bg-[#f1f5f9] text-[#64748b]";
}

function recordStatusLabel(status: HealthRecord["overallStatus"]) {
  if (status === "abnormal") return "Bất thường";
  if (status === "attention") return "Cần chú ý";
  if (status === "normal") return "Bình thường";
  if (status === "error" || status === "failed" || status === "ocr_failed")
    return "Lỗi";
  return "Chưa xác thực";
}

function resolveHomeRecordStatus(record: HealthRecord): string {
  const recordStatus = record.status?.toLowerCase();
  if (recordStatus === "done") {
    if (record.overallStatus === "abnormal" || record.overallStatus === "attention") {
      return record.overallStatus;
    }
    return "normal";
  }
  if (
    recordStatus === "review_required" ||
    recordStatus === "processing" ||
    recordStatus === "pending"
  ) {
    return "unverified";
  }
  if (
    recordStatus === "ocr_failed" ||
    recordStatus === "failed" ||
    recordStatus === "error"
  ) {
    return "error";
  }
  return record.overallStatus === "abnormal" || record.overallStatus === "attention"
    ? record.overallStatus
    : "unverified";
}
